package gov.usgs.wma.mlrgateway.service;

import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;

import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.LegacyTransformerClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import static gov.usgs.wma.mlrgateway.service.LegacyTransformerService.LATITUDE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LegacyTransformerService {

	private LegacyTransformerClient legacyTransformerClient;
	private Logger log = LoggerFactory.getLogger(LegacyTransformerService.class);

	protected static final String STEP_NAME = "Transform Data";
	protected static final String GEO_SUCCESS = "Decimal Location Transformed Successfully.";
	protected static final String GEO_FAILURE = "{\"error\":{\"message\": \"Unable to read transformer decimal_location output.\"}}";
	protected static final String STATION_IX_SUCCESS = "StationIX Tranformed Successfully.";
	protected static final String STATION_IX_FAILURE = "{\"error\":{\"message\": \"Unable to read transformer station_ix output.\"}}";
	protected static final String LATITUDE = "latitude";
	protected static final String LONGITUDE = "longitude";
	protected static final String COORDINATE_DATUM_CODE = "coordinateDatumCode";
	protected static final String STATION_NAME = "stationName";

	@Autowired
	public LegacyTransformerService(LegacyTransformerClient legacyTransformerClient) {
		this.legacyTransformerClient = legacyTransformerClient;
	}

	public Map<String, Object> transformGeo(Map<String, Object> ml) {
		Map<String, Object> transforms = new HashMap<>(ml);
		if (ml.containsKey(LATITUDE) && ml.containsKey(LONGITUDE) && ml.containsKey(COORDINATE_DATUM_CODE)) {

			String json = "{\"" + LATITUDE + "\": \"" + ml.get(LATITUDE) + "\",\"" + LONGITUDE + "\":\"" + ml.get(LONGITUDE) + "\",\"" + COORDINATE_DATUM_CODE + "\":\"" + ml.get(COORDINATE_DATUM_CODE) + "\"}";
			ObjectMapper mapper = new ObjectMapper();
			
			TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {};

			try {
				ResponseEntity<String> response = legacyTransformerClient.decimalLocation(json);
				transforms.putAll(mapper.readValue(response.getBody(), mapType));
				WorkflowController.addStepReport(new StepReport(STEP_NAME, HttpStatus.SC_OK, GEO_SUCCESS, ml.get(LegacyWorkflowService.AGENCY_CODE), ml.get(LegacyWorkflowService.SITE_NUMBER)));
			} catch (Exception e) {
				WorkflowController.addStepReport(new StepReport(STEP_NAME, HttpStatus.SC_INTERNAL_SERVER_ERROR, GEO_FAILURE, ml.get(LegacyWorkflowService.AGENCY_CODE), ml.get(LegacyWorkflowService.SITE_NUMBER)));
				log.error(STEP_NAME + ": " + e.getMessage());
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"" + GEO_FAILURE + "\"}");	
			}
		}
		return transforms;
	}

	public Map<String, Object> transformStationIx(Map<String, Object> ml) {
		Map<String, Object> transformed = new HashMap<>(ml);
		if (ml.containsKey(STATION_NAME)) {
			String json = "{\"" + STATION_NAME + "\": \"" + ml.get(STATION_NAME) + "\"}";
			ObjectMapper mapper = new ObjectMapper();
			TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {};

			ResponseEntity<String> response = legacyTransformerClient.stationIx(json);

			try {
				transformed.putAll(mapper.readValue(response.getBody(), mapType));
				WorkflowController.addStepReport(new StepReport(STEP_NAME, HttpStatus.SC_OK, STATION_IX_SUCCESS, ml.get(LegacyWorkflowService.AGENCY_CODE), ml.get(LegacyWorkflowService.SITE_NUMBER)));
			} catch (Exception e) {
				WorkflowController.addStepReport(new StepReport(STEP_NAME, HttpStatus.SC_INTERNAL_SERVER_ERROR, STATION_IX_FAILURE, ml.get(LegacyWorkflowService.AGENCY_CODE), ml.get(LegacyWorkflowService.SITE_NUMBER)));
				log.error(STEP_NAME + ": " + e.getMessage());
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"" + STATION_IX_FAILURE + "\"}");	
			}
		}
		return transformed;
	}

}
