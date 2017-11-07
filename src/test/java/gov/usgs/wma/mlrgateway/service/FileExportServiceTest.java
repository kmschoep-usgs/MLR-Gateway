package gov.usgs.wma.mlrgateway.service;

import gov.usgs.wma.mlrgateway.client.FileExportClient;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
public class FileExportServiceTest {
	private String reportName = "TEST EXPORT";
	private FileExportService service;
	private ObjectMapper mapper;
	private String exportJson = "{}";
	
	@MockBean
	FileExportClient fileExportClient;

	@Before
	public void init() {
		service = new FileExportService(fileExportClient);
		WorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void addExport_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + FileExportService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(FileExportService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> exportRtn = new ResponseEntity<>(exportJson, HttpStatus.OK);
		given(fileExportClient.exportAdd(anyString())).willReturn(exportRtn);

		service.exportAdd("USGS ", "12345678       ", "{}");
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void addExport_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + FileExportService.EXPORT_ADD_STEP + "\",\"status\":500,\"details\":\"" + "Export add failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(fileExportClient.exportAdd(anyString())).willThrow(new RuntimeException());

		try {
			service.exportAdd("USGS ", "12345678       ", "{}");
			fail("updateExport did not throw exception to caller");
		} catch (Exception e) {}
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}
	
	@Test
	public void updateExport_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + FileExportService.EXPORT_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(FileExportService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> exportRtn = new ResponseEntity<>(exportJson, HttpStatus.OK);
		given(fileExportClient.exportUpdate(anyString())).willReturn(exportRtn);

		service.exportUpdate("USGS ", "12345678       ", "{}");
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
	}

	@Test
	public void updateExport_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + FileExportService.EXPORT_UPDATE_STEP + "\",\"status\":500,\"details\":\"" + "Export update failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(fileExportClient.exportUpdate(anyString())).willThrow(new RuntimeException());
		
		try {
			service.exportUpdate("USGS ", "12345678       ", "{}");
			fail("updateExport did not throw exception to caller");
		} catch (Exception e) {}
		
		
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
	}
}