package gov.usgs.wma.mlrgateway.service;

import gov.usgs.wma.mlrgateway.client.FileExportClient;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
public class FileExportServiceTest {
	private String reportName = "TEST EXPORT";
	private FileExportService service;
	private String exportJson = "{}";
	private String fileName = "test.d";
	private final String agencyCode = "USGS ";
	private final String siteNumber = "12345678       ";
	private String userName = "userName";
	private String reportDate = "01/01/2019";
	
	@MockBean
	FileExportClient fileExportClient;

	@Before
	public void init() {
		service = new FileExportService(fileExportClient);
		WorkflowController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
	}

	@Test
	public void addExport_callsBackingServices() throws Exception {
		ResponseEntity<String> exportRtn = new ResponseEntity<>(exportJson, HttpStatus.OK);

		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(fileExportClient.exportAdd(anyString())).willReturn(exportRtn);

		service.exportAdd(agencyCode, siteNumber, "{}", siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		assertEquals(rtn.getName(), reportName);
		assertEquals(rtn.getInputFileName(), fileName);
		assertEquals(rtn.getSites().get(0).getAgencyCode(), agencyCode);
		assertEquals(rtn.getSites().get(0).getSiteNumber(), siteNumber);
		assertTrue(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().size(), 1);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), FileExportService.EXPORT_ADD_STEP);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), FileExportService.EXPORT_SUCCESSFULL);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "200");
		assertTrue(rtn.getSites().get(0).getSteps().get(0).isSuccess());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}

	@Test
	public void addExport_throwsException() throws Exception {

		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(fileExportClient.exportAdd(anyString())).willThrow(new RuntimeException());

		try {
			service.exportAdd(agencyCode, siteNumber, "{}", siteReport);
			fail("updateExport did not throw exception to caller");
		} catch (Exception e) {}
		
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		assertEquals(rtn.getSites().get(0).getAgencyCode(), agencyCode);
		assertEquals(rtn.getSites().get(0).getSiteNumber(), siteNumber);
		assertFalse(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), "{\"error_message\":\"null.  This error requires manual intervention to resolve. Please contact the support team for assistance.\"}");
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), FileExportService.EXPORT_ADD_STEP);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "500");
		assertFalse(rtn.getSites().get(0).getSteps().get(0).isSuccess());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
	}
	
	@Test
	public void updateExport_callsBackingServices() throws Exception {

		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		ResponseEntity<String> exportRtn = new ResponseEntity<>(exportJson, HttpStatus.OK);
		given(fileExportClient.exportUpdate(anyString())).willReturn(exportRtn);

		service.exportUpdate(agencyCode, siteNumber, "{}", siteReport);
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		assertEquals(rtn.getName(), reportName);
		assertEquals(rtn.getInputFileName(), fileName);
		assertEquals(rtn.getSites().get(0).getAgencyCode(), agencyCode);
		assertEquals(rtn.getSites().get(0).getSiteNumber(), siteNumber);
		assertTrue(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), FileExportService.EXPORT_SUCCESSFULL);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), FileExportService.EXPORT_UPDATE_STEP);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "200");
		assertTrue(rtn.getSites().get(0).getSteps().get(0).isSuccess());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
	}

	@Test
	public void updateExport_throwsException() throws Exception {

		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		given(fileExportClient.exportUpdate(anyString())).willThrow(new RuntimeException());
		
		try {
			service.exportUpdate(agencyCode, siteNumber, "{}", siteReport);
			fail("updateExport did not throw exception to caller");
		} catch (Exception e) {}
		
		GatewayReport rtn = WorkflowController.getReport();
		rtn.addSiteReport(siteReport);
		
		assertEquals(rtn.getSites().get(0).getAgencyCode(), agencyCode);
		assertEquals(rtn.getSites().get(0).getSiteNumber(), siteNumber);
		assertFalse(rtn.getSites().get(0).isSuccess());
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getDetails(), "{\"error_message\":\"null.  This error requires manual intervention to resolve. Please contact the support team for assistance.\"}");
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getName(), FileExportService.EXPORT_UPDATE_STEP);
		assertEquals(rtn.getSites().get(0).getSteps().get(0).getHttpStatus().toString(), "500");
		assertFalse(rtn.getSites().get(0).getSteps().get(0).isSuccess());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
	}
}