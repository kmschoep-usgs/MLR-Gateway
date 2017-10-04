package gov.usgs.wma.mlrgateway.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import net.minidev.json.JSONObject;

@RunWith(SpringRunner.class)
public class DdotServiceTest extends BaseSpringTest {

	@MockBean
	private DdotClient ddotClient;

	private DdotService service;
	private String reportName = "TEST DDOT";
	private ObjectMapper mapper;

	@Before
	public void init() {
		service = new DdotService(ddotClient);
		WorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void garbageFromDdot_thenReturnInternalServerError() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":[{\"name\":\"" + DdotService.STEP_NAME + "\",\"status\":500,\"details\":\""
				+ JSONObject.escape(DdotService.INTERNAL_ERROR_MESSAGE) + "\",\"agencyCode\":null,\"siteNumber\":null}]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn("not json");
		try {
			service.parseDdot(file);
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals(DdotService.INTERNAL_ERROR_MESSAGE , ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
	}

	@Test
	public void incorrectDdotSize_thenReturnBadRequest() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":[{\"name\":\"" + DdotService.STEP_NAME + "\",\"status\":400,\"details\":\""
				+ JSONObject.escape(DdotService.WRONG_FORMAT_MESSAGE.replace("%ddotResponse%", "[{},{}]")) + "\",\"agencyCode\":null,\"siteNumber\":null}]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String ddotResponse = "[{},{}]";
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn(ddotResponse);
		try {
			service.parseDdot(file);
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.BAD_REQUEST.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals(DdotService.WRONG_FORMAT_MESSAGE.replace("%ddotResponse%", "[{},{}]"),
						((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
	}


	@Test
	public void happyPath() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":[{\"name\":\"" + DdotService.STEP_NAME + "\",\"status\":200,\"details\":\""
				+ DdotService.SUCCESS_MESSAGE + "\",\"agencyCode\":null,\"siteNumber\":null}]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		ObjectMapper mapper = new ObjectMapper();
		String ddotResponse = mapper.writeValueAsString(singleAdd());
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn(ddotResponse);
		List<Map<String, Object>> rtn = service.parseDdot(file);
		assertEquals(1, rtn.size());
		assertThat(rtn.get(0), is(getAdd()));
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
	}

	public static List<Map<String, Object>> singleUnknown() {
		List<Map<String, Object>> lm = new ArrayList<>();
		lm.add(getUnknown());
		return lm;
	}

	public static List<Map<String, Object>> singleAdd() {
		List<Map<String, Object>> lm = new ArrayList<>();
		lm.add(getAdd());
		return lm;
	}

	public static List<Map<String, Object>> singleUpdate() {
		List<Map<String, Object>> lm = new ArrayList<>();
		lm.add(getUpdate());
		return lm;
	}

}
