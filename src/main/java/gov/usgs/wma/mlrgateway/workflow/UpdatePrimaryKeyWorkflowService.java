package gov.usgs.wma.mlrgateway.workflow;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.service.LegacyTransformerService;
import gov.usgs.wma.mlrgateway.service.LegacyValidatorService;

@Service
public class UpdatePrimaryKeyWorkflowService {
	private static final Logger LOG = LoggerFactory.getLogger(UpdatePrimaryKeyWorkflowService.class);
	private LegacyCruService legacyCruService;
	private LegacyValidatorService legacyValidatorService;
	private LegacyTransformerService transformService;
	private FileExportService fileExportService;
	
	public static final String AGENCY_CODE = "agencyCode";
	public static final String SITE_NUMBER = "siteNumber";
	public static final String DISTRICT_CODE = "districtCode";
	public static final String SITE_WEB_READY_CODE = "siteWebReadyCode";
	public static final String STATION_NAME = "stationName";
	public static final String TRANSACTION_TYPE = "transactionType";
	public static final String TRANSACTION_TYPE_ADD = "A";
	public static final String TRANSACTION_TYPE_UPDATE = "M";

	public static final String BAD_TRANSACTION_TYPE = "Unable to determine transaction type.";
	
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW = "Site Agency Code and/or Site Number Update Workflow";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW_SUCCESS = "Site Agency Code and/or Site Number Update workflow completed";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW_FAILED = "Site Agency Code and/or Site Number Update workflow failed";
	
	public static final String PRIMARY_KEY_UPDATE_TRANSACTION_STEP = "Update Agency Code and/or Site Number";

	@Autowired
	public UpdatePrimaryKeyWorkflowService(LegacyCruService legacyCruService, LegacyValidatorService legacyValidatorService, LegacyTransformerService transformService, FileExportService fileExportService) {
		this.legacyCruService = legacyCruService;
		this.legacyValidatorService = legacyValidatorService;
		this.transformService = transformService;
		this.fileExportService = fileExportService;
	}

	public void updatePrimaryKeyWorkflow(String oldAgencyCode, String oldSiteNumber, String newAgencyCode, String newSiteNumber) throws HystrixBadRequestException {
		String oldJson;
		String newJson;
		Map<String, Object> oldMonitoringLocation = new HashMap<>();
		Map<String, Object> newMonitoringLocation = new HashMap<>();
		Map<String, Object> transformedOldMonitoringLocation = new HashMap<>();
		SiteReport oldSiteReport = new SiteReport(oldAgencyCode, oldSiteNumber);
		SiteReport newSiteReport = new SiteReport(newAgencyCode, newSiteNumber);
		oldSiteReport.setTransactionType("M");
		newSiteReport.setTransactionType("A");
	
		LOG.trace("Start processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ");

		try {
			//1. Get old monitoring location
			LOG.trace("Get old monitoring location");
			oldMonitoringLocation = legacyCruService.getMonitoringLocation(oldAgencyCode, oldSiteNumber, false, oldSiteReport);
			
			if (!oldMonitoringLocation.isEmpty()) {
				//2. Set new monitoring location
				LOG.trace("Set new monitoring location");
				newMonitoringLocation.putAll(oldMonitoringLocation);
				
				//3.update new monitoring location with new AgencyCode and/or SiteNumber and validate
				newMonitoringLocation = createNewMonitoringLocation(newMonitoringLocation, newAgencyCode, newSiteNumber);
				newMonitoringLocation = legacyValidatorService.doValidation(newMonitoringLocation, true, newSiteReport);
				
				//4.replace station name and site web ready code for old site
				oldMonitoringLocation.put(TRANSACTION_TYPE, "M");
				oldMonitoringLocation.replace(SITE_WEB_READY_CODE, "L");
				oldMonitoringLocation.replace(STATION_NAME, "DEPRECATED SITE: superceded by " + newAgencyCode.trim() + "-" + newSiteNumber.trim());
				
				//5. transform old site so STATIONIX gets generated, then validate.
				LOG.trace("Transform old monitoring location");
				transformedOldMonitoringLocation = transformService.transformStationIx(oldMonitoringLocation, oldSiteReport);
				transformedOldMonitoringLocation = legacyValidatorService.doValidation(transformedOldMonitoringLocation, false, oldSiteReport);
				
				//6. update old monitoring location
				LOG.trace("update old monitoring location");
				oldJson = mlToJson(transformedOldMonitoringLocation);
				oldJson = legacyCruService.updateTransaction(transformedOldMonitoringLocation.get(AGENCY_CODE), transformedOldMonitoringLocation.get(SITE_NUMBER), oldJson, oldSiteReport);
				
				//7. add new monitoring location
				LOG.trace("Add new monitoring location");
				newJson = mlToJson(newMonitoringLocation);
				newJson = legacyCruService.addTransaction(newMonitoringLocation.get(AGENCY_CODE), newMonitoringLocation.get(SITE_NUMBER), newJson, newSiteReport);
				
				//8. export old and new sites.
				LOG.trace("Export old and new monitoring locations");
				fileExportService.exportUpdate(transformedOldMonitoringLocation.get(AGENCY_CODE).toString(), transformedOldMonitoringLocation.get(SITE_NUMBER).toString(), oldJson, oldSiteReport);
				fileExportService.exportAdd(newMonitoringLocation.get(AGENCY_CODE).toString(), newMonitoringLocation.get(SITE_NUMBER).toString(), newJson, newSiteReport);
	
				WorkflowController.addSiteReport(oldSiteReport);
				WorkflowController.addSiteReport(newSiteReport);
			}
		} catch (Exception e) {
			if(e instanceof FeignBadResponseWrapper){
				LOG.debug("An error occurred while processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ", e);
				oldSiteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));
				newSiteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, ((FeignBadResponseWrapper)e).getStatus(), false, ((FeignBadResponseWrapper)e).getBody()));

				WorkflowController.addSiteReport(oldSiteReport);
				WorkflowController.addSiteReport(newSiteReport);
			} else {
				LOG.error("An error occurred while processing primary key update transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ", e);
				oldSiteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));
				newSiteReport.addStepReport(new StepReport(PRIMARY_KEY_UPDATE_TRANSACTION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, "{\"error_message\": \"" + e.getMessage() + "\"}"));

				WorkflowController.addSiteReport(oldSiteReport);
				WorkflowController.addSiteReport(newSiteReport);
			}
		}
		LOG.trace("End processing transaction [" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber + "] ");
	}
	
	protected Map<String, Object> createNewMonitoringLocation(Map<String, Object> newMl, String newAgencyCode, String newSiteNumber) {
		newMl.put(TRANSACTION_TYPE, "PK");
		newMl.replace(AGENCY_CODE, newAgencyCode);
		newMl.replace(SITE_NUMBER, newSiteNumber);
		
		return newMl;
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
