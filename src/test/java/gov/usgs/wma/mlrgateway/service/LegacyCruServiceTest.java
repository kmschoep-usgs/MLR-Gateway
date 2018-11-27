package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import java.util.HashMap;
import java.util.Map;
import net.minidev.json.JSONObject;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
public class LegacyCruServiceTest extends BaseSpringTest {
	private String reportName = "TEST LEGACYCRU";
	private LegacyCruService service;
	private ObjectMapper mapper;
	private String fileName = "test.d";
	private String legacyJson = "{}";
	private final String agencyCode = "USGS ";
	private final String siteNumber = "12345678       ";
	
	@MockBean
	LegacyCruClient legacyCruClient;

	@Before
	public void init() {
		service = new LegacyCruService(legacyCruClient);
		WorkflowController.setReport(new GatewayReport(reportName, fileName));
		mapper = new ObjectMapper();
	}
	
	@Test
	public void addTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,\"workflowStep\":null,\"ddotIngesterStep\":null,\"notificationStep\":null,\"sites\":[{\"isSuccess\":true,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_ADD_STEP + "\",\"httpStatus\":201,\"isSuccess\":true,\"details\":\"" 
				+ LegacyCruService.SITE_ADD_SUCCESSFULL + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.CREATED);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(addRtn);

		service.addTransaction(agencyCode, siteNumber, "{}", siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
	}

	@Test
	public void addTransaction_throwsException() throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,\"workflowStep\":null,\"ddotIngesterStep\":null,"
				+ "\"notificationStep\":null,\"sites\":[{\"isSuccess\":false,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \","
				+ "\"steps\":[{\"name\":\"" + LegacyCruService.SITE_ADD_STEP + "\",\"httpStatus\":500,\"isSuccess\":false,\"details\":\"" 
				+ LegacyCruService.SITE_ADD_FAILED + "\"}]}]}";
		given(legacyCruClient.createMonitoringLocation(anyString())).willThrow(new RuntimeException());
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		try {
			service.addTransaction(agencyCode, siteNumber, "{}", siteReport);
			fail("addTransaction did not throw an exception to its caller");
		} catch(Exception e) {}
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
	}
	
	@Test
	public void updateTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,\"workflowStep\":null,\"ddotIngesterStep\":null,"
				+ "\"notificationStep\":null,\"sites\":[{\"isSuccess\":true,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \","
				+ "\"steps\":[{\"name\":\"" + LegacyCruService.SITE_UPDATE_STEP + "\",\"httpStatus\":200,\"isSuccess\":true,\"details\":\"" 
				+ LegacyCruService.SITE_UPDATE_SUCCESSFULL + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.patchMonitoringLocation(anyString())).willReturn(legacyRtn);
		

		service.updateTransaction(agencyCode, siteNumber, "{}", siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
	}

	@Test
	public void updateTransaction_throwsException() throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,\"workflowStep\":null,\"ddotIngesterStep\":null,"
				+ "\"notificationStep\":null,\"sites\":[{\"isSuccess\":false,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \","
				+ "\"steps\":[{\"name\":\"" + LegacyCruService.SITE_UPDATE_STEP + "\",\"httpStatus\":500,\"isSuccess\":false,\"details\":\"" 
				+ LegacyCruService.SITE_UPDATE_FAILED + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruClient.patchMonitoringLocation(anyString())).willThrow(new RuntimeException());
		
		try {
			service.updateTransaction(agencyCode, siteNumber, "{}", siteReport);
			fail("addTransaction did not throw an exception to its caller");
		} catch(Exception e) {}
		
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
	}
	
	@Test
	public void getLocationUpdate_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,\"workflowStep\":null,"
				+ "\"ddotIngesterStep\":null,\"notificationStep\":null,\"sites\":[{\"isSuccess\":true,\"agencyCode\":\"USGS \","
				+ "\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_GET_STEP + "\",\"httpStatus\":200,\"isSuccess\":true,\"details\":\"" 
				+ LegacyCruService.SITE_GET_SUCCESSFULL + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		service.getMonitoringLocation(agencyCode, siteNumber, false, siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void getLocationAdd_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,"
				+ "\"userName\":null,\"workflowStep\":null,\"ddotIngesterStep\":null,\"notificationStep\":null,"
				+ "\"sites\":[{\"isSuccess\":true,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_GET_STEP + "\",\"httpStatus\":200,\"isSuccess\":true,\"details\":\"" 
				+ LegacyCruService.SITE_GET_SUCCESSFULL + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		service.getMonitoringLocation(agencyCode, siteNumber, true, siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void getLocationUpdate_nullSite () throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,"
				+ "\"workflowStep\":null,\"ddotIngesterStep\":null,\"notificationStep\":null,\"sites\":[{\"isSuccess\":false,"
				+ "\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_GET_STEP + "\",\"httpStatus\":404,\"isSuccess\":false,\"details\":\"" 
				+ LegacyCruService.SITE_GET_DOES_NOT_EXIST + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.NOT_FOUND);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		service.getMonitoringLocation(agencyCode, siteNumber, false, siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void getLocationAdd_nullSite () throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,"
				+ "\"workflowStep\":null,\"ddotIngesterStep\":null,\"notificationStep\":null,\"sites\":[{\"isSuccess\":true,"
				+ "\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"Duplicate agency code/site number check: " 
				+ LegacyCruService.SITE_GET_STEP + "\",\"httpStatus\":404,\"isSuccess\":true,\"details\":\"" 
				+ LegacyCruService.SITE_GET_DOES_NOT_EXIST + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.NOT_FOUND);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		service.getMonitoringLocation(agencyCode, siteNumber, true, siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void validateMonitoringLocation_MlSerializationError () throws Exception {
		ObjectMapper mockMapper = mock(ObjectMapper.class);
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(mockMapper.writeValueAsString(any())).willThrow(JsonProcessingException.class);
		service.setMapper(mockMapper);
		
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,"
				+ "\"workflowStep\":null,\"ddotIngesterStep\":null,\"notificationStep\":null,\"sites\":[{\"isSuccess\":false,"
				+ "\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_VALIDATE_STEP + "\",\"httpStatus\":500,\"isSuccess\":false,\"details\":\"" 
				+ LegacyCruService.SITE_VALIDATE_FAILED + "\"}]}]}";
		try {
			service.validateMonitoringLocation(new HashMap<>(), siteReport);
			fail();
		} catch (FeignBadResponseWrapper e) {
			assertNotNull(e);
		}
		
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		String actualStepReport = mapper.writeValueAsString(rtn);
		JSONAssert.assertEquals(msg, actualStepReport, JSONCompareMode.STRICT);
	}
	
	@Test
	public void validateMonitoringLocation_CruValidationMessageDeserializationError () throws Exception {
		given(legacyCruClient.validateMonitoringLocation(anyString())).willReturn(new ResponseEntity<>("NOT VALID JSON", HttpStatus.INTERNAL_SERVER_ERROR));
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,"
				+ "\"workflowStep\":null,\"ddotIngesterStep\":null,\"notificationStep\":null,\"sites\":[{\"isSuccess\":false,"
				+ "\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_VALIDATE_STEP + "\",\"httpStatus\":500,\"isSuccess\":false,\"details\":\"" 
				+ LegacyCruService.SITE_VALIDATE_FAILED + "\"}]}]}";
		try {
			service.validateMonitoringLocation(new HashMap<>(), siteReport);
			fail();
		} catch (FeignBadResponseWrapper e) {
			assertNotNull(e);
		}
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		String actualStepReport = mapper.writeValueAsString(rtn);
		JSONAssert.assertEquals(msg, actualStepReport, JSONCompareMode.STRICT);
	}
	
	@Test
	public void validateMonitoringLocation_ValidationSuccessful () throws Exception {
		given(legacyCruClient.validateMonitoringLocation(anyString())).willReturn(new ResponseEntity<>("[]", HttpStatus.OK));
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,\"workflowStep\":null,"
				+ "\"ddotIngesterStep\":null,\"notificationStep\":null,\"sites\":[{\"isSuccess\":true,\"agencyCode\":\"USGS \","
				+ "\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_VALIDATE_STEP  + "\",\"httpStatus\":200,\"isSuccess\":true,\"details\":\"" 
				+ LegacyCruService.SITE_VALIDATE_SUCCESSFUL + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		Map<String, Object> input = new HashMap<>();
		input.put(LegacyWorkflowService.AGENCY_CODE, agencyCode);
		input.put(LegacyWorkflowService.SITE_NUMBER, siteNumber);
		
		service.validateMonitoringLocation(input, siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		String actualStepReport = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actualStepReport, JSONCompareMode.STRICT);
	}
	
	@Test
	public void validateMonitoringLocation_ValidationFailed () throws Exception {
		String validationErrorMessages = "[\"it was bad\",\"it was not good\"]";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(legacyCruClient.validateMonitoringLocation(anyString())).willReturn(new ResponseEntity<>(validationErrorMessages, HttpStatus.NOT_ACCEPTABLE));
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":null,\"userName\":null,\"workflowStep\":null,"
				+ "\"ddotIngesterStep\":null,\"notificationStep\":null,\"sites\":[{\"isSuccess\":false,\"agencyCode\":\"USGS \","
				+ "\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_VALIDATE_STEP + "\",\"httpStatus\":406,\"isSuccess\":false,\"details\":\"" 
				+ JSONObject.escape(validationErrorMessages) + "\"}]}]}";
		Map<String, Object> input = new HashMap<>();
		input.put(LegacyWorkflowService.AGENCY_CODE, agencyCode);
		input.put(LegacyWorkflowService.SITE_NUMBER, siteNumber);
		
		service.validateMonitoringLocation(input, siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		String actualStepReport = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actualStepReport, JSONCompareMode.STRICT);
	}
}