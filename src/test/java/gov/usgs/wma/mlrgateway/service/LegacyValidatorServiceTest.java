package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;


import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import java.util.Map;
import net.minidev.json.JSONObject;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@RunWith(SpringRunner.class)
public class LegacyValidatorServiceTest extends BaseSpringTest {

	@MockBean
	private LegacyCruService legacyCruService;
	@MockBean
	private LegacyValidatorClient legacyValidatorClient;

	private LegacyValidatorService service;
	private ObjectMapper mapper;
	private String reportName = "TEST VALIDATOR";
	public static String LEGACY_VALIDATION_ERROR_BODY = "{\"error_message\": \""+LegacyValidatorService.VALIDATION_FAILED+"\"}";
	
	@Before
	public void init() {
		service = new LegacyValidatorService(legacyCruService, legacyValidatorClient);
		BaseController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void validatorService_doValidation_addValidData() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{\"validation_passed_message\": \"Validation passed.\"}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, true);
		
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 1);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"Transaction validated successfully."
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_updateValidData() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{\"validation_passed_message\": \"Validation passed.\"}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, false);
		
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 1);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"Transaction validated successfully."
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_nonExisting() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{\"validation_passed_message\": \"Validation passed.\"}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, true);
		
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 1);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"Transaction validated successfully."
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_cruError() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{\"validation_passed_message\": \"Validation passed.\"}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, true);
		
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 1);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"Transaction validated successfully."
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	
	@Test
	public void validatorService_doValidation_addWarningData() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{\"validation_passed_message\": \"Validation passed.\", \"warning_message\": \"Warnings.\"}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, true);
		
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 2);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
		assertTrue(((Map)mlValid.get("validation")).containsKey("warning_message"));
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"Transaction validated successfully."
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_updateWarningData() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{\"validation_passed_message\": \"Validation passed.\", \"warning_message\": \"Warnings\"}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateUpdate(anyString())).willReturn(validatorResponse);
		
		Map<String, Object> mlValid = service.doValidation(ml, false);
		
		assertTrue(mlValid.containsKey("validation"));
		assertTrue(((Map)mlValid.get("validation")).size() == 2);
		assertTrue(((Map)mlValid.get("validation")).containsKey("validation_passed_message"));
		assertTrue(((Map)mlValid.get("validation")).containsKey("warning_message"));
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"Transaction validated successfully."
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_InvalidData() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{\"fatal_error_message\": \"Fatal Error.\"}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, true);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertTrue(e.getStatus() == 400);
			assertTrue(LEGACY_VALIDATION_ERROR_BODY.equals(e.getBody()));
		}
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":400,\"details\":\"{\\\"error_message\\\": {\\\"fatal_error_message\\\": \\\"Fatal Error.\\\"}}"
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_EmptyValidatorResponse() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, true);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertTrue(e.getStatus() == 500);
			assertTrue(LEGACY_VALIDATION_ERROR_BODY.equals(e.getBody()));
		}
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":500,\"details\":\"{\\\"error_message\\\": {}}"
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_UnknownValidatorResponse() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("{\"invalid_key\":\"some data\"}", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, true);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertTrue(e.getStatus() == 500);
			assertTrue(LEGACY_VALIDATION_ERROR_BODY.equals(e.getBody()));
		}
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":500,\"details\":\"{\\\"error_message\\\": {\\\"invalid_key\\\":\\\"some data\\\"}}"
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_ValidatorErrorResponse() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("Bad Request", HttpStatus.BAD_REQUEST);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, true);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertTrue(e.getStatus() == 400);
			assertTrue(LEGACY_VALIDATION_ERROR_BODY.equals(e.getBody()));
		}
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":400,\"details\":\"{\\\"error_message\\\": \\\"An internal error occurred during validation: Bad Request\\\"}"
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
	
	@Test
	public void validatorService_doValidation_InvalidValidatorResponse() throws Exception {
		Map<String, Object> ml = getAdd();
		ResponseEntity<String> validatorResponse = new ResponseEntity<> ("I'm Not JSON", HttpStatus.OK);
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean())).willReturn(ml);
		given(legacyValidatorClient.validateAdd(anyString())).willReturn(validatorResponse);
		
		try{
			service.doValidation(ml, true);
			fail("Validation should throw an exception when errors are found in order to prevent further processing of this transaction.");
		} catch (FeignBadResponseWrapper e) {
			assertTrue(e.getStatus() == 500);
			assertTrue(LEGACY_VALIDATION_ERROR_BODY.equals(e.getBody()));
		}
		
		//Verify Report Contents
		String expectedReport = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":500,\"details\":\"{\\\"error_message\\\": \\\"Unable to deserialize validator response as JSON: I'm Not JSON\\\"}"
				+ "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}"
				+ "]}";
		JSONAssert.assertEquals(expectedReport, mapper.writeValueAsString(BaseController.getReport()), JSONCompareMode.STRICT);
	}
}
