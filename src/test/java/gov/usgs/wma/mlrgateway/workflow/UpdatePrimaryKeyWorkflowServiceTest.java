package gov.usgs.wma.mlrgateway.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.service.LegacyTransformerService;
import gov.usgs.wma.mlrgateway.service.LegacyValidatorService;

@RunWith(SpringRunner.class)
public class UpdatePrimaryKeyWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private LegacyTransformerService transformService;
	@MockBean
	private LegacyValidatorService legacyValidatorService;
	@MockBean
	private LegacyCruService legacyCruService;
	@MockBean
	private FileExportService fileExportService;

	private UpdatePrimaryKeyWorkflowService service;
	private MockHttpServletResponse response;
	private String reportName = "TEST Legacy Workflow";
	private String fileName = "";
	private String userName = "userName";
	private String reportDate = "01/01/2019";
	private String oldAgencyCode = "USGS ";
	private String newAgencyCode = "BLAH ";
	private String oldSiteNumber = "12345678       ";
	private String newSiteNumber = "87654321       ";
	
	@Before
	public void init() {
		service = new UpdatePrimaryKeyWorkflowService(legacyCruService, legacyValidatorService, transformService, fileExportService);
		response = new MockHttpServletResponse();
		WorkflowController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
	}

	@Test
	public void updatePrimaryKeyTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		Map<String, Object> ml = getAdd();
		Map<String, Object> mlValid = new HashMap<>(ml);
		mlValid.put("validation",legacyValidation);

		given(transformService.transformStationIx(anyMap(), any())).willReturn(ml);
		given(legacyCruService.getMonitoringLocation(any(), any(), anyBoolean(), any())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), any())).willReturn(mlValid);
		
		service.updatePrimaryKeyWorkflow(oldAgencyCode, oldSiteNumber, newAgencyCode, newSiteNumber);
		
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(transformService).transformStationIx(anyMap(), any());
		verify(legacyValidatorService, Mockito.times(2)).doValidation(anyMap(), anyBoolean(), any());
		verify(legacyCruService).addTransaction(anyString(), anyString(), anyString(), any());
		verify(legacyCruService).updateTransaction(anyString(), anyString(), anyString(), any());
		verify(fileExportService).exportAdd(anyString(), anyString(), eq(null), any());
		verify(fileExportService).exportUpdate(anyString(), anyString(), eq(null), any());
		assertTrue(rtn.getSites().get(0).isSuccess());
		assertEquals(UpdatePrimaryKeyWorkflowService.TRANSACTION_TYPE_UPDATE, rtn.getSites().get(0).getTransactionType());
		assertEquals(oldSiteNumber, rtn.getSites().get(0).getSiteNumber());
		assertEquals(oldAgencyCode, rtn.getSites().get(0).getAgencyCode());
		assertTrue(rtn.getSites().get(1).isSuccess());
		assertEquals(UpdatePrimaryKeyWorkflowService.TRANSACTION_TYPE_ADD, rtn.getSites().get(1).getTransactionType());
		assertEquals(newSiteNumber, rtn.getSites().get(1).getSiteNumber());
		assertEquals(newAgencyCode, rtn.getSites().get(1).getAgencyCode());
	}
	
	@Test
	public void updatePrimaryKeyWorkflow_siteDoesNotExist() throws Exception {
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(new HashMap<>());
		given(transformService.transformStationIx(anyMap(), any())).willReturn(new HashMap<>());
		
		service.updatePrimaryKeyWorkflow(oldAgencyCode, oldSiteNumber, newAgencyCode, newSiteNumber);
		
		verify(legacyCruService).getMonitoringLocation(anyString(), anyString(), anyBoolean(), any());
		verify(legacyValidatorService, never()).doValidation(anyMap(), anyBoolean(), any());
		verify(transformService, never()).transformStationIx(anyMap(), any());
		verify(legacyCruService, never()).addTransaction(anyString(), anyString(), anyString(), any());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString(), any());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), any());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), any());
	}
	
	@Test
	public void updatePrimaryKeyWorkflow_validator_throwsException() throws Exception {
		Map<String, Object> ml = getAdd();

		given(transformService.transformStationIx(anyMap(), any())).willReturn(ml);
		given(legacyCruService.getMonitoringLocation(any(), any(), anyBoolean(), any())).willReturn(ml);
		given(legacyValidatorService.doValidation(anyMap(), anyBoolean(), any())).willThrow(new RuntimeException());

		service.updatePrimaryKeyWorkflow(oldAgencyCode, oldSiteNumber, newAgencyCode, newSiteNumber);

		GatewayReport rtn = WorkflowController.getReport();
		
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "500");
		assertFalse(rtn.getSites().get(0).getSteps().get(0).isSuccess());
		assertFalse(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), "{\"error_message\": \"null\"}");
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), UpdatePrimaryKeyWorkflowService.PRIMARY_KEY_UPDATE_TRANSACTION_STEP);
		verify(legacyCruService).getMonitoringLocation(anyString(), anyString(), anyBoolean(), any());
		verify(legacyValidatorService).doValidation(anyMap(), anyBoolean(), any());
		verify(transformService, never()).transformStationIx(anyMap(), any());
		verify(legacyCruService, never()).addTransaction(anyString(), anyString(), anyString(), any());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString(), any());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), any());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), any());
	}

}