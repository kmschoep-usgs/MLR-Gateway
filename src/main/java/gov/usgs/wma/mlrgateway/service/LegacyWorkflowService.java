package gov.usgs.wma.mlrgateway.service;

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
import gov.usgs.wma.mlrgateway.client.FileExportClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;

@Service
public class LegacyWorkflowService {

	private DdotService ddotService;
	private LegacyCruClient legacyCruClient;
	private TransformService transformService;
	private LegacyValidatorClient legacyValidatorClient;
	private FileExportClient fileExportClient;
	private NotificationClient notificationClient;

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

			if (ml.containsKey("transactionType") && ml.get("transactionType") instanceof String) {
				if (((String) ml.get("transactionType")).contentEquals("A")) {
					ResponseEntity<String> resp = legacyCruClient.createMonitoringLocation(json);
					response.setStatus(resp.getStatusCodeValue());
					rtn = resp.getBody();
					fileExportClient.exportAdd(rtn);
				} else {
					ResponseEntity<String> resp = legacyCruClient.updateMonitoringLocation("0", json);
					response.setStatus(resp.getStatusCodeValue());
					rtn = resp.getBody();
					fileExportClient.exportUpdate(rtn);
				}
			} else {
				throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\":{\"message\":\"Unable to determine transactionType.\"},\"data\":" + json + "}");
			}
		}

//		notificationClient.sendEmail("test", "rtn", "drsteini@usgs.gov");
		return rtn;
	}

	public String ddotValidation(MultipartFile file, HttpServletResponse response) throws HystrixBadRequestException {
		String rtn = "{}";
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);

		for (Map<String, Object> ml: ddots) {
			rtn = transformAndValidate(ml);
		}

//		notificationClient.sendEmail("test", "rtn", "drsteini@usgs.gov");
		return rtn;
	}

	protected String transformAndValidate(Map<String, Object> ml) {
		ObjectMapper mapper = new ObjectMapper();
		//Note that this is only coded for a single transaction and will need logic to handle multiples - some of which may succeed while others fail.
		//Each one is a seperate transaction and will not be rolled back no matter what happens before or after it. The case with an invalid transaction type 
		//should also not stop processing of the transactions.
		String json = "";

		ml = transformService.transform(ml);

		try {
			json = mapper.writeValueAsString(ml);
		} catch (Exception e) {
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize ingestor output.\"}");
		}

		ResponseEntity<String> resp = legacyValidatorClient.validate(json);

		return json.substring(0, json.length()-1) + ",\"validation\":" + resp.getBody().toString() + "}";
	}
}
