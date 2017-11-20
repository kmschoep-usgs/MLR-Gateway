package gov.usgs.wma.mlrgateway.workflow;

import gov.usgs.wma.mlrgateway.workflow.ExportWorkflowService;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.controller.ExportWorkflowController;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.never;

@RunWith(SpringRunner.class)
public class ExportWorkflowServiceTest extends BaseSpringTest {
	@MockBean
	private LegacyCruService legacyCruService;
	@MockBean
	private FileExportService fileExportService;
	
	private ExportWorkflowService service;
	private MockHttpServletResponse response;
	private ObjectMapper mapper;
	private String reportName = "TEST Legacy Workflow";

	@Before
	public void init() {
		service = new ExportWorkflowService(legacyCruService, fileExportService);
		response = new MockHttpServletResponse();
		ExportWorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void completeWorkflow_goodSite() throws Exception {
		Map<String,Object> site;
		site = getAdd();
		
		given(legacyCruService.getMonitoringLocation(anyString(), anyString())).willReturn(site);

		service.exportWorkflow("USGS", "12345678");
		
		verify(legacyCruService).getMonitoringLocation(anyString(), anyString());
		verify(fileExportService).exportAdd(anyString(), anyString(), anyString());
	}
	
	@Test
	public void completeWorkflow_siteDoesNotExist() throws Exception {
		given(legacyCruService.getMonitoringLocation(anyString(), anyString())).willReturn(new HashMap());
		
		service.exportWorkflow("USGS", "1234");
		
		verify(legacyCruService).getMonitoringLocation(anyString(), anyString());
		verify(fileExportService, never()).exportAdd(anyString(), anyString(), anyString());
	}

}