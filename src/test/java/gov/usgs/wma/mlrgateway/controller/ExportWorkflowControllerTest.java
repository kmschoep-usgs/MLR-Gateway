package gov.usgs.wma.mlrgateway.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.test.context.junit4.SpringRunner;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.workflow.ExportWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import static org.mockito.ArgumentMatchers.anyList;

@RunWith(SpringRunner.class)
public class ExportWorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private ExportWorkflowService export;
	@MockBean
	private NotificationService notificationService;
	@MockBean
	private OAuth2Authentication authentication;
	@MockBean
	private OAuth2Request mockOAuth2Request;

	@Bean
	@Primary
	public Clock clock() {
		return Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC"));
	}
	
	private ExportWorkflowController controller;
	private MockHttpServletResponse response;
	private String userName = "userName";
	private String reportDate = "01/01/2019";
	private Map<String, Serializable> testEmail;
	public static final String NOTIFICATION_SUCCESSFULL = "Notification sent successfully.";

	@Before
	public void init() {
		controller = new ExportWorkflowController(export, notificationService, clock());
		response = new MockHttpServletResponse();
		testEmail = new HashMap<>();
		testEmail.put("email", "localuser@example.gov");
		ExportWorkflowController.setReport(new GatewayReport(ExportWorkflowController.COMPLETE_WORKFLOW, null, userName, reportDate));
	}

	@Test
	public void happyPath_ExportWorkflow() throws Exception {
		when(authentication.getOAuth2Request()).thenReturn(mockOAuth2Request);
		when(mockOAuth2Request.getExtensions()).thenReturn(testEmail); 
		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response, authentication);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> ExportWorkflowController.COMPLETE_WORKFLOW.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "200");
		assertTrue(completeWorkflowStep.isSuccess());
		assertEquals(rtn.getName(), ExportWorkflowController.COMPLETE_WORKFLOW);
		
		verify(export).exportWorkflow(anyString(), anyString());
		verify(notificationService).sendNotification(anyList(), anyString(), eq(null), anyString(), any());
	}

	@Test
	public void badBackingServiceRequest_ExportWorkflow() throws Exception {
		String badText = "This is really bad.";
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(export).exportWorkflow(anyString(), anyString());

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response, authentication);
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

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response, authentication);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> ExportWorkflowController.COMPLETE_WORKFLOW.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "500");
		assertEquals(rtn.getName(), ExportWorkflowController.COMPLETE_WORKFLOW);
		assertEquals(completeWorkflowStep.getDetails(), badText);

		verify(export).exportWorkflow(anyString(), anyString());
	}

}
