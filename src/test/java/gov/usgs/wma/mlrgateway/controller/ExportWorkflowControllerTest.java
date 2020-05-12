package gov.usgs.wma.mlrgateway.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.workflow.ExportWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.util.UserAuthUtil;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
public class ExportWorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private ExportWorkflowService export;
	@MockBean
	private NotificationService notificationService;
	@MockBean
	private UserAuthUtil userAuthUtil;

	@Bean
	@Primary
	public Clock clock() {
		return Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC"));
	}

	private ExportWorkflowController controller;
	private MockHttpServletResponse response;
	private String userName = "user";
	private String reportDate = "01/01/2019";
	private Map<String, Serializable> testEmail;
	public static final String NOTIFICATION_SUCCESSFULL = "Notification sent successfully.";
	private UsernamePasswordAuthenticationToken mockAuth = new UsernamePasswordAuthenticationToken(userName, "pass");

	@BeforeEach
	public void init() {
		given(userAuthUtil.getUserEmail(any(Authentication.class))).willReturn("test@test");
		given(userAuthUtil.getUserName(any(Authentication.class))).willReturn("test");
		controller = new ExportWorkflowController(export, notificationService, userAuthUtil, clock());
		response = new MockHttpServletResponse();
		testEmail = new HashMap<>();
		testEmail.put("email", "localuser@example.gov");
		ExportWorkflowController.setReport(new GatewayReport(ExportWorkflowController.COMPLETE_WORKFLOW, null, userName, reportDate));
	}

	@Test
	public void happyPath_ExportWorkflow() throws Exception {
		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response, mockAuth);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> ExportWorkflowController.COMPLETE_WORKFLOW.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "200");
		assertTrue(completeWorkflowStep.isSuccess());
		assertEquals(rtn.getName(), ExportWorkflowController.COMPLETE_WORKFLOW);
		
		verify(export).exportWorkflow(anyString(), anyString());
		verify(notificationService).sendNotification(anyString(), anyList(), anyString(), anyString(), anyString(), any());
	}

	@Test
	public void badBackingServiceRequest_ExportWorkflow() throws Exception {
		String badText = "This is really bad.";
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(export).exportWorkflow(anyString(), anyString());

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response, mockAuth);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> ExportWorkflowController.COMPLETE_WORKFLOW.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(rtn.getName(), ExportWorkflowController.COMPLETE_WORKFLOW);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "400");
		assertEquals(completeWorkflowStep.getDetails(), badText);

		verify(export).exportWorkflow(anyString(), anyString());
	}

	@Test
	public void serverError_ExportWorkflow() throws Exception {
		String badText = "This is really bad.";

		willThrow(new HystrixBadRequestException(badText)).given(export).exportWorkflow(anyString(), anyString());

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response, mockAuth);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> ExportWorkflowController.COMPLETE_WORKFLOW.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "500");
		assertEquals(rtn.getName(), ExportWorkflowController.COMPLETE_WORKFLOW);
		assertEquals(completeWorkflowStep.getDetails(), badText);

		verify(export).exportWorkflow(anyString(), anyString());
	}

}