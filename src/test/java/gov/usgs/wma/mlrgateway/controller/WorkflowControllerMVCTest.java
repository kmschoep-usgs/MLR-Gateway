package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

	MockMultipartFile file;

	@Before
	public void init() {
		file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
	}

	@Test
	public void happyPathLegacyWorkflow() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"status\":200,\"steps\":[]}";

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots")
				.file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(legacyJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void badRepsonse_LegacyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"status\":400,\"steps\":[{\"name\":\"" 
				+ LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"status\":400,\"details\":\"{\\\"error\\\": 123}\"}]}";
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
		String badJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"status\":500,\"steps\":[{\"name\":\""
				+ LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"status\":500,\"details\":\"wow 456\"}]}";
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
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"status\":200,\"steps\":[]}";

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots/validate")
				.file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(legacyJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"status\":400,\"steps\":[{\"name\":\"" 
				+ LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"status\":400,\"details\":\"{\\\"error\\\": 123}\"}]}";
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
		String badJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"status\":500,\"steps\":[{\"name\":\""
				+ LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"status\":500,\"details\":\"wow 456\"}]}";
		willThrow(new RuntimeException("wow 456")).given(legacy).ddotValidation(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots/validate")
				.file(file))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

}
