package gov.usgs.wma.mlrgateway.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;


@RunWith(SpringRunner.class)
public class WorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private NotificationService notify;
	@MockBean
	private LegacyWorkflowService legacy;
	@MockBean
	private Authentication authentication;
	
	@Bean
	@Primary
	public Clock clock() {
		return Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC"));
	}

	private WorkflowController controller;
	private MockHttpServletResponse response;

	@Before
	public void init() {
		controller = new WorkflowController(legacy, notify, clock(), authentication);
		response = new MockHttpServletResponse();
	}

	@Test
	public void happyPath_LegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		UserSummaryReport rtn = controller.legacyWorkflow(file, response);
		assertEquals(rtn.getName(), LegacyWorkflowService.COMPLETE_WORKFLOW );
		assertEquals(rtn.getWorkflowSteps(), new ArrayList<>());
		assertEquals(rtn.getSites(), new ArrayList<>());
		assertEquals(rtn.getInputFileName(), "d.");
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void badBackingServiceRequest_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).completeWorkflow(any(MultipartFile.class));

		UserSummaryReport rtn = controller.legacyWorkflow(file, response);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(rtn.getName(), LegacyWorkflowService.COMPLETE_WORKFLOW);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "400");
		assertEquals(completeWorkflowStep.getName(), LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED);
		assertEquals(completeWorkflowStep.getDetails(), badText);
		assertEquals(rtn.getInputFileName(), "d.");
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(legacy).completeWorkflow(any(MultipartFile.class));
		UserSummaryReport rtn = controller.legacyWorkflow(file, response);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(rtn.getName(), LegacyWorkflowService.COMPLETE_WORKFLOW);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "500");
		assertEquals(completeWorkflowStep.getName(), LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED);
		assertEquals(completeWorkflowStep.getDetails(), badText);
		assertEquals(rtn.getInputFileName(), "d.");
		
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void happyPath_LegacyValidationWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		UserSummaryReport userSummaryReport = controller.legacyValidationWorkflow(file, response);	
		assertEquals(userSummaryReport.getName(), LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW);
		assertEquals(userSummaryReport.getWorkflowSteps(), new ArrayList<>());
		assertEquals(userSummaryReport.getSites(), new ArrayList<>());
		assertEquals(userSummaryReport.getInputFileName(), "d.");
		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		UserSummaryReport rtn = controller.legacyValidationWorkflow(file, response);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(rtn.getName(), LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "400");
		assertEquals(completeWorkflowStep.getDetails(), badText);
		assertEquals(completeWorkflowStep.getName(),LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED);

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		UserSummaryReport rtn = controller.legacyValidationWorkflow(file, response);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(rtn.getName(), LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "500");
		assertEquals(completeWorkflowStep.getDetails(), badText);
		assertEquals(completeWorkflowStep.getName(),LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED);

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

}
