package gov.usgs.wma.mlrgateway.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
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

@RunWith(SpringRunner.class)
public class DdotServiceTest extends BaseSpringTest {

	@MockBean
	private DdotClient ddotClient;

	private DdotService service;
	private String reportName = "TEST DDOT";
	private String user = "testUser";
	public static final LocalDate REPORT_DATE = LocalDate.of(2018, 03, 16);
	private String fileName = "test.d";

	@Before
	public void init() {
		service = new DdotService(ddotClient);
		WorkflowController.setReport(new GatewayReport(reportName, user, REPORT_DATE, fileName));
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
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		assertEquals(rtn.getSteps().get(0).getDetails(), DdotService.INTERNAL_ERROR_MESSAGE);
		assertEquals(rtn.getSteps().get(0).getName(), DdotService.STEP_NAME);
		assertEquals(rtn.getName(), reportName);
		assertEquals(rtn.getStatus().toString(), "500");
		assertEquals(rtn.getSteps().get(0).getStatus().toString(), "500");
	}

	@Test
	public void happyPath() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		ObjectMapper mapper = new ObjectMapper();
		String ddotResponse = mapper.writeValueAsString(singleAdd());
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn(ddotResponse);
		List<Map<String, Object>> rtn = service.parseDdot(file);
		GatewayReport gatewayReport = WorkflowController.getReport();
		assertEquals(1, rtn.size());
		assertThat(rtn.get(0), is(getAdd()));
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		assertEquals(gatewayReport.getStatus().toString(), "200");
		assertEquals(gatewayReport.getInputFileName(), fileName);
		assertEquals(gatewayReport.getSteps().get(0).getDetails(), DdotService.SUCCESS_MESSAGE);
		assertEquals(gatewayReport.getSteps().get(0).getName(), DdotService.STEP_NAME);
		assertEquals(gatewayReport.getSteps().get(0).getStatus().toString(),"200");
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
