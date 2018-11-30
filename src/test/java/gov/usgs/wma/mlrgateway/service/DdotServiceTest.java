package gov.usgs.wma.mlrgateway.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;

@RunWith(SpringRunner.class)
public class DdotServiceTest extends BaseSpringTest {

	@MockBean
	private DdotClient ddotClient;

	private DdotService service;
	private String reportName = "TEST DDOT";
	private String fileName = "test.d";

	@Before
	public void init() {
		service = new DdotService(ddotClient);
		WorkflowController.setReport(new GatewayReport(reportName, fileName));
	}

	@Test
	public void garbageFromDdot_thenReturnInternalServerError() throws Exception {

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
		GatewayReport rtn = WorkflowController.getReport();
		StepReport ddotStep = rtn.getWorkflowSteps().stream()
				.filter(s -> DdotService.STEP_NAME.equals(s.getName()))
				.findAny().orElse(null);
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		assertEquals(ddotStep.getDetails(), DdotService.INTERNAL_ERROR_MESSAGE);
		assertEquals(ddotStep.getName(), DdotService.STEP_NAME);
		assertEquals(rtn.getName(), reportName);
		assertEquals(ddotStep.getHttpStatus().toString(), "500");
		assertFalse(ddotStep.isSuccess());
	}
	
	@Test
	public void nullFromDdot_thenReturnInternalServerError() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willThrow(new RuntimeException());
		try {
			service.parseDdot(file);
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals(DdotService.INTERNAL_ERROR_MESSAGE , ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
		GatewayReport rtn = WorkflowController.getReport();
		StepReport ddotStep = rtn.getWorkflowSteps().stream()
				.filter(s -> DdotService.STEP_NAME.equals(s.getName()))
				.findAny().orElse(null);
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		assertEquals(ddotStep.getDetails(), DdotService.INTERNAL_ERROR_MESSAGE);
		assertEquals(ddotStep.getName(), DdotService.STEP_NAME);
		assertEquals(rtn.getName(), reportName);
		assertEquals(ddotStep.getHttpStatus().toString(), "500");
		assertFalse(ddotStep.isSuccess());
	}

	@Test
	public void happyPath() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		ObjectMapper mapper = new ObjectMapper();
		String ddotResponse = mapper.writeValueAsString(singleAdd());
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn(ddotResponse);
		List<Map<String, Object>> rtn = service.parseDdot(file);
		GatewayReport gatewayReport = WorkflowController.getReport();
		StepReport ddotStep = gatewayReport.getWorkflowSteps().stream()
				.filter(s -> DdotService.STEP_NAME.equals(s.getName()))
				.findAny().orElse(null);
		
		assertEquals(1, rtn.size());
		assertThat(rtn.get(0), is(getAdd()));
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		assertEquals(ddotStep.getHttpStatus().toString(), "200");
		assertTrue(ddotStep.isSuccess());
		assertEquals(gatewayReport.getInputFileName(), fileName);
		assertEquals(ddotStep.getDetails(), DdotService.SUCCESS_MESSAGE);
		assertEquals(ddotStep.getName(), DdotService.STEP_NAME);
		
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

	public static List<Map<String, Object>> multipleWithErrors() {
		List<Map<String, Object>> lm = new ArrayList<>();
		lm.add(getUnknown());
		lm.add(getAdd());
		return lm;
	}

}
