package gov.usgs.wma.mlrgateway.workflow;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.controller.ExportWorkflowController;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;

import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.never;

@ExtendWith(SpringExtension.class)
public class ExportWorkflowServiceTest extends BaseSpringTest {
	@MockBean
	private LegacyCruService legacyCruService;
	@MockBean
	private FileExportService fileExportService;
	
	private ExportWorkflowService service;
	private String fileName = "test.d";
	private String reportName = "TEST Legacy Workflow";
	private String userName = "userName";
	private String reportDate = "01/01/2019";

	@BeforeEach
	public void init() {
		service = new ExportWorkflowService(legacyCruService, fileExportService);
		ExportWorkflowController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
	}

	@Test
	public void completeWorkflow_goodSite() throws Exception {
		Map<String,Object> site;
		site = getAdd();
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(site);

		service.exportWorkflow("USGS", "12345678");
		
		verify(legacyCruService).getMonitoringLocation(anyString(), anyString(), anyBoolean(), any());
		verify(fileExportService).exportAdd(anyString(), anyString(), anyString(), any());
	}
	
	@Test
	public void completeWorkflow_siteDoesNotExist() throws Exception {
		given(legacyCruService.getMonitoringLocation(anyString(), anyString(), anyBoolean(), any())).willReturn(new HashMap<>());
		
		service.exportWorkflow("USGS", "1234");
		
		verify(legacyCruService).getMonitoringLocation(anyString(), anyString(), anyBoolean(), any());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString(), any());
	}

}