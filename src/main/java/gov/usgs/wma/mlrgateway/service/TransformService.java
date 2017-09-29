package gov.usgs.wma.mlrgateway.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.LegacyTransformerClient;

@Service
public class TransformService {

	private LegacyTransformerClient legacyTransformerClient;

	@Autowired
	public TransformService(LegacyTransformerClient legacyTransformerClient) {
		this.legacyTransformerClient = legacyTransformerClient;
	}

	public Map<String, Object> transform(Map<String, Object> ml) throws HystrixBadRequestException {
		Map<String, Object> transformed = new HashMap<>();
		transformed.putAll(ml);

		if (ml.containsKey("latitude") && ml.containsKey("longitude") && ml.containsKey("coordinateDatumCode")) {
			transformed.putAll(transformGeo(ml));
		}

		if (ml.containsKey("stationName")) {
			transformed.putAll(transformStationIx(ml));
		}

		return transformed;	
	}

	protected Map<String, Object> transformGeo(Map<String, Object> ml) throws HystrixBadRequestException {
		String json = "\"{\"latitude\": \"" + ml.get("latitude") + "\",\"longitude\":\"" + ml.get("longitude") + "\",\"coordinateDatumCode\":\"" + ml.get("coordinateDatumCode") + "\"}";
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> transforms = null;
		TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {};

		ResponseEntity<String> response = legacyTransformerClient.decimalLocation(json);

		try {
			transforms = mapper.readValue(response.getBody().toString(), mapType);
		} catch (Exception e) {
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error\":{\"message\": \"Unable to read transformer decimal_location output.\"}}");
		}

		return transforms;
	}

	protected Map<String, Object> transformStationIx(Map<String, Object> ml) throws HystrixBadRequestException {
		String json = "{\"stationName\": \"" + ml.get("stationName") + "\"}";
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> transforms = null;
		TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {};

		ResponseEntity<String> response = legacyTransformerClient.stationIx(json);

		try {
			transforms = mapper.readValue(response.getBody().toString(), mapType);
		} catch (Exception e) {
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error\":{\"message\": \"Unable to read transformer station_ix output.\"}}");
		}

		return transforms;
	}
}
