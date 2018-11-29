package gov.usgs.wma.mlrgateway.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.workflow.ExportWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import static org.mockito.Matchers.anyList;

@RunWith(SpringRunner.class)
public class ExportWorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private ExportWorkflowService export;
	@MockBean
	private NotificationService notificationService;
	
	private ExportWorkflowController controller;
	private MockHttpServletResponse response;
	public static final LocalDate REPORT_DATE = LocalDate.of(2018, 03, 16);
	public static final String NOTIFICATION_SUCCESSFULL = "Notification sent successfully.";

	@Before
	public void init() {
		controller = new ExportWorkflowController(export, notificationService);
		response = new MockHttpServletResponse();
		ExportWorkflowController.setReport(new GatewayReport(ExportWorkflowController.COMPLETE_WORKFLOW, null));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void happyPath_ExportWorkflow() throws Exception {
		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> ExportWorkflowController.COMPLETE_WORKFLOW.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "200");
		assertTrue(completeWorkflowStep.isSuccess());
		assertEquals(rtn.getName(), ExportWorkflowController.COMPLETE_WORKFLOW);
		
		verify(export).exportWorkflow(anyString(), anyString());
		verify(notificationService).sendNotification(anyList(), anyString(), anyString(), anyObject());
	}

	@Test
	public void badBackingServiceRequest_ExportWorkflow() throws Exception {
		String badText = "This is really bad.";
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(export).exportWorkflow(anyString(), anyString());

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response);
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

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response);
		StepReport completeWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> ExportWorkflowController.COMPLETE_WORKFLOW.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(completeWorkflowStep.getHttpStatus().toString(), "500");
		assertEquals(rtn.getName(), ExportWorkflowController.COMPLETE_WORKFLOW);
		assertEquals(completeWorkflowStep.getDetails(), badText);

		verify(export).exportWorkflow(anyString(), anyString());
	}

}
