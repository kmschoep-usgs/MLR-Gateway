package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc()
@ActiveProfiles("test")
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
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file)
					.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				)
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(legacyJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void badResponse_LegacyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\",\"workflowSteps\":[{\"name\":\"" 
				+ LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED + "\",\"httpStatus\":400,\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],"
				+ "\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(legacy).completeWorkflow(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file)
					.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+  "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(legacy).completeWorkflow(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots").file(file)
					.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				)
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void happyPathLegacyValidationWorkflow() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file)
					.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				)
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(legacyJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED + "\",\"httpStatus\":400,"
				+ "\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(legacy).ddotValidation(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file)
					.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyValidationWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW + "\",\"inputFileName\":\"d.\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+ "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(legacy).ddotValidation(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/ddots/validate").file(file)
					.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				)
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void happyPathUpdatePrimaryKeyWorkflow() throws Exception {
		String legacyJson = "{\"name\":\"" + LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW + "\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				.params(params))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(legacyJson));

		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	public void badResponse_UpdatePrimaryKeyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW + "\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\",\"workflowSteps\":[{\"name\":\"" 
				+ LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED + "\",\"httpStatus\":400,\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],"
				+ "\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				.params(params))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	public void serverError_UpdatePrimaryKeyWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW + "\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+  "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.set("oldAgencyCode", "USGS");
		params.set("newAgencyCode", "BLAH");
		params.set("oldSiteNumber", "123345");
		params.set("newSiteNumber", "9999090");
		mvc.perform(MockMvcRequestBuilders.post("/workflows/primaryKey/update")
				.headers(getAuthHeaders("test", "test@test.gov", "test_allowed"))
				.params(params))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());
	}
	
	public HttpHeaders getAuthHeaders(String username, String email, String ... roles) throws UnsupportedEncodingException {
		HttpHeaders headers = new HttpHeaders();
		String jwtToken = JWT.create()
			.withClaim("user_name", username)
			.withClaim("email", email)
			.withArrayClaim("authorities", roles)
			.sign(Algorithm.HMAC256("secret"));
		headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
		return headers;
	}
}
