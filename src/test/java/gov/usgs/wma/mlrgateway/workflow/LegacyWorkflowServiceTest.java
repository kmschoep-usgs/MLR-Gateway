package gov.usgs.wma.mlrgateway.workflow;

import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.DdotService;
import gov.usgs.wma.mlrgateway.service.DdotServiceTest;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.service.LegacyValidatorService;
import gov.usgs.wma.mlrgateway.service.LegacyTransformerService;
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
	private String reportName = "TEST Legacy Workflow";
	private String legacyJson = "{\"transactionType\":\"A\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}";

	@Before
	public void init() {
		service = new LegacyWorkflowService(ddotService, legacyCruService, transformService, legacyValidatorService, fileExportService);
		response = new MockHttpServletResponse();
		WorkflowController.setReport(new GatewayReport(reportName, fileName));
		mapper = new ObjectMapper();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void noTransactionType_completeWorkflow_thenReturnBadRequest() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUnknown();
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);

		service.completeWorkflow(file);

		GatewayReport rtn = WorkflowController.getReport();
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transformStationIx(anyMap(), anyObject());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean(), anyObject());
		verify(transformService, never()).transformGeo(anyMap(), anyObject());
		verify(legacyCruService, never()).addTransaction(anyString(), anyString(), anyString(), anyObject());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString(), anyObject());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), anyObject());
		verify(fileExportService, never()).exportUpdate(anyString(), anyString(), anyString(), anyObject());
		assertEquals(rtn.getSites().get(0).getIsSuccess(), false);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getIsSuccess(), false);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), LegacyWorkflowService.COMPLETE_TRANSACTION_STEP);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void noTransactionType_completeWorkflow_noStopOnError() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.multipleWithErrors();
		Map<String, Object> ml = getAdd();

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap(), anyObject())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), anyObject())).willReturn(getAddValid());
		given(transformService.transformGeo(anyMap(), anyObject())).willReturn(getAddValid());

		service.completeWorkflow(file);
		
		GatewayReport rtn = WorkflowController.getReport();

		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), anyObject());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), anyObject());
		verify(transformService).transformGeo(anyMap(), anyObject());
		verify(legacyCruService).addTransaction(anyString(), anyString(), anyString(), anyObject());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString(), anyObject());
		verify(fileExportService).exportAdd(anyString(), anyString(), anyString(), anyObject());
		verify(fileExportService, never()).exportUpdate(anyString(), anyString(), anyString(), anyObject());
		assertFalse(rtn.getSites().get(0).getIsSuccess());
		assertFalse(rtn.getSites().get(0).getSteps().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "400");
		assertTrue(rtn.getSites().get(1).getIsSuccess());
		assertEquals(rtn.getSites().get(1).getSteps().get(0).getHttpStatus().toString(), "201");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void oneAddTransaction_completeWorkflow_thenReturnCreated() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleAdd();
		Map<String, Object> ml = ddotRtn.get(0);
		Map<String, Object> mlValid = new HashMap<>(ml);
		mlValid.put("validation",legacyValidation);

		
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap(), anyObject())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), anyObject())).willReturn(mlValid);
		given(transformService.transformGeo(anyMap(), anyObject())).willReturn(mlValid);
		
		service.completeWorkflow(file);
		
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), anyObject());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), anyObject());
		verify(transformService).transformGeo(anyMap(), anyObject());
		verify(legacyCruService).addTransaction(anyString(), anyString(), anyString(), anyObject());
		verify(fileExportService).exportAdd(anyString(), anyString(), anyString(), anyObject());
		assertTrue(rtn.getSites().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "201");
		assertTrue(rtn.getSites().get(0).getSteps().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), LegacyWorkflowService.COMPLETE_TRANSACTION_STEP_SUCCESS);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), LegacyWorkflowService.COMPLETE_TRANSACTION_STEP);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void oneUpdateTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUpdate();
		Map<String, Object> ml = ddotRtn.get(0);
		Map<String, Object> mlValid = new HashMap<>(ml);
		mlValid.put("validation",legacyValidation);

		
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap(), anyObject())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), anyObject())).willReturn(mlValid);
		given(transformService.transformGeo(anyMap(), anyObject())).willReturn(mlValid);

		service.completeWorkflow(file);
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), anyObject());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), anyObject());
		verify(transformService).transformGeo(anyMap(), anyObject());
		verify(legacyCruService).updateTransaction(anyString(), anyString(), anyString(), anyObject());
		verify(fileExportService).exportUpdate(anyString(), anyString(), anyString(), anyObject());
		assertTrue(rtn.getSites().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "200");
		assertTrue(rtn.getSites().get(0).getSteps().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), LegacyWorkflowService.COMPLETE_TRANSACTION_STEP_SUCCESS);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), LegacyWorkflowService.COMPLETE_TRANSACTION_STEP);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void ddotAddValidation_callsCorrectBackingServices() throws Exception {
		
		Map<String, Object> ml = getAdd();
		Map<String, Object> mlValid = getAddValid();
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		given(transformService.transformStationIx(anyMap(), anyObject())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), anyObject())).willReturn(mlValid);
		
		service.ddotValidation(file);
		
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), anyObject());
		verify(legacyValidatorService).doValidation(anyMap(), eq(true), anyObject());
		verify(legacyValidatorService, never()).doValidation(anyMap(), eq(false), anyObject());
		assertTrue(rtn.getSites().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "200");
		assertTrue(rtn.getSites().get(0).getSteps().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), JSONObject.escape(LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP_SUCCESS));
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void ddotUpdateValidation_callsCorrectBackingServices() throws Exception {
		Map<String, Object> ml = getUpdate();
		Map<String, Object> mlValid = getUpdateValid();
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleUpdate());
		given(transformService.transformStationIx(anyMap(), anyObject())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), anyObject())).willReturn(mlValid);
		
		service.ddotValidation(file);
		GatewayReport rtn = WorkflowController.getReport();

		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), anyObject());
		verify(legacyValidatorService).doValidation(anyMap(), eq(false), anyObject());
		verify(legacyValidatorService, never()).doValidation(anyMap(), eq(true), anyObject());
		assertTrue(rtn.getSites().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "200");
		assertTrue(rtn.getSites().get(0).getSteps().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), JSONObject.escape(LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP_SUCCESS));
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void ddotValidation_throwsException() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		given(transformService.transformStationIx(anyMap(), anyObject())).willReturn(getAdd());
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), anyObject())).willThrow(new RuntimeException());

		service.ddotValidation(file);

		GatewayReport rtn = WorkflowController.getReport();
		
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "500");
		assertFalse(rtn.getSites().get(0).getSteps().get(0).getIsSuccess());
		assertFalse(rtn.getSites().get(0).getIsSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), "{\"error_message\": \"null\"}");
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), anyObject());
	}
}