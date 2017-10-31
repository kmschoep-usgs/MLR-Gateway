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
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import net.minidev.json.JSONObject;
import static org.mockito.Matchers.anyBoolean;


@RunWith(SpringRunner.class)
public class LegacyWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private DdotService ddotService;
	@MockBean
	private LegacyCruClient legacyCruClient;
	@MockBean
	private TransformService transformService;
	@MockBean
	private LegacyValidatorService legacyValidatorService;
	@MockBean
	private FileExportClient fileExportClient;

	private LegacyWorkflowService service;
	private MockHttpServletResponse response;
	private ObjectMapper mapper;
	private String reportName = "TEST Legacy Workflow";
	private String legacyJson = "{\"transactionType\":\"A\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}";

	@Before
	public void init() {
		service = new LegacyWorkflowService(ddotService, legacyCruClient, transformService, legacyValidatorService, fileExportClient);
		response = new MockHttpServletResponse();
		WorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath_transformAndValidate_thenReturnObject() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> mlRtn = getAddValid();
		given(transformService.transform(anyMap())).willReturn(mlRtn);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlRtn);
		String serviceJson =  service.validateAndTransform(ml, true);
		JSONAssert.assertEquals(legacyJson.replace("}", ",\"validation\":" + legacyValidation + "}"), serviceJson, JSONCompareMode.STRICT);
		verify(transformService).transform(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
	}

	@Test
	public void noTransactionType_completeWorkflow_thenReturnBadRequest() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":400,\"details\":\"" + LegacyWorkflowService.BAD_TRANSACTION_TYPE
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (1/1)\",\"status\":400,\"details\": \""+ "{\\\"error_message\\\": \\\"Validation failed due to a missing transaction type.\\\"}"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUnknown();
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);


		service.completeWorkflow(file);
		System.out.println("\n\n" + mapper.writeValueAsString(WorkflowController.getReport()) + "\n\n");
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void noTransactionType_completeWorkflow_noStopOnError() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":400,\"details\":\"" + LegacyWorkflowService.BAD_TRANSACTION_TYPE
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (1/2)\",\"status\":400,\"details\":\"" + "{\\\"error_message\\\": \\\"Validation failed due to a missing transaction type.\\\"}"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":201,\"details\":\"" + LegacyWorkflowService.SITE_ADD_SUCCESSFULL
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + LegacyWorkflowService.EXPORT_SUCCESSFULL
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (2/2)\",\"status\":200,\"details\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP_SUCCESS
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.multipleWithErrors();
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		Map<String, Object> ml = getAdd();
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyValidation, HttpStatus.OK);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(getAddValid());
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.CREATED);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(addRtn);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);

		service.completeWorkflow(file);
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void oneAddTransaction_completeWorkflow_thenReturnCreated() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":201,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_ADD_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (1/1)\",\"status\":200,\"details\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP_SUCCESS
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleAdd();
		Map<String, Object> ml = ddotRtn.get(0);
		Map<String, Object> mlValid = ml;
		mlValid.put("validation",legacyValidation);
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.CREATED);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(addRtn);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlValid);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);

		service.completeWorkflow(file);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void oneUpdateTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_UPDATE_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (1/1)\",\"status\":200,\"details\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP_SUCCESS
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUpdate();
		Map<String, Object> ml = ddotRtn.get(0);
		Map<String, Object> mlValid = ml;
		mlValid.put("validation",legacyValidation);
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyCruClient.patchMonitoringLocation(anyString())).willReturn(legacyRtn);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlValid);
		given(fileExportClient.exportUpdate(anyString())).willReturn(legacyRtn);

		service.completeWorkflow(file);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
	}

	@Test
	public void ddotValidation_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP + " (1/1)\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP_SUCCESS)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		Map<String, Object> ml = getAdd();
		Map<String, Object> mlValid = getAddValid();
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		
		given(transformService.transform(anyMap())).willReturn(mlValid);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlValid);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		
		service.ddotValidation(file);
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
	}

	@Test
	public void ddotValidation_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP + " (1/1)\",\"status\":500,\"details\":\"" +"{\\\"error_message\\\": \\\"null\\\"}"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		Map<String, Object> mlValid = getAddValid();
		given(transformService.transform(anyMap())).willThrow(new RuntimeException());
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlValid);

		service.ddotValidation(file);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}
	
	@Test
	public void addTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":201,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_ADD_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.CREATED);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(addRtn);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);

		service.addTransaction("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void addTransaction_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":500,\"details\":\"" + "Site add failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(legacyCruClient.createMonitoringLocation(anyString())).willThrow(new RuntimeException());

		service.addTransaction("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void addExport_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);

		service.exportAdd("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void addExport_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":500,\"details\":\"" + "Export add failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(fileExportClient.exportAdd(anyString())).willThrow(new RuntimeException());

		service.exportAdd("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void updateTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_UPDATE_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.patchMonitoringLocation(anyString())).willReturn(legacyRtn);
		given(fileExportClient.exportUpdate(anyString())).willReturn(legacyRtn);

		service.updateTransaction("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
	}

	@Test
	public void updateTransaction_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_UPDATE_STEP + "\",\"status\":500,\"details\":\"" + "Site update failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(legacyCruClient.patchMonitoringLocation(anyString())).willThrow(new RuntimeException());

		service.updateTransaction("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void updateExport_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<>(legacyJson, HttpStatus.OK);
		given(fileExportClient.exportUpdate(anyString())).willReturn(legacyRtn);

		service.exportUpdate("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
	}

	@Test
	public void updateExport_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_UPDATE_STEP + "\",\"status\":500,\"details\":\"" + "Export update failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(fileExportClient.exportUpdate(anyString())).willThrow(new RuntimeException());

		service.exportUpdate("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
	}
}