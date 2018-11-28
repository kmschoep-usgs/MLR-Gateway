package gov.usgs.wma.mlrgateway.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
public class WorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private NotificationService notify;
	@MockBean
	private LegacyWorkflowService legacy;

	private WorkflowController controller;
	private MockHttpServletResponse response;

	@Before
	public void init() {
		controller = new WorkflowController(legacy, notify);
		response = new MockHttpServletResponse();
	}

	@Test
	public void happyPath_LegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		GatewayReport rtn = controller.legacyWorkflow(file, response);
		assertEquals(rtn.getName(), LegacyWorkflowService.COMPLETE_WORKFLOW );
		assertEquals(rtn.getWorkflowStep().getHttpStatus().toString(), "200");
		assertEquals(rtn.getInputFileName(), "file");
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void badBackingServiceRequest_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).completeWorkflow(any(MultipartFile.class));

		GatewayReport rtn = controller.legacyWorkflow(file, response);
		assertEquals(rtn.getName(), LegacyWorkflowService.COMPLETE_WORKFLOW);
		assertEquals(rtn.getWorkflowStep().getHttpStatus().toString(), "400");
		assertEquals(rtn.getWorkflowStep().getName(), LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED);
		assertEquals(rtn.getWorkflowStep().getHttpStatus().toString(), "400");
		assertEquals(rtn.getWorkflowStep().getDetails(), badText);
		assertEquals(rtn.getInputFileName(), "file");
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(legacy).completeWorkflow(any(MultipartFile.class));
		GatewayReport rtn = controller.legacyWorkflow(file, response);
		assertEquals(rtn.getName(), LegacyWorkflowService.COMPLETE_WORKFLOW);
		assertEquals(rtn.getWorkflowStep().getHttpStatus().toString(), "500");
		assertEquals(rtn.getWorkflowStep().getName(), LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED);
		assertEquals(rtn.getWorkflowStep().getDetails(), badText);
		assertEquals(rtn.getInputFileName(), "file");
		
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void happyPath_LegacyValidationWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		GatewayReport rtn = controller.legacyValidationWorkflow(file, response);
		assertEquals(rtn.getName(), LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW);
		assertEquals(rtn.getWorkflowStep().getHttpStatus().toString(), "200");
		assertEquals(rtn.getInputFileName(), "d.");
		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		GatewayReport rtn = controller.legacyValidationWorkflow(file, response);
		assertEquals(rtn.getName(), LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW);
		assertEquals(rtn.getWorkflowStep().getHttpStatus().toString(), "400");
		assertEquals(rtn.getWorkflowStep().getDetails(), badText);
		assertEquals(rtn.getWorkflowStep().getName(),LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED);

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		GatewayReport rtn = controller.legacyValidationWorkflow(file, response);
		assertEquals(rtn.getName(), LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW);
		assertEquals(rtn.getWorkflowStep().getHttpStatus().toString(), "500");
		assertEquals(rtn.getWorkflowStep().getDetails(), badText);
		assertEquals(rtn.getWorkflowStep().getName(),LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED);

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

}
