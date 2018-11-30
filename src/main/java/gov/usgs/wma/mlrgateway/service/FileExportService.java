package gov.usgs.wma.mlrgateway.service;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.FileExportClient;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class FileExportService {
	private FileExportClient fileExportClient;
	private Logger log = LoggerFactory.getLogger(FileExportService.class);
	
	public static final String STEP_NAME = "";
	public static final String EXPORT_ADD_STEP = "Export Add Transaction File";
	public static final String EXPORT_UPDATE_STEP = "Export Update Transaction File";
	public static final String EXPORT_SUCCESSFULL = "Transaction File created Successfully.";
	public static final String EXPORT_ADD_FAILED = "Export add failed";
	public static final String EXPORT_UPDATE_FAILED = "Export update failed";
	
	@Autowired
	public FileExportService(FileExportClient fileExportClient){
		this.fileExportClient = fileExportClient;
	}
	
	public void exportAdd(String agencyCode, String siteNumber, String json, SiteReport siteReport) {
		try {
			ResponseEntity<String> exportResp = fileExportClient.exportAdd(json);
			int exportStatus = exportResp.getStatusCodeValue();
			siteReport.addStepReport(new StepReport(EXPORT_ADD_STEP, exportStatus, 200 == exportStatus ? true : false, 200 == exportStatus ? EXPORT_SUCCESSFULL : exportResp.getBody()));
		} catch (Exception e) {
			siteReport.addStepReport(new StepReport(EXPORT_ADD_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, EXPORT_ADD_FAILED));
			log.error(EXPORT_ADD_STEP + ": " + e.getMessage());
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"" + EXPORT_ADD_FAILED + "\"}");	
		}
	}

	public void exportUpdate(String agencyCode, String siteNumber, String json, SiteReport siteReport) {
		try {
			ResponseEntity<String> exportResp = fileExportClient.exportUpdate(json);
			int exportStatus = exportResp.getStatusCodeValue();
			siteReport.addStepReport(new StepReport(EXPORT_UPDATE_STEP, exportStatus, 200 == exportStatus ? true : false, 200 == exportStatus ? EXPORT_SUCCESSFULL : exportResp.getBody()));
		} catch (Exception e) {
			siteReport.addStepReport(new StepReport(EXPORT_UPDATE_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, EXPORT_UPDATE_FAILED));
			log.error(EXPORT_UPDATE_STEP + ": " + e.getMessage());
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"" + EXPORT_UPDATE_FAILED + "\"}");	
		}
	}
}
