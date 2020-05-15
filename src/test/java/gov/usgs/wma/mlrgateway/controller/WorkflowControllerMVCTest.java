package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.PermissionEvaluatorImpl;
import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.service.UserAuthService;
import gov.usgs.wma.mlrgateway.config.MethodSecurityConfig;
import gov.usgs.wma.mlrgateway.config.OAuth2Config;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers={WorkflowController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({MvcTestConfig.class, PermissionEvaluatorImpl.class, MethodSecurityConfig.class, OAuth2Config.class})
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
	private UserAuthService userAuthService;

	@MockBean
	private Clock clockMock;

	private MockMultipartFile file;

	@BeforeEach
	public void init() {
		file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		when(clockMock.instant()).thenReturn(Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC")).instant());
		when(userAuthService.getUserName(any(Authentication.class))).thenReturn("test");
		when(userAuthService.getUserEmail(any(Authentication.class))).thenReturn("test@test.test");
	}
	
	@Test
	@WithMockUser(authorities = "test_allowed")
	public void happyPathLegacyWorkflow() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(legacyJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	@WithMockUser
	public void happyPathLegacyWorkflowNoAuthorities() throws Exception {
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/json"));

		verify(legacy, never()).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void badResponse_LegacyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\",\"workflowSteps\":[{\"name\":\"" 
				+ LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED + "\",\"httpStatus\":400,\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],"
				+ "\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(legacy).completeWorkflow(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void serverError_LegacyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+  "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(legacy).completeWorkflow(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void happyPathLegacyValidationWorkflow() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(legacyJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	@WithMockUser
	public void happyPathLegacyValidationWorkflowNoAuthorities() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(legacyJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED + "\",\"httpStatus\":400,"
				+ "\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(legacy).ddotValidation(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void serverError_LegacyValidationWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+ "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(legacy).ddotValidation(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void happyPathUpdatePrimaryKeyWorkflow() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW + "\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		params.set("reasonText", "test");
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.params(params))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(legacyJson));

		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	@WithMockUser
	public void happyPathUpdatePrimaryKeyWorkflowNoAuthorities() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		params.set("reasonText", "test");
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.params(params))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/json"));

		verify(legacy, never()).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	@WithMockUser(authorities = "test_allowed")
	public void badResponse_UpdatePrimaryKeyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW + "\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\",\"workflowSteps\":[{\"name\":\"" 
				+ LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED + "\",\"httpStatus\":400,\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],"
				+ "\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		params.set("reasonText", "test");
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.params(params))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	@WithMockUser(authorities = "test_allowed")
	public void serverError_UpdatePrimaryKeyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW + "\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+  "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		params.set("reasonText", "test");
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.params(params))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	@WithMockUser(authorities = "test_allowed")
	public void badText_UpdatePrimaryKeyWorkflow() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		params.set("reasonText", "test.");
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.params(params))
				.andExpect(status().isBadRequest());

		verify(legacy, never()).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	@WithMockUser(authorities = "test_allowed")
	public void badTextTooLong_UpdatePrimaryKeyWorkflow() throws Exception {
		String textTooLong = "jjjjjjjjjj jjjjjjjjjj jjjjjjjjjj jjjjjjjjjj jjjjjjjjjj jjjjjjjjjj jjjjjjjjjj";
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		params.set("reasonText", textTooLong);
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.params(params))
				.andExpect(status().isBadRequest());

		verify(legacy, never()).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void expiredToken_LegacyValidateUpdateWorkflow() throws Exception {
		doThrow(new ClientAuthorizationRequiredException("test-client")).when(userAuthService).validateToken(any());
		
		// First time the token is expired it returns 401 and clears session
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType("application/json"));
		
		// Subsequent requests should cause login redirect since the session is gone
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file))
				.andExpect(status().is3xxRedirection());

		verify(legacy, never()).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void expiredToken_LegacyValidateWorkflow() throws Exception {
		doThrow(new ClientAuthorizationRequiredException("test-client")).when(userAuthService).validateToken(any());
		
		// First time the token is expired it returns 401 and clears session
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType("application/json"));

		// Subsequent requests should cause login redirect since the session is gone
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file))
				.andExpect(status().is3xxRedirection());

		verify(legacy, never()).ddotValidation(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(authorities = "test_allowed")
	public void expiredToken_PKUpdateWorkflow() throws Exception {
		doThrow(new ClientAuthorizationRequiredException("test-client")).when(userAuthService).validateToken(any());
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		params.set("reasonText", "text");

		// First time the token is expired it returns 401 and clears session
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update").params(params))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType("application/json"));

		// Subsequent requests should cause login redirect since the session is gone
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update").params(params))
			.andExpect(status().is3xxRedirection());

		verify(legacy, never()).updatePrimaryKeyWorkflow(any(), any(), any(), any(), any());
	}
}