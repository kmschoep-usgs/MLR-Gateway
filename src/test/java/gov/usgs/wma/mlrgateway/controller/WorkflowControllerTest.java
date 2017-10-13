package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.service.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class WorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private NotificationService notify;
	@MockBean
	private LegacyWorkflowService legacy;

	private WorkflowController controller;
	private MockHttpServletResponse response;
	private ObjectMapper mapper;

	@Before
	public void init() {
		controller = new WorkflowController(legacy, notify);
		response = new MockHttpServletResponse();
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath_LegacyWorkflow() throws Exception {
		String json = "{\"name\":\"" + WorkflowController.COMPLETE_WORKFLOW + "\",\"status\":200,\"steps\":[]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		GatewayReport rtn = controller.legacyWorkflow(file, response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void badBackingServiceRequest_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		String json = "{\"name\":\"" + WorkflowController.COMPLETE_WORKFLOW + "\",\"status\":400,\"steps\":[{\"name\":\"" + WorkflowController.COMPLETE_WORKFLOW
				+ "\",\"status\":400,\"details\":\"" + badText + "\"}]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).completeWorkflow(any(MultipartFile.class));

		GatewayReport rtn = controller.legacyWorkflow(file, response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		String json = "{\"name\":\"" + WorkflowController.COMPLETE_WORKFLOW + "\",\"status\":500,\"steps\":[{\"name\":\""
				+ WorkflowController.COMPLETE_WORKFLOW + "\",\"status\":500,\"details\":\"" + badText + "\"}]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(legacy).completeWorkflow(any(MultipartFile.class));

		GatewayReport rtn = controller.legacyWorkflow(file, response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);

		verify(legacy).completeWorkflow(any(MultipartFile.class));
	}

	@Test
	public void happyPath_LegacyValidationWorkflow() throws Exception {
		String json = "{\"name\":\"" + WorkflowController.VALIDATE_DDOT_WORKFLOW + "\",\"status\":200,\"steps\":[]}";

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		GatewayReport rtn = controller.legacyValidationWorkflow(file, response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";
		String json = "{\"name\":\"" + WorkflowController.VALIDATE_DDOT_WORKFLOW + "\",\"status\":400,\"steps\":[{\"name\":\"" + WorkflowController.VALIDATE_DDOT_WORKFLOW
				+ "\",\"status\":400,\"details\":\"" + badText + "\"}]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		GatewayReport rtn = controller.legacyValidationWorkflow(file, response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

	@Test
	public void serverError_LegacyValidationWorkflow() throws Exception {
		String badText = "This is really bad.";
		String json = "{\"name\":\"" + WorkflowController.VALIDATE_DDOT_WORKFLOW + "\",\"status\":500,\"steps\":[{\"name\":\"" + WorkflowController.VALIDATE_DDOT_WORKFLOW
				+ "\",\"status\":500,\"details\":\"" + badText + "\"}]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(legacy).ddotValidation(any(MultipartFile.class));

		GatewayReport rtn = controller.legacyValidationWorkflow(file, response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);

		verify(legacy).ddotValidation(any(MultipartFile.class));
	}

}
