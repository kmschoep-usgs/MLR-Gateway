package gov.usgs.wma.mlrgateway.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

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
import static org.mockito.Matchers.anyBoolean;

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

	@SuppressWarnings("unchecked")
	@Test
	public void updatePrimaryKeyTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		Map<String, Object> ml = getAdd();

		given(transformService.transformStationIx(anyMap(), anyObject())).willReturn(ml);
		given(legacyCruService.getMonitoringLocation(anyObject(), anyObject(), anyBoolean(), anyObject())).willReturn(ml);
		
		service.updatePrimaryKeyWorkflow(oldAgencyCode, oldSiteNumber, newAgencyCode, newSiteNumber);
		
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(transformService).transformStationIx(anyMap(), anyObject());
		verify(legacyCruService).addTransaction(anyString(), anyString(), anyString(), anyObject());
		verify(legacyCruService).updateTransaction(anyString(), anyString(), anyString(), anyObject());
		verify(fileExportService).exportAdd(anyString(), anyString(), anyString(), anyObject());
		verify(fileExportService).exportUpdate(anyString(), anyString(), anyString(), anyObject());
		assertTrue(rtn.getSites().get(0).isSuccess());
		assertEquals(UpdatePrimaryKeyWorkflowService.TRANSACTION_TYPE_UPDATE, rtn.getSites().get(0).getTransactionType());
		assertEquals(oldSiteNumber, rtn.getSites().get(0).getSiteNumber());
		assertEquals(oldAgencyCode, rtn.getSites().get(0).getAgencyCode());
		assertTrue(rtn.getSites().get(1).isSuccess());
		assertEquals(UpdatePrimaryKeyWorkflowService.TRANSACTION_TYPE_ADD, rtn.getSites().get(1).getTransactionType());
		assertEquals(newSiteNumber, rtn.getSites().get(1).getSiteNumber());
		assertEquals(newAgencyCode, rtn.getSites().get(1).getAgencyCode());
	}

}