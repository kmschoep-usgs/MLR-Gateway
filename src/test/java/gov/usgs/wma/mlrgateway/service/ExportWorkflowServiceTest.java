package gov.usgs.wma.mlrgateway.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.client.FileExportClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.ExportWorkflowController;
import net.minidev.json.JSONObject;

@RunWith(SpringRunner.class)
public class ExportWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private LegacyCruClient legacyCruClient;
	@MockBean
	private FileExportClient fileExportClient;
	@MockBean
	private NotificationClient notificationClient;

	private ExportWorkflowService service;
	private MockHttpServletResponse response;
	private ObjectMapper mapper;
	private String reportName = "TEST Legacy Workflow";
	private String legacyJson = "[{\"agencyCode\": \"USGS\",\"siteNumber\": \"12345678\"}]";
	private String legacyExportJson = "{\"agencyCode\": \"USGS\",\"siteNumber\": \"12345678\"}";

	@Before
	public void init() {
		service = new ExportWorkflowService(legacyCruClient, fileExportClient, notificationClient);
		response = new MockHttpServletResponse();
		ExportWorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void completeWorkflow_goodSite() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + ExportWorkflowService.SITE_GET_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(ExportWorkflowService.SITE_GET_SUCCESSFULL)
					+ "\",\"agencyCode\":\"USGS\",\"siteNumber\":\"12345678\"},"
				+ "{\"name\":\"" + ExportWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(ExportWorkflowService.EXPORT_SUCCESSFULL)
					+ "\",\"agencyCode\":\"USGS\",\"siteNumber\":\"12345678\"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyExportJson, HttpStatus.OK);
		ResponseEntity<String> addRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.getMonitoringLocations(anyString(), anyString())).willReturn(addRtn);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);

		service.completeWorkflow("USGS", "12345678");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(ExportWorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocations(anyString(), anyString());
		verify(fileExportClient).exportAdd(anyString());
	}
	
	@Test
	public void completeWorkflow_siteDoesNotExist() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + ExportWorkflowService.SITE_GET_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(ExportWorkflowService.SITE_GET_DOES_NOT_EXIST)
					+ "\",\"agencyCode\":\"USGS\",\"siteNumber\":\"1234\"}"
				+ "]}";
		ResponseEntity<String> addRtn = new ResponseEntity<String>("[]", HttpStatus.OK);
		given(legacyCruClient.getMonitoringLocations(anyString(), anyString())).willReturn(addRtn);

		service.completeWorkflow("USGS", "1234");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(ExportWorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyCruClient).getMonitoringLocations(anyString(), anyString());
	}

}
