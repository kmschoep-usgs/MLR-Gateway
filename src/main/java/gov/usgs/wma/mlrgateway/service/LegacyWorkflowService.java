package gov.usgs.wma.mlrgateway.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.FileExportClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;

@Service
public class LegacyWorkflowService {

	private DdotService ddotService;
	private LegacyCruClient legacyCruClient;
	private TransformService transformService;
	private LegacyValidatorClient legacyValidatorClient;
	private FileExportClient fileExportClient;
	private NotificationClient notificationClient;

	public static final String AGENCY_CODE = "agencyCode";
	public static final String SITE_NUMBER = "siteNumber";
	public static final String TRANSACTION_TYPE = "transactionType";
	public static final String TRANSACTION_TYPE_ADD = "A";
	public static final String TRANSACTION_TYPE_UPDATE = "M";
	public static final String SITE_ADD_STEP = "Site Add";
	public static final String SITE_ADD_SUCCESSFULL = "Site Added Successfully";
	public static final String SITE_UPDATE_STEP = "Site Update";
	public static final String SITE_UPDATE_SUCCESSFULL = "Site Updated Successfully.";
	public static final String VALIDATION_STEP = "Validate";
	public static final String VALIDATION_SUCCESSFULL = "Transaction validated successfully.";
	public static final String BAD_TRANSACTION_TYPE = "{\"error\":{\"message\":\"Unable to determine transactionType.\"},\"data\":%json%}";

	@Autowired
	public LegacyWorkflowService(DdotService ddotService, LegacyCruClient legacyCruClient, TransformService transformService, LegacyValidatorClient legacyValidatorClient,
			FileExportClient fileExportClient, NotificationClient notificationClient) {
		this.ddotService = ddotService;
		this.legacyCruClient = legacyCruClient;
		this.transformService = transformService;
		this.legacyValidatorClient = legacyValidatorClient;
		this.fileExportClient = fileExportClient;
		this.notificationClient = notificationClient;
	}

	public String completeWorkflow(MultipartFile file, HttpServletResponse response) throws HystrixBadRequestException {
		String rtn = "{}";
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);

		for (Map<String, Object> ml: ddots) {
			//Note that this is only coded for a single transaction and will need logic to handle multiples - some of which may succeed while others fail.
			//Each one is a seperate transaction and will not be rolled back no matter what happens before or after it. The case with an invalid transaction type 
			//should also not stop processing of the transactions.
			String json = transformAndValidate(ml);

			if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
				if (((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD)) {
					ResponseEntity<String> resp = legacyCruClient.createMonitoringLocation(json);
					response.setStatus(resp.getStatusCodeValue());
					rtn = resp.getBody();
					fileExportClient.exportAdd(rtn);
					WorkflowController.addStepReport(new StepReport(SITE_ADD_STEP, HttpStatus.SC_OK, SITE_ADD_SUCCESSFULL, ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
				} else {
					ResponseEntity<String> resp = legacyCruClient.patchMonitoringLocation(json);
					response.setStatus(resp.getStatusCodeValue());
					rtn = resp.getBody();
					fileExportClient.exportUpdate(rtn);
					WorkflowController.addStepReport(new StepReport(SITE_UPDATE_STEP, HttpStatus.SC_OK, SITE_UPDATE_SUCCESSFULL, ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
				}
			} else {
				WorkflowController.addStepReport(new StepReport(VALIDATION_STEP, HttpStatus.SC_BAD_REQUEST, BAD_TRANSACTION_TYPE.replace("%json%", json), ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
			}
		}

		notificationClient.sendEmail("test", "rtn", "drsteini@usgs.gov");
		return rtn;
	}

	public String ddotValidation(MultipartFile file, HttpServletResponse response) throws HystrixBadRequestException {
		String rtn = "{}";
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);

		for (Map<String, Object> ml: ddots) {
			rtn = transformAndValidate(ml);
		}

		notificationClient.sendEmail("test" + LocalDate.now(), "rtn", "drsteini@usgs.gov");
		return rtn;
	}

	protected String transformAndValidate(Map<String, Object> ml) {
		//Note that this does not inspect the input for cross-field dependencies (like latitude, longitude, and coordinateDatumCode) We will probably need to 
		//handle the case where one, but not all of the fields are present in the update. 
		ObjectMapper mapper = new ObjectMapper();
		String json = "";

		ml = transformService.transform(ml);

		try {
			json = mapper.writeValueAsString(ml);
		} catch (Exception e) {
			// Unable to determine when this might actually happen, but the api says it can...
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize transformer output.\"}");
		}

		ResponseEntity<String> resp = legacyValidatorClient.validate(json);
		WorkflowController.addStepReport(new StepReport(VALIDATION_STEP, HttpStatus.SC_OK, VALIDATION_SUCCESSFULL, ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));

		return json.substring(0, json.length()-1) + ",\"validation\":" + resp.getBody().toString() + "}";
	}

}
