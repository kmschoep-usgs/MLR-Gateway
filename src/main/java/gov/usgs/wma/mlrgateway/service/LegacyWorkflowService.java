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
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;

@Service
public class LegacyWorkflowService {

	private DdotService ddotService;
	private LegacyCruClient legacyCruClient;

	@Autowired
	public LegacyWorkflowService(DdotService ddotService, LegacyCruClient legacyCruClient) {
		this.ddotService = ddotService;
		this.legacyCruClient = legacyCruClient;
	}

	public String completeWorkflow(MultipartFile file, HttpServletResponse response) throws HystrixBadRequestException {
		String rtn = "{}";
		ObjectMapper mapper = new ObjectMapper();
		List<Map<?,?>> ddots = ddotService.parseDdot(file);

		for (Map<?,?> ml: ddots) {
			//Note that this is only coded for a single transaction and will need logic to handle multiples - some of which may succeed while others fail.
			//Each one is a seperate transaction and will not be rolled back no matter what happens before or after it. The case with an invalid transaction type 
			//should also not stop processing of the transactions.
			String json = "";
			try {
				json = mapper.writeValueAsString(ml);
			} catch (Exception e) {
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize ingestor output.\"}");
			}
			if (ml.containsKey("transactionType") && ml.get("transactionType") instanceof String) {
				if (((String) ml.get("transactionType")).contentEquals("A")) {
					ResponseEntity<String> resp = legacyCruClient.createMonitoringLocation(json);
					response.setStatus(resp.getStatusCodeValue());
					rtn = resp.getBody();
				} else {
					ResponseEntity<String> resp = legacyCruClient.updateMonitoringLocation("0", json);
					response.setStatus(resp.getStatusCodeValue());
					rtn = resp.getBody();
				}
			} else {
				throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\":{\"message\":\"Unable to determine transactionType.\"},\"data\":" + json + "}");
			}
		}

		return rtn;
	}

	public String ddotValidation(MultipartFile file, HttpServletResponse response) throws HystrixBadRequestException {
		List<Map<?,?>> ddots = ddotService.parseDdot(file);
		//TODO need to call validator here & return status from it.
		return "{}";
	}

}
