package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.service.ExportWorkflowService;

@RunWith(SpringRunner.class)
public class ExportWorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private ExportWorkflowService export;

	private ExportWorkflowController controller;
	private MockHttpServletResponse response;
	private ObjectMapper mapper;

	@Before
	public void init() {
		controller = new ExportWorkflowController(export);
		response = new MockHttpServletResponse();
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath_ExportWorkflow() throws Exception {
		String json = "{\"name\":\"" + ExportWorkflowController.COMPLETE_WORKFLOW + "\",\"status\":200,\"steps\":[]}";

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		verify(export).completeWorkflow(anyString(), anyString());
	}

	@Test
	public void badBackingServiceRequest_ExportWorkflow() throws Exception {
		String badText = "This is really bad.";
		String json = "{\"name\":\"" + ExportWorkflowController.COMPLETE_WORKFLOW + "\",\"status\":400,\"steps\":[{\"name\":\"" + ExportWorkflowController.COMPLETE_WORKFLOW
				+ "\",\"status\":400,\"details\":\"" + badText + "\"}]}";
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(export).completeWorkflow(anyString(), anyString());

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);

		verify(export).completeWorkflow(anyString(), anyString());
	}

	@Test
	public void serverError_ExportWorkflow() throws Exception {
		String badText = "This is really bad.";
		String json = "{\"name\":\"" + ExportWorkflowController.COMPLETE_WORKFLOW + "\",\"status\":500,\"steps\":[{\"name\":\""
				+ ExportWorkflowController.COMPLETE_WORKFLOW + "\",\"status\":500,\"details\":\"" + badText + "\"}]}";
		willThrow(new HystrixBadRequestException(badText)).given(export).completeWorkflow(anyString(), anyString());

		GatewayReport rtn = controller.exportWorkflow("USGS", "12345678", response);
		JSONAssert.assertEquals(json, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);

		verify(export).completeWorkflow(anyString(), anyString());
	}

}
