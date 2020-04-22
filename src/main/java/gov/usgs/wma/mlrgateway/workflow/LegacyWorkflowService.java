package gov.usgs.wma.mlrgateway.workflow;

import java.util.HashMap;
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

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.DdotService;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.service.LegacyValidatorService;
import gov.usgs.wma.mlrgateway.service.LegacyTransformerService;

@Service
public class LegacyWorkflowService {
	private static final Logger LOG = LoggerFactory.getLogger(LegacyWorkflowService.class);
	private DdotService ddotService;
	private LegacyTransformerService transformService;
	private LegacyValidatorService legacyValidatorService;
	private LegacyCruService legacyCruService;
	private FileExportService fileExportService;
	
	public static final String ID = "id";
	public static final String AGENCY_CODE = "agencyCode";
	public static final String SITE_NUMBER = "siteNumber";
	public static final String DISTRICT_CODE = "districtCode";
	public static final String TRANSACTION_TYPE = "transactionType";
	public static final String TRANSACTION_TYPE_ADD = "A";
	public static final String TRANSACTION_TYPE_UPDATE = "M";

	public static final String BAD_TRANSACTION_TYPE = "Unable to determine transaction type.";
	
	public static final String COMPLETE_WORKFLOW = "Validate and Process D dot File Workflow";
	public static final String COMPLETE_WORKFLOW_SUCCESS = "Validate and Process D dot File Workflow completed";
	public static final String COMPLETE_WORKFLOW_FAILED = "Validate and Process D dot File Workflow failed";
	public static final String VALIDATE_DDOT_WORKFLOW = "Validate D dot File";
	public static final String VALIDATE_DDOT_WORKFLOW_FAILED = "Validate D dot File workflow failed";
	public static final String VALIDATE_DDOT_WORKFLOW_SUCCESS = "Validate D dot File workflow completed";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW = "Site Agency Code and/or Site Number Update Workflow";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW_SUCCESS = "Site Agency Code and/or Site Number Update workflow completed";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW_FAILED = "Site Agency Code and/or Site Number Update workflow failed";
	
	public static final String VALIDATE_DDOT_TRANSACTION_STEP = "Validate Single D dot Transaction";
	public static final String VALIDATE_DDOT_TRANSACTION_STEP_FAILURE = "Single transaction validation failed.";
	public static final String COMPLETE_TRANSACTION_STEP = "Process Single D dot Transaction";
	public static final String PRIMARY_KEY_UPDATE_TRANSACTION_STEP = "Update Agency Code and/or Site Number";


	@Autowired
	public LegacyWorkflowService(DdotService ddotService, LegacyCruService legacyCruService, LegacyTransformerService transformService, 
			LegacyValidatorService legacyValidatorService, FileExportService fileExportService) {
		this.ddotService = ddotService;
		this.legacyCruService = legacyCruService;
		this.transformService = transformService;
		this.legacyValidatorService = legacyValidatorService;
		this.fileExportService = fileExportService;
	}

	public void completeWorkflow(MultipartFile file) throws HystrixBadRequestException {
		String json;
		
		//1. Parse Ddot File
		LOG.trace("Start DDOT Parsing");
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);
		LOG.trace("End DDOT Parsing");

