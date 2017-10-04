package gov.usgs.wma.mlrgateway.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.client.FileExportClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import net.minidev.json.JSONObject;

@RunWith(SpringRunner.class)
public class LegacyWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private DdotService ddotService;
	@MockBean
	private LegacyCruClient legacyCruClient;
	@MockBean
	private TransformService transformService;
	@MockBean
	private LegacyValidatorClient legacyValidatorClient;
	@MockBean
	private FileExportClient fileExportClient;
	@MockBean
	private NotificationClient notificationClient;

	private LegacyWorkflowService service;
	private MockHttpServletResponse response;
	private ObjectMapper mapper;
	private String reportName = "TEST Legacy Workflow";
	private String legacyJson = "{\"transactionType\":\"A\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}";
	private String legacyValidation = "{\"validation_passed_message\": \"Validations Passed\"}";


	@Before
	public void init() {
		service = new LegacyWorkflowService(ddotService, legacyCruClient, transformService, legacyValidatorClient, fileExportClient, notificationClient);
		response = new MockHttpServletResponse();
		WorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath_transformAndValidate_thenReturnObject() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\""
				+ LegacyWorkflowService.VALIDATION_SUCCESSFULL + "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}]}";
		Map<String, Object> ml = getAdd();
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyValidation, HttpStatus.CREATED);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);

		JSONAssert.assertEquals(legacyJson.replace("}", ",\"validation\":" + legacyValidation + "}"), service.transformAndValidate(ml), JSONCompareMode.STRICT);
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
	}

	@Test
	public void noTransactionType_completeWorkflow_thenReturnBadRequest() throws Exception {
		String data = "{\"transactionType\":\"A\",\"siteNumber\":\"12345678       \",\"agencyCode\":\"USGS \",\"validation\":{\"validation_passed_message\": \"Validations Passed\"}}";
		String msg = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"" + LegacyWorkflowService.VALIDATION_SUCCESSFULL
					+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":400,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.BAD_TRANSACTION_TYPE.replace("%json%", data))
					+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		String json = "{}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUnknown();
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		Map<String, Object> ml = getAdd();
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyValidation, HttpStatus.CREATED);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);

		String rtn = service.completeWorkflow(file, response);
		JSONAssert.assertEquals(json, rtn, JSONCompareMode.STRICT);
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void oneAddTransaction_completeWorkflow_thenReturnCreated() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"" + LegacyWorkflowService.VALIDATION_SUCCESSFULL
					+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_ADD_SUCCESSFULL)
					+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleAdd();
		Map<String, Object> ml = ddotRtn.get(0);
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.CREATED);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(legacyRtn);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);

		String rtn = service.completeWorkflow(file, response);
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
		JSONAssert.assertEquals(legacyJson, rtn, JSONCompareMode.STRICT);

		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void oneUpdateTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"" + LegacyWorkflowService.VALIDATION_SUCCESSFULL
					+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_UPDATE_SUCCESSFULL)
					+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUpdate();
		Map<String, Object> ml = ddotRtn.get(0);
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.CREATED);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyCruClient.patchMonitoringLocation(anyString())).willReturn(legacyRtn);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);

		String rtn = service.completeWorkflow(file, response);
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
		JSONAssert.assertEquals(legacyJson, rtn, JSONCompareMode.STRICT);

		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void ddotValidation_callsBackingServices() throws Exception {
		String json = "{\"transactionType\":\"A\",\"siteNumber\":\"12345678       \",\"agencyCode\":\"USGS \",\"validation\":{\"validation_passed_message\": \"Validations Passed\"}}";
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\""
				+ LegacyWorkflowService.VALIDATION_SUCCESSFULL + "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}]}";
		Map<String, Object> ml = getAdd();
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyValidation, HttpStatus.CREATED);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());

		assertEquals(json, service.ddotValidation(file, response));
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

}
