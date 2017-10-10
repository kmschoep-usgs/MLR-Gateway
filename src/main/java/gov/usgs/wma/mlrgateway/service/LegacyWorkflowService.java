package gov.usgs.wma.mlrgateway.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	public static final String SITE_ADD_FAILED = "Site add failed";
	public static final String SITE_UPDATE_STEP = "Site Update";
	public static final String SITE_UPDATE_SUCCESSFULL = "Site Updated Successfully.";
	public static final String SITE_UPDATE_FAILED = "Site update failed";
	public static final String EXPORT_ADD_STEP = "Export Add Transaction File";
	public static final String EXPORT_UPDATE_STEP = "Export Update Transaction File";
	public static final String EXPORT_SUCCESSFULL = "Transaction File created Successfully.";
	public static final String EXPORT_ADD_FAILED = "Export add failed";
	public static final String EXPORT_UPDATE_FAILED = "Export update failed";
	public static final String VALIDATION_STEP = "Validate";
	public static final String VALIDATION_SUCCESSFULL = "Transaction validated successfully.";
	public static final String BAD_TRANSACTION_TYPE = "{\"error\":{\"message\":\"Unable to determine transactionType.\"},\"data\":%json%}";
	public static final String COMPLETE_WORKFLOW = "Complete Workflow";
	public static final String COMPLETE_WORKFLOW_FAILED = "Complete workflow failed";

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

	public void completeWorkflow(MultipartFile file) throws HystrixBadRequestException {
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);

		String json = "{}";
		for (Map<String, Object> ml: ddots) {
			//Note that this is only coded for a single transaction and will need logic to handle multiples - some of which may succeed while others fail.
			//Each one is a seperate transaction and will not be rolled back no matter what happens before or after it. The case with an invalid transaction type
			//should also not stop processing of the transactions.
			try {
					//loop through every item, try/catch
					if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
						json = transformAndValidate(ml);
						if (((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD)) {
							addTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json);
						} else {
							updateTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json);
						}
					} else {
						WorkflowController.addStepReport(new StepReport(VALIDATION_STEP, HttpStatus.SC_BAD_REQUEST, BAD_TRANSACTION_TYPE.replace("%json%", json), ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
					}
			} catch (Exception e) {
				WorkflowController.addStepReport(new StepReport(COMPLETE_WORKFLOW, HttpStatus.SC_INTERNAL_SERVER_ERROR, COMPLETE_WORKFLOW_FAILED,  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
			}
		}

		notificationClient.sendEmail("test", "rtn", "drsteini@usgs.gov");
	}

	public void ddotValidation(MultipartFile file) throws HystrixBadRequestException {
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);

		for (Map<String, Object> ml: ddots) {
			transformAndValidate(ml);
		}

		notificationClient.sendEmail("test" + LocalDate.now(), "rtn", "drsteini@usgs.gov");
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
		WorkflowController.addStepReport(new StepReport(VALIDATION_STEP, resp.getStatusCodeValue(), VALIDATION_SUCCESSFULL, ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));

		return json.substring(0, json.length()-1) + ",\"validation\":" + resp.getBody().toString() + "}";
	}

	protected void addTransaction(Object agencyCode, Object siteNumber, String json) {
		//catch bad adds and exports
		try {
			ResponseEntity<String> cruResp = legacyCruClient.createMonitoringLocation(json);
			int cruStatus = cruResp.getStatusCodeValue();
			WorkflowController.addStepReport(new StepReport(SITE_ADD_STEP, cruStatus, 201 == cruStatus ? SITE_ADD_SUCCESSFULL : cruResp.getBody(), agencyCode, siteNumber));
			exportAdd(agencyCode, siteNumber, cruResp.getBody());
		} catch (Exception e) {
			WorkflowController.addStepReport(new StepReport(SITE_ADD_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, SITE_ADD_FAILED,  agencyCode, siteNumber));
		}

	}

	protected void exportAdd(Object agencyCode, Object siteNumber, String json) {
		try {
			ResponseEntity<String> exportResp = fileExportClient.exportAdd(json);
			int exportStatus = exportResp.getStatusCodeValue();
			WorkflowController.addStepReport(new StepReport(EXPORT_ADD_STEP, exportStatus, 200 == exportStatus ? EXPORT_SUCCESSFULL : exportResp.getBody(), agencyCode, siteNumber));
		} catch (Exception e) {
			WorkflowController.addStepReport(new StepReport(EXPORT_ADD_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, EXPORT_ADD_FAILED,  agencyCode, siteNumber));
		}
	}


	protected void updateTransaction(Object agencyCode, Object siteNumber, String json) {
		//catch bad updates and exports (new stepreport)
		try {
			ResponseEntity<String> cruResp = legacyCruClient.patchMonitoringLocation(json);
			int cruStatus = cruResp.getStatusCodeValue();
			WorkflowController.addStepReport(new StepReport(SITE_UPDATE_STEP, cruStatus, 200 == cruStatus ? SITE_UPDATE_SUCCESSFULL : cruResp.getBody(), agencyCode, siteNumber));
			exportUpdate(agencyCode, siteNumber, cruResp.getBody());
		} catch (Exception e){
			WorkflowController.addStepReport(new StepReport(SITE_UPDATE_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, SITE_UPDATE_FAILED,  agencyCode, siteNumber));

		}


	}

	protected void exportUpdate(Object agencyCode, Object siteNumber, String json) {
		try {
			ResponseEntity<String> exportResp = fileExportClient.exportUpdate(json);
			int exportStatus = exportResp.getStatusCodeValue();
			WorkflowController.addStepReport(new StepReport(EXPORT_UPDATE_STEP, exportStatus, 200 == exportStatus ? EXPORT_SUCCESSFULL : exportResp.getBody(), agencyCode, siteNumber));
		} catch (Exception e) {
			WorkflowController.addStepReport(new StepReport(EXPORT_UPDATE_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, EXPORT_UPDATE_FAILED,  agencyCode, siteNumber));

		}
	}

}
