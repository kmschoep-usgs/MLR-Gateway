package gov.usgs.wma.mlrgateway.service;

import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.apache.http.HttpStatus;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.util.ClientErrorParser;

import org.codehaus.jackson.io.JsonStringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LegacyValidatorService {
	private Logger log = LoggerFactory.getLogger(LegacyTransformerService.class);
	
	private final LegacyCruService legacyCruService;
	private final LegacyValidatorClient legacyValidatorClient;
	private ClientErrorParser clientErrorParser;
	public static final String VALIDATION_STEP = "Validate";
	public static final String VALIDATION_SUCCESSFUL = "Transaction validated successfully.";
	public static final String VALIDATION_FAILED = "{\"error_message\":\"Transaction validation failed.\"}";
	public static final String SITE_VALIDATE_STEP = "Validate Duplicate Monitoring Location Name";
	public static final String SITE_VALIDATE_SUCCESSFUL = "Monitoring Location Duplicate Name Validation Succeeded";
	public static final String SITE_VALIDATE_FAILED = "{\"error_message\":\"Monitoring Location Duplicate Name Validation Failed\"}";

	@Autowired
	public LegacyValidatorService(LegacyCruService legacyCruService, LegacyValidatorClient legacyValidatorClient){
		this.legacyCruService = legacyCruService;
		this.legacyValidatorClient = legacyValidatorClient;
	}

	public Map<String, Object> doValidation(Map<String, Object> ml, boolean isAddTransaction, SiteReport siteReport) throws FeignBadResponseWrapper {
		clientErrorParser = new ClientErrorParser();
		int duplicateValidationStatus = 200;
		int otherValidationStatus = 200;
		try {
			doDuplicateValidation(ml, siteReport);
		} catch (Exception e) {
			if(e instanceof FeignBadResponseWrapper) {
				duplicateValidationStatus = ((FeignBadResponseWrapper)e).getStatus();
				siteReport.addStepReport(new StepReport(SITE_VALIDATE_STEP, duplicateValidationStatus, false, clientErrorParser.parseClientError("LegacyCruClient#validateMonitoringLocation(String)", ((FeignBadResponseWrapper)e).getBody())));
			} else {
				duplicateValidationStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				siteReport.addStepReport(new StepReport(SITE_VALIDATE_STEP, duplicateValidationStatus, false, e.getMessage()));
			}
		}
		try {
			ResponseEntity<String> validationResponse;
			String validationPayload = attachExistingMonitoringLocation(ml, isAddTransaction, siteReport);
			if(isAddTransaction) {
				validationResponse = legacyValidatorClient.validateAdd(validationPayload);
			} else {
				 validationResponse = legacyValidatorClient.validateUpdate(validationPayload);
			}
			ml = verifyValidationStatus(ml, validationResponse);
			siteReport.addStepReport(new StepReport(VALIDATION_STEP, validationResponse.getStatusCodeValue(), true, validationResponse.getBody() ));
		} catch (Exception e) {
			if(e instanceof FeignBadResponseWrapper) {
				otherValidationStatus = ((FeignBadResponseWrapper)e).getStatus();
				siteReport.addStepReport(new StepReport(VALIDATION_STEP, otherValidationStatus, false, ((FeignBadResponseWrapper)e).getBody()));
				
			} else {
				otherValidationStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				siteReport.addStepReport(new StepReport(VALIDATION_STEP, otherValidationStatus, false, "{\"error_message\":\"" + e.getMessage() + "\"}"));
			}
		}
		//We have to choose between two status codes.
		//Favor advertising a server-side error over a client-side error
		//Favor advertising a client-side error over a server-side success
		//5xx > 4xx > 2xx
		
		int finalStatus = Math.max(duplicateValidationStatus, otherValidationStatus);

		if(200 == finalStatus) {
			return ml;
		} else {
			//Throw error to stop additional transaction processing
			throw new FeignBadResponseWrapper(finalStatus, null, VALIDATION_FAILED );	
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
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"validator_message\": " + validationResponse.getBody() + "}");	
			}
			else if (validationMessage.containsKey(LegacyValidatorClient.RESPONSE_ERROR_MESSAGE)) {
				throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"validator_message\": " + validationResponse.getBody() + "}");
			}
		} else {
			throw new FeignBadResponseWrapper(validationResponse.getStatusCodeValue(), null, "{\"error_message\": \"An internal error occurred during validation: " + validationResponse.getBody() + "\"}");	
		}

		return ml;
	}

	private String attachExistingMonitoringLocation(Map<String, Object> ml, boolean isAddTransaction, SiteReport siteReport) {
		Map<String, Object> existingRecord = new HashMap<>();
		Map<String, Object> validationPayload = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();

		//Fetch Existing Record
		String siteNumber = ml.get(LegacyWorkflowService.SITE_NUMBER) != null ? ml.get(LegacyWorkflowService.SITE_NUMBER).toString() : null;
		String agencyCode = ml.get(LegacyWorkflowService.AGENCY_CODE) != null ? ml.get(LegacyWorkflowService.AGENCY_CODE).toString() : null;

		existingRecord = legacyCruService.getMonitoringLocation(agencyCode, siteNumber, isAddTransaction, siteReport);

		validationPayload.put(LegacyValidatorClient.NEW_RECORD_PAYLOAD,ml);
		validationPayload.put(LegacyValidatorClient.EXISTING_RECORD_PAYLOAD,existingRecord);

		try {
			String json = mapper.writeValueAsString(validationPayload);

			return json;
		} catch(Exception e) {
			log.error(VALIDATION_STEP + ": " + e.getMessage());
			siteReport.addStepReport(new StepReport(VALIDATION_FAILED, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, e.getMessage()));
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize input as validator payload.\"}");
		}
	}
	
	/**
	 * 
	 * @param ml
	 * @throws FeignBadResponseWrapper 
	 */
	protected void doDuplicateValidation(Map<String, Object> ml, SiteReport siteReport) throws FeignBadResponseWrapper {
		String msgs = legacyCruService.validateMonitoringLocation(ml, siteReport);
		if(!msgs.equals("{}")) {
			String msgsString = new String(JsonStringEncoder.getInstance().quoteAsString(String.join(", ", msgs)));
			throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": \"" + msgsString + "\"}");
		} else {
			siteReport.addStepReport(new StepReport(SITE_VALIDATE_STEP, HttpStatus.SC_OK, true, SITE_VALIDATE_SUCCESSFUL ));
		}
	}
	
	protected String parseCruClientError(String input) {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<Map<String, List<Object>>> mapType = new TypeReference<Map<String, List<Object>>>() {};
		Map<String, List<Object>> map = new HashMap<>();
		String rtn;
		try {
			map = mapper.readValue(input, mapType);
			rtn = mapper.writeValueAsString(map.get("LegacyCruClient#validateMonitoringLocation(String)").get(0));
		} catch (JsonGenerationException e) {
			rtn = input;
			e.printStackTrace();
		} catch (JsonMappingException e) {
			rtn = input;
			e.printStackTrace();
		} catch (IOException e) {
			rtn = input;
			e.printStackTrace();
		}
		
		return rtn;
	}
}
