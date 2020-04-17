package gov.usgs.wma.mlrgateway.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
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

@ExtendWith(SpringExtension.class)
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
	private String fileName = "test.d";
	private String userName = "userName";
	private String reportDate = "01/01/2019";

	@BeforeEach
	public void init() {
		service = new LegacyWorkflowService(ddotService, legacyCruService, transformService, legacyValidatorService, fileExportService);
		response = new MockHttpServletResponse();
		WorkflowController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
	}

	@Test
	public void noTransactionType_completeWorkflow_thenReturnBadRequest() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUnknown();
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);

		service.completeWorkflow(file);

		GatewayReport rtn = WorkflowController.getReport();
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transformStationIx(anyMap(), any());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean(), any());
		verify(transformService, never()).transformGeo(anyMap(), any());
		verify(legacyCruService, never()).addTransaction(anyString(), anyString(), anyString(), any());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString(), any());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), any());
		verify(fileExportService, never()).exportUpdate(anyString(), anyString(), anyString(), any());
		assertEquals(rtn.getSites().get(0).isSuccess(), false);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).isSuccess(), false);
		assertNull(rtn.getSites().get(0).getTransactionType());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), LegacyWorkflowService.COMPLETE_TRANSACTION_STEP);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
	}

	@Test
	public void noTransactionType_completeWorkflow_noStopOnError() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.multipleWithErrors();
		Map<String, Object> ml = getAdd();

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap(), any())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), any())).willReturn(getAddValid());
		given(transformService.transformGeo(anyMap(), any())).willReturn(getAddValid());

		service.completeWorkflow(file);
		
		GatewayReport rtn = WorkflowController.getReport();

		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), any());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), any());
		verify(transformService).transformGeo(anyMap(), any());
		verify(legacyCruService).addTransaction(anyString(), anyString(), anyString(), any());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString(), any());
		verify(fileExportService).exportAdd(anyString(), anyString(), eq(null), any());
		verify(fileExportService, never()).exportUpdate(anyString(), anyString(), anyString(), any());
		assertFalse(rtn.getSites().get(0).isSuccess());
		assertFalse(rtn.getSites().get(0).getSteps().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "400");
	}

	@Test
	public void oneAddTransaction_completeWorkflow_thenReturnCreated() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleAdd();
		Map<String, Object> ml = ddotRtn.get(0);
		Map<String, Object> mlValid = new HashMap<>(ml);
		mlValid.put("validation",legacyValidation);

		
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap(), any())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), any())).willReturn(mlValid);
		given(transformService.transformGeo(anyMap(), any())).willReturn(mlValid);
		
		service.completeWorkflow(file);
		
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), any());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), any());
		verify(transformService).transformGeo(anyMap(), any());
		verify(legacyCruService).addTransaction(anyString(), anyString(), anyString(), any());
		verify(fileExportService).exportAdd(anyString(), anyString(), eq(null), any());
		assertTrue(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getTransactionType(), LegacyWorkflowService.TRANSACTION_TYPE_ADD);
	}

	@Test
	public void oneUpdateTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUpdate();
		Map<String, Object> ml = ddotRtn.get(0);
		Map<String, Object> mlValid = new HashMap<>(ml);
		mlValid.put("validation",legacyValidation);

		
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(transformService.transformStationIx(anyMap(), any())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), any())).willReturn(mlValid);
		given(transformService.transformGeo(anyMap(), any())).willReturn(mlValid);

		service.completeWorkflow(file);
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), any());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), any());
		verify(transformService).transformGeo(anyMap(), any());
		verify(legacyCruService).updateTransaction(anyString(), anyString(), anyString(), any());
		verify(fileExportService).exportUpdate(anyString(), anyString(), eq(null), any());
		assertTrue(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getTransactionType(), LegacyWorkflowService.TRANSACTION_TYPE_UPDATE);
	}
	
	@Test
	public void ddotAddValidation_callsCorrectBackingServices() throws Exception {
		
		Map<String, Object> ml = getAdd();
		Map<String, Object> mlValid = getAddValid();
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		given(transformService.transformStationIx(anyMap(), any())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), any())).willReturn(mlValid);
		
		service.ddotValidation(file);
		
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), any());
		verify(legacyValidatorService).doValidation(anyMap(), eq(true), any());
		verify(legacyValidatorService, never()).doValidation(anyMap(), eq(false), any());
		assertTrue(rtn.getSites().get(0).isSuccess());
	}

	@Test
	public void ddotUpdateValidation_callsCorrectBackingServices() throws Exception {
		Map<String, Object> ml = getUpdate();
		Map<String, Object> mlValid = getUpdateValid();
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleUpdate());
		given(transformService.transformStationIx(anyMap(), any())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), any())).willReturn(mlValid);
		
		service.ddotValidation(file);
		GatewayReport rtn = WorkflowController.getReport();

		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transformStationIx(anyMap(), any());
		verify(legacyValidatorService).doValidation(anyMap(), eq(false), any());
		verify(legacyValidatorService, never()).doValidation(anyMap(), eq(true), any());
		assertTrue(rtn.getSites().get(0).isSuccess());
	}

	@Test
	public void ddotValidation_throwsException() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		given(transformService.transformStationIx(anyMap(), any())).willReturn(getAdd());
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), any())).willThrow(new RuntimeException());

		service.ddotValidation(file);

		GatewayReport rtn = WorkflowController.getReport();
		
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "500");
		assertFalse(rtn.getSites().get(0).getSteps().get(0).isSuccess());
		assertFalse(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), "{\"error_message\": \"null\"}");
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), LegacyWorkflowService.VALIDATE_DDOT_TRANSACTION_STEP);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), any());
	}
}