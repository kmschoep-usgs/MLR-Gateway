package gov.usgs.wma.mlrgateway.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@RunWith(SpringRunner.class)
public class WorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private NotificationService notify;
	@MockBean
	private LegacyWorkflowService legacy;
	@MockBean
	private OAuth2Authentication authentication;
	@MockBean
	private OAuth2Request mockOAuth2Request;
	
	@Bean
	@Primary
	public Clock clock() {
		return Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC"));
	}

	private WorkflowController controller;
	private MockHttpServletResponse response;
	private Map<String, Serializable> testEmail;

	@Before
	public void init() {
		testEmail = new HashMap<>();
		testEmail.put("email", "localuser@example.gov");
		controller = new WorkflowController(legacy, notify, clock());
		response = new MockHttpServletResponse();
	}

	@Test
	public void happyPath_LegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		when(authentication.getOAuth2Request()).thenReturn(mockOAuth2Request);
		when(mockOAuth2Request.getExtensions()).thenReturn(testEmail); 
		UserSummaryReport rtn = controller.legacyWorkflow(file, response, authentication);
		assertEquals(LegacyWorkflowService.COMPLETE_WORKFLOW, rtn.getName() );
		assertEquals(new ArrayList<>(), rtn.getWorkflowSteps());
		assertEquals(new ArrayList<>(), rtn.getSites());
		assertEquals("d.", rtn.getInputFileName());
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void badBackingServiceRequest_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).completeWorkflow(any(MultipartFile.class));

		UserSummaryReport rtn = controller.legacyWorkflow(file, response, authentication);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(LegacyWorkflowService.COMPLETE_WORKFLOW, rtn.getName());
		assertEquals("400", completeWorkflowStep.getHttpStatus().toString());
		assertEquals(LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED, completeWorkflowStep.getName());
		assertEquals(badText, completeWorkflowStep.getDetails());
		assertEquals("d.", rtn.getInputFileName());
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(legacy).completeWorkflow(any(MultipartFile.class));
		UserSummaryReport rtn = controller.legacyWorkflow(file, response, authentication);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(LegacyWorkflowService.COMPLETE_WORKFLOW, rtn.getName());
		assertEquals("500", completeWorkflowStep.getHttpStatus().toString());
		assertEquals(LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED, completeWorkflowStep.getName());
		assertEquals(badText, completeWorkflowStep.getDetails());
		assertEquals("d.", rtn.getInputFileName());
		
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void happyPath_LegacyValidationWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		when(authentication.getOAuth2Request()).thenReturn(mockOAuth2Request);
		when(mockOAuth2Request.getExtensions()).thenReturn(testEmail); 
		UserSummaryReport userSummaryReport = controller.legacyValidationWorkflow(file, response, authentication);	
		assertEquals(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW, userSummaryReport.getName());
		assertEquals(new ArrayList<>(), userSummaryReport.getWorkflowSteps());
		assertEquals(new ArrayList<>(), userSummaryReport.getSites());
		assertEquals("d.", userSummaryReport.getInputFileName());
		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		UserSummaryReport rtn = controller.legacyValidationWorkflow(file, response, authentication);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW, rtn.getName());
		assertEquals("400", completeWorkflowStep.getHttpStatus().toString());
		assertEquals(badText, completeWorkflowStep.getDetails());
		assertEquals(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED, completeWorkflowStep.getName());

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		UserSummaryReport rtn = controller.legacyValidationWorkflow(file, response, authentication);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW, rtn.getName());
		assertEquals("500", completeWorkflowStep.getHttpStatus().toString());
		assertEquals(badText, completeWorkflowStep.getDetails());
		assertEquals(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED, completeWorkflowStep.getName());

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}
	
	@Test
	public void happyPath_UpdatePrimaryKeyWorkflow() throws Exception {
		String oldAgencyCode = "USGS";
		String newAgencyCode = "BLAH";
		String oldSiteNumber = "123456789";
		String newSiteNumber = "987654321";
		when(authentication.getOAuth2Request()).thenReturn(mockOAuth2Request);
		when(mockOAuth2Request.getExtensions()).thenReturn(testEmail); 
		UserSummaryReport rtn = controller.updatePrimaryKeyWorkflow(oldAgencyCode, newAgencyCode, oldSiteNumber, newSiteNumber, response, authentication);
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW, rtn.getName() );
		assertEquals(new ArrayList<>(), rtn.getWorkflowSteps());
		assertEquals(new ArrayList<>(), rtn.getSites());
		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	public void badBackingServiceRequest_UpdatePrimaryKeyWorkflow() throws Exception {
		String badText = "This is really bad.";
		String oldAgencyCode = "USGS";
		String newAgencyCode = "BLAH";
		String oldSiteNumber = "123456789";
		String newSiteNumber = "987654321";
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());

		UserSummaryReport rtn = controller.updatePrimaryKeyWorkflow(oldAgencyCode, newAgencyCode, oldSiteNumber, newSiteNumber, response, authentication);
		StepReport updatePrimaryKeyWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW, rtn.getName());
		assertEquals("400", updatePrimaryKeyWorkflowStep.getHttpStatus().toString());
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED, updatePrimaryKeyWorkflowStep.getName());
		assertEquals(badText, updatePrimaryKeyWorkflowStep.getDetails());

		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	public void serverError_UpdatePrimaryKeyWorkflow() throws Exception {
		String badText = "This is really bad.";
		String oldAgencyCode = "USGS";
		String newAgencyCode = "BLAH";
		String oldSiteNumber = "123456789";
		String newSiteNumber = "987654321";
		willThrow(new HystrixBadRequestException(badText)).given(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());
		UserSummaryReport rtn = controller.updatePrimaryKeyWorkflow(oldAgencyCode, newAgencyCode, oldSiteNumber, newSiteNumber, response, authentication);
		StepReport updatePrimaryKeyWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW, rtn.getName());
		assertEquals("500", updatePrimaryKeyWorkflowStep.getHttpStatus().toString());
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED, updatePrimaryKeyWorkflowStep.getName());
		assertEquals(badText, updatePrimaryKeyWorkflowStep.getDetails());
		
		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString());
	}

}
