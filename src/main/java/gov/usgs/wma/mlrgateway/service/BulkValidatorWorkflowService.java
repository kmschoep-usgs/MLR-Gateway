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
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;

@Service
public class BulkValidatorWorkflowService {

	private LegacyCruClient legacyCruClient;
	private LegacyValidatorClient legacyValidatorClient;
	
	public static final String SITE_GET_STEP = "Location Get by ID";
	public static final String SITE_GET_SUCCESSFULL = "Location Get Successful";
	public static final String SITE_GET_DOES_NOT_EXIST = "Requested Location Not Found";
	public static final String VALIDATION_STEP = "Validate MLR sitefile record.";
	public static final String VALIDATION_SUCCESSFUL = "MLR Sitefile Validated Successfully.";
	public static final String VALIDATION_FAILED = "Transaction validation failed.";
	
	protected static final String INTERNAL_ERROR_MESSAGE = "{\"error\":{\"message\": \"Unable to read Legacy CRU output.\"}}";
	protected static final String SC_INTERNAL_ERROR_MESSAGE = "{\"error_message\": \"Unable to serialize Legacy CRU output.\"}";

	@Autowired
	public BulkValidatorWorkflowService(LegacyCruClient legacyCruClient, LegacyValidatorClient legacyValidatorClient) {
		this.legacyCruClient = legacyCruClient;
		this.legacyValidatorClient = legacyValidatorClient;
	}

	public void completeWorkflow() throws HystrixBadRequestException {
		Map<String, Object> rec = new HashMap<>();
		for (int i = 1; i <= 33721; i++) {
			rec = doValidate(Integer.toString(i));
		}
	}
	public Map<String, Object> doValidate(String legacyLocationId) throws FeignBadResponseWrapper {
		Map<String, Object> ml = new HashMap<>();
		try {
			ResponseEntity<String> validationResponse;
				
			String validationPayload = preValidation(legacyLocationId);
			validationResponse = legacyValidatorClient.validateAdd(validationPayload);
			ml = verifyValidationStatus(ml, validationResponse);
			BaseController.addStepReport(new StepReport(VALIDATION_STEP, validationResponse.getStatusCodeValue(),  validationResponse.getBody(), "{\"legacy_location_id\": \"" + legacyLocationId + "\"}", null));
			return ml;
			} catch (Exception e) {
				if(e instanceof FeignBadResponseWrapper) {
					BaseController.addStepReport(new StepReport(VALIDATION_STEP, ((FeignBadResponseWrapper)e).getStatus(),  ((FeignBadResponseWrapper)e).getBody(), "{\"legacy_location_id\": \"" + legacyLocationId + "\"}", null));
				} else {
					BaseController.addStepReport(new StepReport(VALIDATION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR,  e.getMessage(), "{\"legacy_location_id\": \"" + legacyLocationId + "\"}", null));
				}
								//Throw error to stop additional transaction processing
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"" + VALIDATION_FAILED + "\"}");	
			}
	}
	
	private String preValidation(String legacyLocationId) {
		TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {};
		Map<String, Object> existingRecord = new HashMap<>();
		Map<String, Object> placeholder = new HashMap<>();
		Map<String, Object> validationPayload = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		
		//Fetch Existing Record
		try {
			ResponseEntity<String> existingRecordResponse = legacyCruClient.getMonitoringLocations(legacyLocationId);
			int cruStatus = existingRecordResponse.getStatusCodeValue();
			
			if(cruStatus == 200) {
				existingRecord = mapper.readValue(existingRecordResponse.getBody(), mapType);
			}
		} catch (Exception e) {
			//Do nothing
		}
		
		validationPayload.put(LegacyValidatorClient.NEW_RECORD_PAYLOAD,existingRecord);
		validationPayload.put(LegacyValidatorClient.EXISTING_RECORD_PAYLOAD, placeholder);
		
		try {
			String json = mapper.writeValueAsString(validationPayload);
			//json = json.substring(0,json.length()-2) + ",\"existingLocation\":{}}";
			
			return json;
		} catch(Exception e) {
			// Unable to determine when this might actually happen, but the api says it can...
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize input as validator payload.\"}");
		}
	}
	
	private Map<String, Object> verifyValidationStatus(Map<String,Object> ml, ResponseEntity<String> validationResponse) {
		if(validationResponse.getStatusCode().value() == 200) {
			ObjectMapper mapper = new ObjectMapper();
			TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {};
			Map<String, Object> validationMessage = new HashMap<>();
			
			try {
				validationMessage = mapper.readValue(validationResponse.getBody(), mapType);
				ml.put("validation", validationMessage);
			} catch (Exception e) {
				throw new FeignBadResponseWrapper(validationResponse.getStatusCodeValue(), null, "{\"error_message\": \"Unable to deserialize validator response as JSON: " + validationResponse.getBody() + "\"}");
			}
			
		} else {
			throw new FeignBadResponseWrapper(validationResponse.getStatusCodeValue(), null, "{\"error_message\": \"An internal error occurred during validation: " + validationResponse.getBody() + "\"}");	
		}
		
		return ml;
	}

}
