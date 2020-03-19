package gov.usgs.wma.mlrgateway.service;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.DdotClient;

@Service
public class PreVerificationService {

	private DdotClient ddotClient;

	protected static final String INTERNAL_ERROR_MESSAGE = "{\"error_message\": \"Unable to read ingestor output.\"}";
	
	@Autowired
	public PreVerificationService(DdotClient ddotClient) {
		this.ddotClient = ddotClient;
	}

	public List<Map<String, Object>> parseDdot(MultipartFile file) throws HystrixBadRequestException {
		List<Map<String, Object>> ddots = null;
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<List<Map<String, Object>>> mapType = new TypeReference<List<Map<String, Object>>>() {};
		
		try {
			String ddotResponse = ddotClient.ingestDdot(file).getBody();
			ddots = mapper.readValue(ddotResponse, mapType);
		} catch (Exception e) {
			int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			throw new FeignBadResponseWrapper(status, null, INTERNAL_ERROR_MESSAGE);
		}

		return ddots;
	}
}
