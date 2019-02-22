package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;

@RunWith(SpringRunner.class)
@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(secure=false)
public class WorkflowControllerMVCTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private LegacyWorkflowService legacy;
	
	@MockBean
	private NotificationService notificationService;
	
	@MockBean
	private NotificationClient notificationClient;

	@MockBean
	private DdotClient ddotClient;

	@MockBean
	private LegacyCruClient legacyClient;

	@MockBean
	private DiscoveryClient dc;

	@MockBean
	private SpringClientFactory scf;
	
	@MockBean
	private Clock clockMock;
	
	@MockBean
	private OAuth2Authentication auth;

	MockMultipartFile file;
	
	@Before
	public void init() {
		file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		when(clockMock.instant()).thenReturn(Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC")).instant());
	}
	
	@Test
	public void happyPathLegacyWorkflow() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"Unknown\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots")
				.file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(legacyJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void badResponse_LegacyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"Unknown\",\"workflowSteps\":[{\"name\":\"" 
				+ LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED + "\",\"httpStatus\":400,\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],"
				+ "\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(legacy).completeWorkflow(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots")
				.file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"Unknown\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+  "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(legacy).completeWorkflow(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots")
				.file(file))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void happyPathLegacyValidationWorkflow() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"Unknown\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots/validate")
				.file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(legacyJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"Unknown\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED + "\",\"httpStatus\":400,"
				+ "\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(legacy).ddotValidation(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots/validate")
				.file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyValidationWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"Unknown\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+ "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(legacy).ddotValidation(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots/validate")
				.file(file))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

}
