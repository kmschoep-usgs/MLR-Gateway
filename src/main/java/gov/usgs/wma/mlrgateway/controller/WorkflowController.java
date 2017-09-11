package gov.usgs.wma.mlrgateway.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;

@RestController
public class WorkflowController {

	@Autowired
	private DdotClient ddotClient;

	@Autowired
	private LegacyCruClient legacyCruClient;

	@PostMapping("/workflow/ddot")
	public String legacyWorkflow(@RequestPart MultipartFile file, HttpServletResponse response) throws Exception {
		String rtn = "{}";
		String json = ddotClient.injestDdot(file);
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<List<Map<?,?>>> mapType = new TypeReference<List<Map<?,?>>>() {};
		List<Map<?,?>> ddots = mapper.readValue(json, mapType);

		if (ddots.size() == 1) {
			Map<?,?> ml = ddots.get(0);
			if (ml.containsKey("transactionType") && ml.get("transactionType") instanceof String) {
				if (((String) ml.get("transactionType")).contentEquals("A")) {
					ResponseEntity<String> resp = legacyCruClient.createMonitoringLocation(mapper.writeValueAsString(ml));
					response.setStatus(resp.getStatusCodeValue());
					rtn = resp.getBody();
				} else {
					ResponseEntity<String> resp = legacyCruClient.updateMonitoringLocation("1", mapper.writeValueAsString(ml));
					response.setStatus(resp.getStatusCodeValue());
					rtn = resp.getBody();
				}
			} else {
				response.setStatus(HttpStatus.SC_BAD_REQUEST);
				rtn = "{\"error\":{\"message\":\"Unable to determine transactionType.\"},\"data\":"
							+ json + "}";
			}
		} else {
			response.setStatus(HttpStatus.SC_BAD_REQUEST);
			rtn = "{\"error\":{\"message\":\"Only accepting files with one transaction at this time.\"},\"data\":"
					+ json + "}";
		}

		response.setContentType("application/json;charset=UTF-8");
		return rtn;
	}

}
