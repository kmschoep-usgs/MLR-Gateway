package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import javax.servlet.http.HttpServletResponse;

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

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.service.LegacyWorkflowService;

@RunWith(SpringRunner.class)
public class WorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private LegacyWorkflowService legacy;

	private WorkflowController controller;
	private MockHttpServletResponse response;

	@Before
	public void init() {
		controller = new WorkflowController(legacy);
		response = new MockHttpServletResponse();
	}

	@Test
	public void happyPathLegacyWorkflow_thenReturnData() throws Exception {
		String legacyJson = getCompareFile("testData/", "oneAdd.json");
		given(legacy.completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class))).willReturn(legacyJson);

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		String rtn = controller.legacyWorkflow(file, response);
		JSONAssert.assertEquals(legacyJson, rtn, JSONCompareMode.STRICT);
		verify(legacy).completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void badDdot_LegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badJson = "{\"error\": \"bad Ddot\"}";
		given(legacy.completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class))).willThrow(new FeignBadResponseWrapper(400, null, badJson));

		String rtn = controller.legacyWorkflow(file, response);
		JSONAssert.assertEquals(badJson, rtn, JSONCompareMode.STRICT);

		verify(legacy).completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void serverError_LegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badMsg = "{\"error\": \"bad stuff\"}";
		given(legacy.completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class))).willThrow(new HystrixBadRequestException(badMsg));

		String rtn = controller.legacyWorkflow(file, response);
		JSONAssert.assertEquals(badMsg, rtn, JSONCompareMode.STRICT);

		verify(legacy).completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void happyPathLegacyValidationWorkflow_thenReturnData() throws Exception {
		String legacyJson = getCompareFile("testData/", "oneAdd.json");
		given(legacy.ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class))).willReturn(legacyJson);

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		String rtn = controller.legacyValidationWorkflow(file, response);
		JSONAssert.assertEquals(legacyJson, rtn, JSONCompareMode.STRICT);
		verify(legacy).ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badJson = "{\"error\": \"bad Ddot\"}";
		given(legacy.ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class))).willThrow(new FeignBadResponseWrapper(400, null, badJson));

		String rtn = controller.legacyValidationWorkflow(file, response);
		JSONAssert.assertEquals(badJson, rtn, JSONCompareMode.STRICT);

		verify(legacy).ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void serverError_LegacyValidationWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badMsg = "{\"error\": \"bad stuff\"}";
		given(legacy.ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class))).willThrow(new HystrixBadRequestException(badMsg));

		String rtn = controller.legacyValidationWorkflow(file, response);
		JSONAssert.assertEquals(badMsg, rtn, JSONCompareMode.STRICT);

		verify(legacy).ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class));
	}

}