		//2. Process Individual Transactions
		for (int i = 0; i < ddots.size(); i++) {
			LOG.trace("Start processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size());
			Map<String, Object> ml = ddots.get(i);
			SiteReport siteReport = new SiteReport(ml.get(AGENCY_CODE).toString(), ml.get(SITE_NUMBER).toString());
			try {
				if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
					Boolean isAddTransaction = ((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD);
					siteReport.setTransactionType(ml.get(TRANSACTION_TYPE).toString());
					ml = transformService.transformStationIx(ml, siteReport);
					ml = legacyValidatorService.doValidation(ml, isAddTransaction, siteReport);
					ml = transformService.transformGeo(ml, siteReport);
					json = mlToJson(ml);

					if (isAddTransaction) {
						json = legacyCruService.addTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json, siteReport);
						fileExportService.exportAdd(ml.get(AGENCY_CODE).toString(), ml.get(SITE_NUMBER).toString(), json, siteReport);
					} else {
						json = legacyCruService.patchTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json, siteReport);
						fileExportService.exportUpdate(ml.get(AGENCY_CODE).toString(), ml.get(SITE_NUMBER).toString(), json, siteReport);
					}

					WorkflowController.addSiteReport(siteReport);
				} else {
					throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
				}
			} catch (Exception e) {
				if(e instanceof FeignBadResponseWrapper){
					LOG.debug("An error occurred while processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size(), e);
					siteReport.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP, ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
					WorkflowController.addSiteReport(siteReport);
				} else {
					LOG.error("An error occurred while processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size(), e);
					siteReport.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
					WorkflowController.addSiteReport(siteReport);
				}
			}
			LOG.trace("End processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size());
		}
	}

	public void ddotValidation(MultipartFile file) throws HystrixBadRequestException {
		//1. Parse Ddot File
		LOG.trace("Start DDOT Parsing");
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);
		LOG.trace("End DDOT Parsing");
		
		//2. Process Individual Transactions
		for (int i = 0; i < ddots.size(); i++) {
			LOG.trace("Start processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size());
			Map<String, Object> ml = ddots.get(i);
			SiteReport siteReport = new SiteReport(ml.get(AGENCY_CODE).toString(), ml.get(SITE_NUMBER).toString());
			try {
				if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
					siteReport.setTransactionType(ml.get(TRANSACTION_TYPE).toString());
					ml = transformService.transformStationIx(ml, siteReport);
					if (((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD)) {
						ml = legacyValidatorService.doValidation(ml, true, siteReport);
					} else {
						ml = legacyValidatorService.doValidation(ml, false, siteReport);
					}
				} else {
					siteReport.addStepReport(new StepReport(LegacyValidatorService.VALIDATION_STEP, HttpStatus.SC_BAD_REQUEST, false, BAD_TRANSACTION_TYPE));
					throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
				}		
				
			} catch (Exception e) {
				if(e instanceof FeignBadResponseWrapper){
					LOG.debug("An error occurred while processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size(), e);
					siteReport.addStepReport(new StepReport(VALIDATE_DDOT_TRANSACTION_STEP , ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
				} else {
					LOG.error("An error occurred while processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size(), e);
					siteReport.addStepReport(new StepReport(VALIDATE_DDOT_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
				}
			}
			WorkflowController.addSiteReport(siteReport);
			LOG.trace("End processing transaction [" + file.getOriginalFilename() + "] " + (i+1) + "/" + ddots.size());
		}
	}
	
	public void updatePrimaryKeyWorkflow(String oldAgencyCode, String oldSiteNumber, String newAgencyCode, String newSiteNumber) throws HystrixBadRequestException {
		String json;
		Map<String, Object> monitoringLocation = new HashMap<>();
		Map<String, Object> updatedMonitoringLocation = new HashMap<>();
		SiteReport siteReport = new SiteReport(oldAgencyCode, oldSiteNumber);
		// TODO: This might change to a new transaction type once we figure out what the new transaction file needs to look like
		siteReport.setTransactionType("M");
		
		LOG.trace("Start processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ");
		
		monitoringLocation = legacyCruService.getMonitoringLocation(oldAgencyCode, oldSiteNumber, false, siteReport);
		try {
			if (!monitoringLocation.isEmpty()) {
				
				updatedMonitoringLocation.putAll(monitoringLocation);
				// TODO: This might change to a new transaction type once we figure out what the new transaction file needs to look like
				updatedMonitoringLocation.put(TRANSACTION_TYPE, "M");
				
				updatedMonitoringLocation.replace(AGENCY_CODE, newAgencyCode);
				updatedMonitoringLocation.replace(SITE_NUMBER, newSiteNumber);
				
				// Need full object to validate as an Add transaction.  Need to validate as an Add transaction because the update
				// validations will attempt to retrieve the existing record based on the new primary key, which won't exist.
				updatedMonitoringLocation = legacyValidatorService.doValidation(updatedMonitoringLocation, true, siteReport);
				
				json = mlToJson(updatedMonitoringLocation);
				
				// Need to submit entire record for update (vs. patch), otherwise fields that are not submitted are set to null in the database.
				json = legacyCruService.updateTransaction(updatedMonitoringLocation.get(ID).toString(), json, siteReport);
				fileExportService.exportUpdate(updatedMonitoringLocation.get(AGENCY_CODE).toString(), updatedMonitoringLocation.get(SITE_NUMBER).toString(), json, siteReport);
			}
		} catch (Exception e) {
			if(e instanceof FeignBadResponseWrapper){
				LOG.debug("An error occurred while processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ", e);
				siteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
			} else {
				LOG.error("An error occurred while processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ", e);
				siteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
			}
		}
		WorkflowController.addSiteReport(siteReport);
		LOG.trace("End processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ");
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
