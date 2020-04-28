package gov.usgs.wma.mlrgateway.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.opencsv.exceptions.CsvException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.controller.BulkTransactionFilesWorkflowController;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.util.ParseCSV;

@Service
public class BulkTransactionFilesWorkflowService {
	private static final Logger LOG = LoggerFactory.getLogger(BulkTransactionFilesWorkflowService.class);
	private ParseCSV parseService;
	private LegacyCruService legacyCruService;
	private FileExportService fileExportService;
	
	public static final String AGENCY_CODE = "agencyCode";
	public static final String SITE_NUMBER = "siteNumber";	
	public static final String BULK_GENERATE_TRANSACTION_FILES_WORKFLOW = "Submit a CSV file of agency codes and site numbers to bulk generate transaction files";
	public static final String BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_SUBJECT = "Submitted Bulk Generation Transactions file";
	public static final String BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED = "Submit a CSV file of agency codes and site numbers to bulk generate transaction files failed";

	public static final String GET_LIST_OF_MONITORING_LOCATIONS_STEP = "Retrieve a list of agency codes and site numbers from a CSV file";
	public static final String GET_LIST_OF_MONITORING_LOCATIONS_STEP_FAILED = "Retrieve a list of agency codes and site numbers from a CSV file failed";
	public static final String GENERATE_TRANSACTION_FILE_STEP = "Generate transaction file for monitoring location";
	public static final String GENERATE_TRANSACTION_FILE_STEP_FAILED = "Generate transaction file failed";
	


	@Autowired
	public BulkTransactionFilesWorkflowService(ParseCSV parseService, LegacyCruService legacyCruService, FileExportService fileExportService) {
		this.parseService = parseService;
		this.legacyCruService = legacyCruService;
		this.fileExportService = fileExportService;
	}

	public void generateTransactionFilesWorkflow(MultipartFile file) throws HystrixBadRequestException {
		String json;
		//1. Parse CSV File
		LOG.trace("Start CSV file Parsing");
		List<String[]> monitoringLocations = new LinkedList<>();
		try {
			monitoringLocations = parseService.getMlList(file);
		} catch (CsvException | IOException | FeignBadResponseWrapper e) {
			LOG.error("An error occurred while parsing input file [" + file.getOriginalFilename() + "] ", e);	
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				BulkTransactionFilesWorkflowController.addWorkflowStepReport(new StepReport(GET_LIST_OF_MONITORING_LOCATIONS_STEP_FAILED, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				BulkTransactionFilesWorkflowController.addWorkflowStepReport(new StepReport(GET_LIST_OF_MONITORING_LOCATIONS_STEP_FAILED, status, false, e.getMessage()));
			}
		}
		LOG.trace("End CSV file Parsing");

		//2. Get Individual Monitoring Locations
		LOG.trace("Start processing monitoring locations [" + file.getOriginalFilename() + "] " + monitoringLocations.size());
		 for (String[] ml : monitoringLocations) {
			 SiteReport siteReport = new SiteReport(ml[0], ml[1]);
		 try {
			 Map<String, Object> monitoringLocation = new HashMap<>();
			 monitoringLocation = legacyCruService.getMonitoringLocation(ml[0].trim(), ml[1].trim(), false, siteReport);
			 json = mlToJson(monitoringLocation);
			 fileExportService.exportUpdate(monitoringLocation.get(AGENCY_CODE).toString(), monitoringLocation.get(SITE_NUMBER).toString(), json, siteReport);
			 BulkTransactionFilesWorkflowController.addSiteReport(siteReport);	
		 	} catch (Exception e) {
		 		if(e instanceof FeignBadResponseWrapper){
		 			LOG.error("An error occurred while processing transaction: agency code: " + ml[0] + "site number: " + ml[1], e);
		 			siteReport.addStepReport(new StepReport(GENERATE_TRANSACTION_FILE_STEP, ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
		 			BulkTransactionFilesWorkflowController.addSiteReport(siteReport);
			 	} else {
		 			LOG.error("An error occurred while processing transaction: agency code: " + ml[0] + "site number: " + ml[1], e);
					siteReport.addStepReport(new StepReport(GENERATE_TRANSACTION_FILE_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
					BulkTransactionFilesWorkflowController.addSiteReport(siteReport);
				}
		 	}
		 }
		 LOG.trace("End processing monitoring locations [" + file.getOriginalFilename() + "] " + monitoringLocations.size());
	}

	
	protected String mlToJson(Map<String, Object> ml) {
		ObjectMapper mapper = new ObjectMapper();
		String json = "{}";
		
		try {
			json = mapper.writeValueAsString(ml);
		} catch (Exception e) {
			// Unable to determine when this might actually happen, but the api says it can...
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize transformer output.\"}");
		}
		
		return json;
	}
}
