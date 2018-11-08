package gov.usgs.wma.mlrgateway.service;

import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LegacyValidatorService {
	private Logger log = LoggerFactory.getLogger(LegacyTransformerService.class);

	private LegacyCruService legacyCruService;
	private LegacyValidatorClient legacyValidatorClient;
	public static final String VALIDATION_STEP = "Validate";
	public static final String VALIDATION_SUCCESSFUL = "Transaction validated successfully.";
	public static final String VALIDATION_FAILED = "Transaction validation failed.";

	@Autowired
	public LegacyValidatorService(LegacyCruService legacyCruService, LegacyValidatorClient legacyValidatorClient){
		this.legacyCruService = legacyCruService;
		this.legacyValidatorClient = legacyValidatorClient;
	}

	public Map<String, Object> doValidation(Map<String, Object> ml, boolean isAddTransaction) throws FeignBadResponseWrapper {
		try {
			ResponseEntity<String> validationResponse;
			String validationPayload = preValidation(ml, isAddTransaction);
			if(isAddTransaction) {
				validationResponse = legacyValidatorClient.validateAdd(validationPayload);
			} else {
				 validationResponse = legacyValidatorClient.validateUpdate(validationPayload);
			}
			ml = verifyValidationStatus(ml, validationResponse);
			BaseController.addStepReport(new StepReport(VALIDATION_STEP, validationResponse.getStatusCodeValue(),  VALIDATION_SUCCESSFUL, ml.get(LegacyWorkflowService.AGENCY_CODE), ml.get(LegacyWorkflowService.SITE_NUMBER)));
			return ml;
		} catch (Exception e) {
			int status;
			if(e instanceof FeignBadResponseWrapper) {
				status = ((FeignBadResponseWrapper)e).getStatus();
				BaseController.addStepReport(new StepReport(VALIDATION_STEP, status,  ((FeignBadResponseWrapper)e).getBody(), ml.get(LegacyWorkflowService.AGENCY_CODE), ml.get(LegacyWorkflowService.SITE_NUMBER)));
			} else {
				status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				BaseController.addStepReport(new StepReport(VALIDATION_STEP, status,  e.getMessage(), ml.get(LegacyWorkflowService.AGENCY_CODE), ml.get(LegacyWorkflowService.SITE_NUMBER)));
			}

			//Throw error to stop additional transaction processing
			throw new FeignBadResponseWrapper(status, null, "{\"error_message\": \"" + VALIDATION_FAILED + "\"}");	
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
				log.error(VALIDATION_STEP + ": " + e.getMessage());
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to deserialize validator response as JSON: " + validationResponse.getBody() + "\"}");
			}

			if((!validationMessage.containsKey(LegacyValidatorClient.RESPONSE_PASSED_MESSAGE) &&
			     !validationMessage.containsKey(LegacyValidatorClient.RESPONSE_WARNING_MESSAGE) &&
			     !validationMessage.containsKey(LegacyValidatorClient.RESPONSE_ERROR_MESSAGE)) || 
			      validationMessage.isEmpty()) {
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": " + validationResponse.getBody() + "}");	
			}
			else if (validationMessage.containsKey(LegacyValidatorClient.RESPONSE_ERROR_MESSAGE)) {
				throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": " + validationResponse.getBody() + "}");
			}
		} else {
			throw new FeignBadResponseWrapper(validationResponse.getStatusCodeValue(), null, "{\"error_message\": \"An internal error occurred during validation: " + validationResponse.getBody() + "\"}");	
		}

		return ml;
	}

	private String preValidation(Map<String, Object> ml, boolean isAddTransaction) {
		Map<String, Object> existingRecord = new HashMap<>();
		Map<String, Object> validationPayload = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();

		//Fetch Existing Record
		String siteNumber = ml.get(LegacyWorkflowService.SITE_NUMBER) != null ? ml.get(LegacyWorkflowService.SITE_NUMBER).toString() : null;
		String agencyCode = ml.get(LegacyWorkflowService.AGENCY_CODE) != null ? ml.get(LegacyWorkflowService.AGENCY_CODE).toString() : null;

		existingRecord = legacyCruService.getMonitoringLocation(agencyCode, siteNumber, isAddTransaction);

		validationPayload.put(LegacyValidatorClient.NEW_RECORD_PAYLOAD,ml);
		validationPayload.put(LegacyValidatorClient.EXISTING_RECORD_PAYLOAD,existingRecord);

		try {
			String json = mapper.writeValueAsString(validationPayload);

			return json;
		} catch(Exception e) {
			log.error(VALIDATION_STEP + ": " + e.getMessage());
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize input as validator payload.\"}");
		}
	}
}
