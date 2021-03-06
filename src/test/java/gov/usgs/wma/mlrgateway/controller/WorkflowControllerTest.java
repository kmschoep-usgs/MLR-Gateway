package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.service.UserAuthService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@ExtendWith(SpringExtension.class)
public class WorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private NotificationService notify;
	@MockBean
	private LegacyWorkflowService legacy;
	@MockBean
	private UserAuthService userAuthService;
	
	@Bean
	@Primary
	public Clock clock() {
		return Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC"));
	}

	private WorkflowController controller;
	private MockHttpServletResponse response;
	private Map<String, Serializable> testEmail;
	private UsernamePasswordAuthenticationToken mockAuth = new UsernamePasswordAuthenticationToken("user", "pass");

	@BeforeEach
	public void init() {
		given(userAuthService.getUserEmail(any(Authentication.class))).willReturn("test@test");
		given(userAuthService.getUserName(any(Authentication.class))).willReturn("test");
		given(notify.buildEmailSubject(anyString())).willReturn("test Subject");
		testEmail = new HashMap<>();
		testEmail.put("email", "localuser@example.gov");
		controller = new WorkflowController(legacy, notify, userAuthService, clock());
		response = new MockHttpServletResponse();
		controller.setEnablePrimaryKeyUpdate(true);
	}

	@Test
	public void happyPath_LegacyWorkflow() throws Exception {
		controller.setNotificationEmailCCListString("testCCList@test");
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		UserSummaryReport rtn = controller.legacyWorkflow(file, response, mockAuth);
		assertEquals(LegacyWorkflowService.COMPLETE_WORKFLOW, rtn.getName() );
		assertEquals(new ArrayList<>(), rtn.getWorkflowSteps());
		assertEquals(new ArrayList<>(), rtn.getSites());
		assertEquals("d.", rtn.getInputFileName());
		verify(legacy).completeWorkflow(any(MultipartFile.class));
		verify(notify).sendNotification(anyList(), anyList(), anyString(), anyString(), anyString(), any());
	}
	
	@Test
	public void noUserEmail_LegacyWorkflow() throws Exception {
		given(userAuthService.getUserEmail(any(Authentication.class))).willReturn(null);
		controller.setNotificationEmailCCListString("testCCList@test");
		String errorMessage = "{\"error_message\": \"Could not find valid user email in security context.\"}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		UserSummaryReport rtn = controller.legacyWorkflow(file, response, mockAuth);
		assertEquals(LegacyWorkflowService.COMPLETE_WORKFLOW, rtn.getName() );
		assertEquals(errorMessage, rtn.getWorkflowSteps().get(0).getDetails());
		assertEquals(new ArrayList<>(), rtn.getSites());
		assertEquals("d.", rtn.getInputFileName());
		verify(legacy).completeWorkflow(any(MultipartFile.class));
		verify(notify).sendNotification(anyList(), anyList(), anyString(), anyString(), anyString(), any());
	}
	
	@Test
	public void noUserEmail_noCCListLegacyWorkflow() throws Exception {
		given(userAuthService.getUserEmail(any(Authentication.class))).willReturn(null);
		controller.setNotificationEmailCCListString(null);
		String errorMessage = "{\"error_message\": \"Could not find valid user email in security context.\"}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		UserSummaryReport rtn = controller.legacyWorkflow(file, response, mockAuth);
		assertEquals(LegacyWorkflowService.COMPLETE_WORKFLOW, rtn.getName() );
		assertEquals(errorMessage, rtn.getWorkflowSteps().get(0).getDetails());
		assertEquals(new ArrayList<>(), rtn.getSites());
		assertEquals("d.", rtn.getInputFileName());
		verify(legacy).completeWorkflow(any(MultipartFile.class));
		verify(notify).sendNotification(anyList(), anyList(), anyString(), anyString(), anyString(), any());
	}

	@Test
	public void badBackingServiceRequest_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).completeWorkflow(any(MultipartFile.class));

		UserSummaryReport rtn = controller.legacyWorkflow(file, response, mockAuth);
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
		UserSummaryReport rtn = controller.legacyWorkflow(file, response, mockAuth);
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
		UserSummaryReport userSummaryReport = controller.legacyValidationWorkflow(file, response, mockAuth);	
		assertEquals(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW, userSummaryReport.getName());
		assertEquals(new ArrayList<>(), userSummaryReport.getWorkflowSteps());
		assertEquals(new ArrayList<>(), userSummaryReport.getSites());
		assertEquals("d.", userSummaryReport.getInputFileName());
		verify(legacy).ddotValidation(any(MultipartFile.class));
	}
	
	@Test
	public void noUserEmail_LegacyValidationWorkflow() throws Exception {
		given(userAuthService.getUserEmail(any(Authentication.class))).willReturn(null);
		String errorMessage = "{\"error_message\": \"Could not find valid user email in security context.\"}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		UserSummaryReport userSummaryReport = controller.legacyValidationWorkflow(file, response, mockAuth);	
		assertEquals(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW, userSummaryReport.getName());
		assertEquals(errorMessage, userSummaryReport.getWorkflowSteps().get(0).getDetails());
		assertEquals(new ArrayList<>(), userSummaryReport.getSites());
		assertEquals("d.", userSummaryReport.getInputFileName());
		verify(legacy).ddotValidation(any(MultipartFile.class));
		verify(notify).sendNotification(anyList(), anyList(), anyString(), anyString(), anyString(), any());
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		UserSummaryReport rtn = controller.legacyValidationWorkflow(file, response, mockAuth);
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

		UserSummaryReport rtn = controller.legacyValidationWorkflow(file, response, mockAuth);
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
		String reasonText = "primary key change";
		UserSummaryReport rtn = controller.updatePrimaryKeyWorkflow(oldAgencyCode, newAgencyCode, oldSiteNumber, newSiteNumber, reasonText, response, mockAuth);
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW, rtn.getName() );
		assertEquals(new ArrayList<>(), rtn.getWorkflowSteps());
		assertEquals(new ArrayList<>(), rtn.getSites());
		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	public void badBackingServiceRequest_UpdatePrimaryKeyWorkflow() throws Exception {
		String badText = "This is really bad.";
		String oldAgencyCode = "USGS";
		String newAgencyCode = "BLAH";
		String oldSiteNumber = "123456789";
		String newSiteNumber = "987654321";
		String reasonText = "primary key change";
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());

		UserSummaryReport rtn = controller.updatePrimaryKeyWorkflow(oldAgencyCode, newAgencyCode, oldSiteNumber, newSiteNumber, reasonText, response, mockAuth);
		StepReport updatePrimaryKeyWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW, rtn.getName());
		assertEquals("400", updatePrimaryKeyWorkflowStep.getHttpStatus().toString());
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED, updatePrimaryKeyWorkflowStep.getName());
		assertEquals(badText, updatePrimaryKeyWorkflowStep.getDetails());

		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	public void serverError_UpdatePrimaryKeyWorkflow() throws Exception {
		String badText = "This is really bad.";
		String oldAgencyCode = "USGS";
		String newAgencyCode = "BLAH";
		String oldSiteNumber = "123456789";
		String newSiteNumber = "987654321";
		String reasonText = "primary key change";
		willThrow(new HystrixBadRequestException(badText)).given(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
		UserSummaryReport rtn = controller.updatePrimaryKeyWorkflow(oldAgencyCode, newAgencyCode, oldSiteNumber, newSiteNumber, reasonText, response, mockAuth);
		StepReport updatePrimaryKeyWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW, rtn.getName());
		assertEquals("500", updatePrimaryKeyWorkflowStep.getHttpStatus().toString());
		assertEquals(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED, updatePrimaryKeyWorkflowStep.getName());
		assertEquals(badText, updatePrimaryKeyWorkflowStep.getDetails());
		
		verify(legacy).updatePrimaryKeyWorkflow(anyString(), anyString(), anyString(), anyString(), anyString());
	}
	
	@Test
	public void pkUpdateDisabled() throws Exception {
		WorkflowController controllerTest = new WorkflowController(legacy, notify, userAuthService, clock());
		controllerTest.setEnablePrimaryKeyUpdate(false);
		
		try {
			controllerTest.updatePrimaryKeyWorkflow("test", "test", "test", "test", "reason", response, mockAuth);
			fail("Expected UnsupportedOperationException but got no exception.");
		} catch(UnsupportedOperationException e) {
			assertTrue(e.getMessage().contains("Feature not enabled"));
		} catch(Exception e) {
			fail("UnsupportedOperationException, but got " + e.getClass().getName());
		}
	}

	@Test
	public void validateUpdateExpiredTokenTest() throws Exception {
		doThrow(new ClientAuthorizationRequiredException("test-client")).when(userAuthService).validateToken(mockAuth);
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		try {
			controller.legacyWorkflow(file, response, mockAuth);
			fail("Expected ClientAuthorizationRequiredException but got no exception.");
		} catch(ClientAuthorizationRequiredException e) {
			assertTrue(e.getMessage().contains("test-client"));
		} catch(Exception e) {
			fail("Expected ClientAuthorizationRequiredException, but got " + e.getClass().getName());
		}
	}

	@Test
	public void validateExpiredTokenTest() throws Exception {
		doThrow(new ClientAuthorizationRequiredException("test-client")).when(userAuthService).validateToken(mockAuth);
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		try {
			controller.legacyValidationWorkflow(file, response, mockAuth);
			fail("Expected ClientAuthorizationRequiredException but got no exception.");
		} catch(ClientAuthorizationRequiredException e) {
			assertTrue(e.getMessage().contains("test-client"));
		} catch(Exception e) {
			fail("Expected ClientAuthorizationRequiredException, but got " + e.getClass().getName());
		}
	}

	@Test
	public void pkUpdateExpiredTokenTest() throws Exception {
		doThrow(new ClientAuthorizationRequiredException("test-client")).when(userAuthService).validateToken(mockAuth);
		
		try {
			controller.updatePrimaryKeyWorkflow("test", "test", "test", "test", "reason", response, mockAuth);
			fail("Expected ClientAuthorizationRequiredException but got no exception.");
		} catch(ClientAuthorizationRequiredException e) {
			assertTrue(e.getMessage().contains("test-client"));
		} catch(Exception e) {
			fail("Expected ClientAuthorizationRequiredException, but got " + e.getClass().getName());
		}
	}
}
