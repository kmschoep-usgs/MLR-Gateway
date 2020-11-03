package gov.usgs.wma.mlrgateway.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@ExtendWith(SpringExtension.class)
public class LegacyValidatorServiceTest extends BaseSpringTest {

	@MockBean
	private LegacyCruService legacyCruService;
	@MockBean
	private LegacyValidatorClient legacyValidatorClient;

	private LegacyValidatorService service;
	private String reportName = "TEST VALIDATOR";
	private String fileName = "test.d";
	private final String agencyCode = "USGS ";
	private final String siteNumber = "12345678       ";
	private String userName = "userName";
	private String reportDate = "01/01/2019";
	public static String LEGACY_VALIDATION_ERROR_BODY = LegacyValidatorService.VALIDATION_FAILED;
	
	@BeforeEach
	public void init() {
		service = new LegacyValidatorService(legacyCruService, legacyValidatorClient);
		BaseController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void validatorService_doValidation_addValidData() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, existingRecord, true, siteReport);
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 1);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void validatorService_doValidation_updateValidData() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, existingRecord, false, siteReport);
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 1);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void validatorService_doValidation_nonExisting() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, existingRecord, true, siteReport);
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 1);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));

	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void validatorService_doValidation_addWarningData() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\", \"warning_message\": \"Warnings.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, existingRecord, true, siteReport);
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 2);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
		assertTrue(((Map)mlValid.get("validation")).containsKey("warning_message"));
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void validatorService_doValidation_updateWarningData() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\", \"warning_message\": \"Warnings.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, existingRecord, false, siteReport);
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 2);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
		assertTrue(((Map)mlValid.get("validation")).containsKey("warning_message"));

	}
	
	@Test
	public void validatorService_doValidation_InvalidData() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"fatal_error_message\": \"Fatal Error.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, existingRecord, true, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(400, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}
		
		GatewayReport gatewayReport = WorkflowController.getReport();
		gatewayReport.addSiteReport(siteReport);
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doValidation_EmptyValidatorResponse() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, existingRecord, true, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(500, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertEquals("500", siteReport.getSteps().get(1).getHttpStatus().toString());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doValidation_UnknownValidatorResponse() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"invalid_key\":\"some data\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, existingRecord, true, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(500, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertEquals("500", siteReport.getSteps().get(1).getHttpStatus().toString());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doValidation_ValidatorErrorResponse() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "Bad Request";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.BAD_REQUEST);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, existingRecord, true, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(400, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"error_message\": \"An internal error occurred during validation: " + responseMsg + "\"}", siteReport.getSteps().get(1).getDetails());
		assertEquals("400", siteReport.getSteps().get(1).getHttpStatus().toString());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doValidation_InvalidValidatorResponse() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "I'm Not JSON";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, existingRecord, true, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(500, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}

		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"error_message\": \"Unable to deserialize validator response as JSON: " + responseMsg + "\"}", siteReport.getSteps().get(1).getDetails());
		assertEquals("500", siteReport.getSteps().get(1).getHttpStatus().toString());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doValidation_DuplicateValidationFailure() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\", \"warning_message\": \"Warnings.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("Error");

		try{
			service.doValidation(ml, existingRecord, true, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			int status = e.getStatus();
			assertEquals(400, status);
			String body = e.getBody();
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, body);
		}

		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals("{\"error_message\": \"Error\"}", siteReport.getSteps().get(0).getDetails());
		assertFalse(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doValidation_DuplicateValidationError() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\", \"warning_message\": \"Warnings.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willThrow(new RuntimeException("error"));

		try{
			service.doValidation(ml, existingRecord, true, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			int status = e.getStatus();
			assertEquals(500, status);
			String body = e.getBody();
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, body);
		}
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals("error", siteReport.getSteps().get(0).getDetails());
		assertFalse(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doValidation_DuplicateValidationFailure_InvalidData() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"fatal_error_message\": \"Fatal Error.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("Error");

		try{
			service.doValidation(ml, existingRecord, true, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			int status = e.getStatus();
			assertEquals(400, status);
			String body = e.getBody();
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, body);
		}

		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals("{\"error_message\": \"Error\"}", siteReport.getSteps().get(0).getDetails());
		assertFalse(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void validatorService_doPKValidation_updateValidData() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doPKValidation(ml, existingRecord, siteReport);
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 1);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
	}
	
	@Test
	public void validatorService_doPKValidation_InvalidData() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"fatal_error_message\": \"Fatal Error.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		try{
			service.doPKValidation(ml, existingRecord, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(400, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}
		
		GatewayReport gatewayReport = WorkflowController.getReport();
		gatewayReport.addSiteReport(siteReport);
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doPKValidation_EmptyValidatorResponse() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		try{
			service.doPKValidation(ml, existingRecord, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(500, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertEquals("500", siteReport.getSteps().get(1).getHttpStatus().toString());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doPKValidation_UnknownValidatorResponse() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"invalid_key\":\"some data\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		try{
			service.doPKValidation(ml, existingRecord, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(500, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertEquals("500", siteReport.getSteps().get(1).getHttpStatus().toString());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doPKValidation_ValidatorErrorResponse() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "Bad Request";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.BAD_REQUEST);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		try{
			service.doPKValidation(ml, existingRecord, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(400, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"error_message\": \"An internal error occurred during validation: " + responseMsg + "\"}", siteReport.getSteps().get(1).getDetails());
		assertEquals("400", siteReport.getSteps().get(1).getHttpStatus().toString());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doPKValidation_InvalidValidatorResponse() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "I'm Not JSON";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("{}");
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		try{
			service.doPKValidation(ml, existingRecord, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(500, e.getStatus());
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, e.getBody());
		}

		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_SUCCESSFUL, siteReport.getSteps().get(0).getDetails());
		assertTrue(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"error_message\": \"Unable to deserialize validator response as JSON: " + responseMsg + "\"}", siteReport.getSteps().get(1).getDetails());
		assertEquals("500", siteReport.getSteps().get(1).getHttpStatus().toString());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doPKValidation_DuplicateValidationFailure() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\", \"warning_message\": \"Warnings.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("Error");

		try{
			service.doPKValidation(ml, existingRecord, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			int status = e.getStatus();
			assertEquals(400, status);
			String body = e.getBody();
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, body);
		}

		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals("{\"error_message\": \"Error\"}", siteReport.getSteps().get(0).getDetails());
		assertFalse(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_dopKValidation_DuplicateValidationError() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"validation_passed_message\": \"Validation passed.\", \"warning_message\": \"Warnings.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willThrow(new RuntimeException("error"));

		try{
			service.doPKValidation(ml, existingRecord, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			int status = e.getStatus();
			assertEquals(500, status);
			String body = e.getBody();
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, body);
		}
		
		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals("error", siteReport.getSteps().get(0).getDetails());
		assertFalse(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertTrue(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	@Test
	public void validatorService_doPKValidation_DuplicateValidationFailure_InvalidData() throws Exception {
		Map<String, Object> ml = getUpdatePK();
		Map<String, Object> existingRecord = getAdd();
		String responseMsg = "{\"fatal_error_message\": \"Fatal Error.\"}";
		ResponseEntity<String> validatorResponse = new ResponseEntity<> (responseMsg, HttpStatus.OK);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(ml);
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		given(legacyCruService.validateMonitoringLocation(any(), any())).willReturn("Error");

		try{
			service.doPKValidation(ml, existingRecord, siteReport);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			int status = e.getStatus();
			assertEquals(400, status);
			String body = e.getBody();
			assertEquals(LEGACY_VALIDATION_ERROR_BODY, body);
		}

		assertEquals(2, siteReport.getSteps().size());
		assertEquals(LegacyValidatorService.SITE_VALIDATE_STEP, siteReport.getSteps().get(0).getName());
		assertEquals("{\"error_message\": \"Error\"}", siteReport.getSteps().get(0).getDetails());
		assertFalse(siteReport.getSteps().get(0).isSuccess());
		assertEquals(LegacyValidatorService.VALIDATION_STEP, siteReport.getSteps().get(1).getName());
		assertEquals("{\"validator_message\": " + responseMsg + "}", siteReport.getSteps().get(1).getDetails());
		assertFalse(siteReport.getSteps().get(1).isSuccess());
		assertFalse(siteReport.isSuccess());
	}
	
	
}
