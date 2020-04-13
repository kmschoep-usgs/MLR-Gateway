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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
public class LegacyCruServiceTest extends BaseSpringTest {
	private String reportName = "TEST LEGACYCRU";
	private LegacyCruService service;
	private ObjectMapper mapper;
	private String fileName = "test.d";
	private String legacyJson = "{}";
	private final String agencyCode = "USGS ";
	private final String siteNumber = "12345678       ";
	private String userName = "userName";
	private String reportDate = "01/01/2019";
	
	@MockBean
	LegacyCruClient legacyCruClient;

	@BeforeEach
	public void init() {
		service = new LegacyCruService(legacyCruClient);
		WorkflowController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
		mapper = new ObjectMapper();
	}
	
	@Test
	public void addTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\","
				+ "\"workflowSteps\":[],"
				+ "\"sites\":[{\"success\":true,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_ADD_STEP + "\",\"httpStatus\":201,\"success\":true,\"details\":\"" 
				+ LegacyCruService.SITE_ADD_SUCCESSFUL + "\"}]}]}";
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
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\",\"workflowSteps\":[],"
				+ "\"sites\":[{\"success\":false,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \","
				+ "\"steps\":[{\"name\":\"" + LegacyCruService.SITE_ADD_STEP + "\",\"httpStatus\":500,\"success\":false,\"details\":\"" 
				+ JSONObject.escape(LegacyCruService.SITE_ADD_FAILED) + "\"}]}]}";
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
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\",\"workflowSteps\":[],"
				+ "\"sites\":[{\"success\":true,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \","
				+ "\"steps\":[{\"name\":\"" + LegacyCruService.SITE_UPDATE_STEP + "\",\"httpStatus\":200,\"success\":true,\"details\":\"" 
				+ LegacyCruService.SITE_UPDATE_SUCCESSFUL + "\"}]}]}";
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
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\",\"workflowSteps\":[],"
				+ "\"sites\":[{\"success\":false,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \","
				+ "\"steps\":[{\"name\":\"" + LegacyCruService.SITE_UPDATE_STEP + "\",\"httpStatus\":500,\"success\":false,\"details\":\"" 
				+ JSONObject.escape(LegacyCruService.SITE_UPDATE_FAILED) + "\"}]}]}";
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
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\",\"workflowSteps\":[],"
				+ "\"sites\":[{\"success\":true,\"agencyCode\":\"USGS \","
				+ "\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_GET_STEP + "\",\"httpStatus\":200,\"success\":true,\"details\":\"" 
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
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\","
				+ "\"userName\":\"userName\",\"workflowSteps\":[],"
				+ "\"sites\":[{\"success\":true,\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_GET_STEP + "\",\"httpStatus\":200,\"success\":true,\"details\":\"" 
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
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\","
				+ "\"workflowSteps\":[],\"sites\":[{\"success\":false,"
				+ "\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_GET_STEP + "\",\"httpStatus\":404,\"success\":false,\"details\":\"" 
				+ JSONObject.escape(LegacyCruService.SITE_GET_DOES_NOT_EXIST_FAILED) + "\"}]}]}";
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.NOT_FOUND);
		given(legacyCruClient.getMonitoringLocation(anyString(), anyString())).willReturn(addRtn);

		try {
			service.getMonitoringLocation(agencyCode, siteNumber, false, siteReport);
			fail("getMonitoringLocation did not throw an exception to its caller");
		} catch (FeignBadResponseWrapper e) {
			assertEquals(404, e.getStatus());
			assertEquals(LegacyCruService.SITE_GET_DOES_NOT_EXIST_FAILED, e.getBody());
		}
		
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocation(anyString(), anyString());
	}
	
	@Test
	public void getLocationAdd_nullSite () throws Exception {
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\","
				+ "\"workflowSteps\":[],\"sites\":[{\"success\":true,"
				+ "\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"Duplicate agency code/site number check: " 
				+ LegacyCruService.SITE_GET_STEP + "\",\"httpStatus\":404,\"success\":true,\"details\":\"" 
				+ LegacyCruService.SITE_GET_DOES_NOT_EXIST_SUCCESSFUL + "\"}]}]}";
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
		
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\","
				+ "\"workflowSteps\":[],\"sites\":[{\"success\":false,"
				+ "\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_NAME_GET_STEP + "\",\"httpStatus\":500,\"success\":false,\"details\":\"" 
				+ JSONObject.escape(LegacyCruService.SITE_NAME_GET_FAILED) + "\"}]}]}";
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
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\",\"workflowSteps\":[],"
				+ "\"sites\":[{\"success\":true,\"agencyCode\":\"USGS \","
				+ "\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_NAME_GET_STEP  + "\",\"httpStatus\":200,\"success\":true,\"details\":\"" 
				+ LegacyCruService.SITE_GET_DOES_NOT_EXIST_SUCCESSFUL + "\"}]}]}";
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
		String msg = "{\"name\":\"TEST LEGACYCRU\",\"inputFileName\":\"test.d\",\"reportDateTime\":\"01/01/2019\",\"userName\":\"userName\",\"workflowSteps\":[],"
				+ "\"sites\":[{\"success\":false,\"agencyCode\":\"USGS \","
				+ "\"siteNumber\":\"12345678       \",\"steps\":[{\"name\":\"" 
				+ LegacyCruService.SITE_NAME_GET_STEP + "\",\"httpStatus\":406,\"success\":false,\"details\":\"" 
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