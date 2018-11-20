package gov.usgs.wma.mlrgateway.workflow;

import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
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
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.DdotService;
import gov.usgs.wma.mlrgateway.service.DdotServiceTest;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.service.LegacyValidatorService;
import gov.usgs.wma.mlrgateway.service.LegacyTransformerService;
import java.util.HashMap;
import net.minidev.json.JSONObject;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;

@RunWith(SpringRunner.class)
public class LegacyWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private DdotService ddotService;
	@MockBean
	private LegacyTransformerService transformService;
	@MockBean
	private LegacyValidatorService legacyValidatorService;
	@MockBean
	private LegacyCruService legacyCruService;
	@MockBean
	private FileExportService fileExportService;

	private LegacyWorkflowService service;
	private MockHttpServletResponse response;
	private ObjectMapper mapper;
	private String reportName = "TEST Legacy Workflow";
	private String legacyJson = "{\"transactionType\":\"A\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}";

	@Before
	public void init() {
		service = new LegacyWorkflowService(ddotService, legacyCruService, transformService, legacyValidatorService, fileExportService);
		response = new MockHttpServletResponse();
		WorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
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
		verify(transformService, never()).transformStationIx(anyMap());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean());
		verify(transformService, never()).transformGeo(anyMap());
		verify(legacyCruService, never()).addTransaction(anyString(), anyString(), anyString());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString());
		verify(fileExportService, never()).exportUpdate(anyString(), anyString(), anyString());
	}

	@Test
	public void noTransactionType_completeWorkflow_noStopOnError() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":["
				+ "{\"name\":\"" + LegacyValidatorService.VALIDATION_STEP + "\",\"status\":400,\"details\":\"" + LegacyWorkflowService.BAD_TRANSACTION_TYPE
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (1/2)\",\"status\":400,\"details\":\"" + "{\\\"error_message\\\": \\\"Validation failed due to a missing transaction type.\\\"}"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (2/2)\",\"status\":201,\"details\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP_SUCCESS
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.multipleWithErrors();
		Map<String, Object> ml = getAdd();

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(getAddValid());
		given(transformService.transformGeo(anyMap())).willReturn(getAddValid());
		ResponseEntity<String> addRtn = new ResponseEntity<>(legacyJson, HttpStatus.CREATED);

		service.completeWorkflow(file);

		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
		verify(transformService).transformGeo(anyMap());
		verify(legacyCruService).addTransaction(anyString(), anyString(), anyString());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString());
		verify(fileExportService).exportAdd(anyString(), anyString(), anyString());
		verify(fileExportService, never()).exportUpdate(anyString(), anyString(), anyString());
	}

	@Test
	public void oneAddTransaction_completeWorkflow_thenReturnCreated() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (1/1)\",\"status\":201,\"details\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP_SUCCESS
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleAdd();
		Map<String, Object> ml = ddotRtn.get(0);
		Map<String, Object> mlValid = new HashMap<>(ml);
		String emptyRecord = new String();
		Map<String, Object> emptySite = new HashMap<>();
		mlValid.put("validation",legacyValidation);

		
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlValid);
		given(transformService.transformGeo(anyMap())).willReturn(mlValid);
		
		service.completeWorkflow(file);
		
		String mapperRet = mapper.writeValueAsString(WorkflowController.getReport());

		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
		verify(transformService).transformGeo(anyMap());
		verify(legacyCruService).addTransaction(anyString(), anyString(), anyString());
		verify(fileExportService).exportAdd(anyString(), anyString(), anyString());
	}

	@Test
	public void oneUpdateTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP + " (1/1)\",\"status\":200,\"details\":\"" + LegacyWorkflowService.COMPLETE_TRANSACTION_STEP_SUCCESS
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUpdate();
		Map<String, Object> ml = ddotRtn.get(0);
		Map<String, Object> mlValid = new HashMap<>(ml);
		mlValid.put("validation",legacyValidation);

		
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlValid);
		given(transformService.transformGeo(anyMap())).willReturn(mlValid);

		service.completeWorkflow(file);

		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
		verify(transformService).transformGeo(anyMap());
		verify(legacyCruService).updateTransaction(anyString(), anyString(), anyString());
		verify(fileExportService).exportUpdate(anyString(), anyString(), anyString());
	}

	@Test
	public void ddotAddValidation_callsCorrectBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP + " (1/1)\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP_SUCCESS)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		Map<String, Object> ml = getAdd();
		Map<String, Object> mlValid = getAddValid();
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		given(transformService.transformStationIx(anyMap())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlValid);
		
		service.ddotValidation(file);
		String actual = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actual, JSONCompareMode.STRICT);

		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), eq(true));
		verify(legacyValidatorService, never()).doValidation(anyMap(), eq(false));
	}

	@Test
	public void ddotUpdateValidation_callsCorrectBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP + " (1/1)\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP_SUCCESS)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		Map<String, Object> ml = getUpdate();
		Map<String, Object> mlValid = getUpdateValid();
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleUpdate());
		given(transformService.transformStationIx(anyMap())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willReturn(mlValid);
		
		service.ddotValidation(file);
		String actual = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actual, JSONCompareMode.STRICT);

		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap());
		verify(legacyValidatorService).doValidation(anyMap(), eq(false));
		verify(legacyValidatorService, never()).doValidation(anyMap(), eq(true));
	}

	@Test
	public void ddotValidation_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP + " (1/1)\",\"status\":500,\"details\":\"" +"{\\\"error_message\\\": \\\"null\\\"}"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		Map<String, Object> mlValid = getAddValid();

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		given(transformService.transformStationIx(getAdd())).willReturn(getAdd());
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean())).willThrow(new RuntimeException());

		service.ddotValidation(file);

		assertEquals(HttpStatus.OK.value(), response.getStatus());
		String actual = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actual, JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean());
	}
}