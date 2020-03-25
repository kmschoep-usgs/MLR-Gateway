package gov.usgs.wma.mlrgateway.workflow;

import static org.junit.Assert.assertEquals;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.service.LegacyTransformerService;

@RunWith(SpringRunner.class)
public class UpdatePrimaryKeyWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private LegacyTransformerService transformService;
	@MockBean
	private LegacyCruService legacyCruService;
	@MockBean
	private FileExportService fileExportService;

	private UpdatePrimaryKeyWorkflowService service;
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
		service = new UpdatePrimaryKeyWorkflowService(legacyCruService, transformService, fileExportService);
		WorkflowController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
	}

	@Test
	public void updatePrimaryKeyTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		Map<String, Object> ml = getAdd();

		given(transformService.transformStationIx(anyMap(), any())).willReturn(ml);
		given(legacyCruService.getMonitoringLocation(any(), any(), anyBoolean(), any())).willReturn(ml);
		
		service.updatePrimaryKeyWorkflow(oldAgencyCode, oldSiteNumber, newAgencyCode, newSiteNumber);
		
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(transformService).transformStationIx(anyMap(), any());
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
		verify(transformService, never()).transformStationIx(anyMap(), any());
		verify(legacyCruService, never()).addTransaction(anyString(), anyString(), anyString(), any());
		verify(legacyCruService, never()).updateTransaction(anyString(), anyString(), anyString(), any());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), any());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), any());
	}

}