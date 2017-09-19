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
public class DdotService {

	private DdotClient ddotClient;

	@Autowired
	public DdotService(DdotClient ddotClient) {
		this.ddotClient = ddotClient;
	}

	public List<Map<?,?>> parseDdot(MultipartFile file) throws HystrixBadRequestException {
		List<Map<?,?>> ddots = null;
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<List<Map<?,?>>> mapType = new TypeReference<List<Map<?,?>>>() {};

		String ddotResponse = ddotClient.ingestDdot(file);

		try {
			ddots = mapper.readValue(ddotResponse, mapType);
		} catch (Exception e) {
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error\":{\"message\": \"Unable to read ingestor output.\"}}");
		}

		if (ddots.size() != 1) {
			throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\":{\"message\":\"Only accepting files with one transaction at this time.\"},\"data\":"
					+ ddotResponse + "}");
		}

		return ddots;
	}

}
