package gov.usgs.wma.mlrgateway.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.LinkedList;
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
import gov.usgs.wma.mlrgateway.controller.BulkTransactionFilesWorkflowController;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.util.ParseCSV;

@ExtendWith(SpringExtension.class)
public class BulkTransactionFilesWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private ParseCSV parseService;
	@MockBean
	private LegacyCruService legacyCruService;
	@MockBean
	private FileExportService fileExportService;

	private BulkTransactionFilesWorkflowService service;
	private MockHttpServletResponse response;
	private String reportName = "TEST Bulk Transaction File Workflow";
	private String fileName = "sites.csv";
	private String userName = "userName";
	private String reportDate = "01/01/2019";
	private static String fileContents = "agency_cd,site_no\nUSGS,01234\nblah,05431";
	private static List<String[]> mlList = new LinkedList<>();

	@BeforeEach
	public void init() {
		service = new BulkTransactionFilesWorkflowService(parseService, legacyCruService, fileExportService);
		response = new MockHttpServletResponse();
		BulkTransactionFilesWorkflowController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
		String [] ml1 = new String[] {"USGS","01234"};
		mlList.clear();
		mlList.add(ml1);
	}

	@Test
	public void oneTransaction_bulkTransactionFilesWorkflow_thenReturnCreated() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", ".csv", "text/plain", fileContents.getBytes());
		Map<String, Object> m1 = new HashMap<>();
		m1.put(LegacyWorkflowService.AGENCY_CODE, "USGS ");
		m1.put(LegacyWorkflowService.SITE_NUMBER, "01234       ");
		
		given(parseService.getMlList(any(MultipartFile.class))).willReturn(mlList);
		given(legacyCruService.getMonitoringLocation(any(), any(), anyBoolean(), any())).willReturn(m1);
		
		
		service.generateTransactionFilesWorkflow(file);
		
		GatewayReport rtn = WorkflowController.getReport();
		
		verify(parseService).getMlList(any(MultipartFile.class));
		verify(legacyCruService).getMonitoringLocation(any(), any(), anyBoolean(), any());
		verify(fileExportService).exportUpdate(anyString(), anyString(), any(), any());
		assertTrue(rtn.getSites().get(0).isSuccess());
	}

	@Test
	public void bulkTransactionFilesWorkflow_throwsException() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", ".csv", "text/plain", fileContents.getBytes());
		Map<String, Object> m1 = new HashMap<>();
		m1.put(LegacyWorkflowService.AGENCY_CODE, "USGS ");
		m1.put(LegacyWorkflowService.SITE_NUMBER, "01234       ");
		
		given(parseService.getMlList(any(MultipartFile.class))).willReturn(mlList);
		given(legacyCruService.getMonitoringLocation(any(), any(), anyBoolean(), any())).willThrow(new RuntimeException());

		service.generateTransactionFilesWorkflow(file);

		GatewayReport rtn = WorkflowController.getReport();
		
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "500");
		assertFalse(rtn.getSites().get(0).getSteps().get(0).isSuccess());
		assertFalse(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), "{\"error_message\": \"null\"}");
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_STEP);
		verify(parseService).getMlList(any(MultipartFile.class));
		verify(legacyCruService).getMonitoringLocation(any(), any(), anyBoolean(), any());
	}

}