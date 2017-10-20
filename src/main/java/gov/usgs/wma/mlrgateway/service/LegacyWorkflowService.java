package gov.usgs.wma.mlrgateway.service;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.FileExportClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;

@Service
public class LegacyWorkflowService {

	private DdotService ddotService;
	private LegacyCruClient legacyCruClient;
	private TransformService transformService;
	private LegacyValidatorService legacyValidatorService;
	private FileExportClient fileExportClient;

	public static final String AGENCY_CODE = "agencyCode";
	public static final String SITE_NUMBER = "siteNumber";
	public static final String TRANSACTION_TYPE = "transactionType";
	public static final String TRANSACTION_TYPE_ADD = "A";
	public static final String TRANSACTION_TYPE_UPDATE = "M";
	public static final String SITE_ADD_STEP = "Site Add";
	public static final String SITE_ADD_SUCCESSFULL = "Site Added Successfully";
	public static final String SITE_ADD_FAILED = "Site add failed";
	public static final String SITE_UPDATE_STEP = "Site Update";
	public static final String SITE_UPDATE_SUCCESSFULL = "Site Updated Successfully.";
	public static final String SITE_UPDATE_FAILED = "Site update failed";
	public static final String EXPORT_ADD_STEP = "Export Add Transaction File";
	public static final String EXPORT_UPDATE_STEP = "Export Update Transaction File";
	public static final String EXPORT_SUCCESSFULL = "Transaction File created Successfully.";
	public static final String EXPORT_ADD_FAILED = "Export add failed";
	public static final String EXPORT_UPDATE_FAILED = "Export update failed";
	public static final String BAD_TRANSACTION_TYPE = "Unable to determine transaction type.";
	public static final String COMPLETE_WORKFLOW = "Complete Workflow";
	public static final String COMPLETE_WORKFLOW_FAILED = "Complete workflow failed";
	public static final String VALIDATE_DDOT_WORKFLOW = "Validate D dot File";
	public static final String VALIDATE_DDOT_TRANSACTION_STEP = "Validate Single D dot Transaction";
	public static final String VALIDATE_DDOT_TRANSACTION_STEP_SUCCESS = "Single transaction validation passed.";
	public static final String VALIDATE_DDOT_TRANSACTION_STEP_FAILURE = "Single transaction validation failed.";
	public static final String COMPLETE_TRANSACTION_STEP = "Process Single D dot Transaction";
	public static final String COMPLETE_TRANSACTION_STEP_SUCCESS = "D dot Transaction Processed";

	@Autowired
	public LegacyWorkflowService(DdotService ddotService, LegacyCruClient legacyCruClient, TransformService transformService, 
			LegacyValidatorService legacyValidatorService, FileExportClient fileExportClient) {
		this.ddotService = ddotService;
		this.legacyCruClient = legacyCruClient;
		this.transformService = transformService;
		this.legacyValidatorService = legacyValidatorService;
		this.fileExportClient = fileExportClient;
	}

	public void completeWorkflow(MultipartFile file) throws HystrixBadRequestException {
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);
		
		String json = "{}";
		for (int i = 0; i < ddots.size(); i++) {
			Map<String, Object> ml = ddots.get(i);
			try {
				if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
					if (((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD)) {
						json = validateAndTransform(ml, true);
						addTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json);
					} else {
						json = validateAndTransform(ml, false);
						updateTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json);
					}
				} else {
					WorkflowController.addStepReport(new StepReport(LegacyValidatorService.VALIDATION_STEP, HttpStatus.SC_BAD_REQUEST, BAD_TRANSACTION_TYPE, ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
					throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
				}
				
				WorkflowController.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP  + " (" + (i+1) + "/" + ddots.size() + ")", HttpStatus.SC_OK,COMPLETE_TRANSACTION_STEP_SUCCESS,  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
			} catch (Exception e) {
				if(e instanceof FeignBadResponseWrapper){
					WorkflowController.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP  + " (" + (i+1) + "/" + ddots.size() + ")", ((FeignBadResponseWrapper)e).getStatus(), ((FeignBadResponseWrapper)e).getBody(),  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
				} else {
					WorkflowController.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP  + " (" + (i+1) + "/" + ddots.size() + ")", HttpStatus.SC_INTERNAL_SERVER_ERROR, "{\"error_message\": \"" + e.getMessage() + "\"}",  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
				}
			}
		}
	}

	public void ddotValidation(MultipartFile file) throws HystrixBadRequestException {
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);
		String json = "{}";
		
		for (int i = 0; i < ddots.size(); i++) {
			Map<String, Object> ml = ddots.get(i);
			try {
				if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
					if (((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD)) {
						json = validateAndTransform(ml, true);
					} else {
						json = validateAndTransform(ml, false);
					}
				} else {
					WorkflowController.addStepReport(new StepReport(LegacyValidatorService.VALIDATION_STEP, HttpStatus.SC_BAD_REQUEST, BAD_TRANSACTION_TYPE, ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
					throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
				}		
				
				WorkflowController.addStepReport(new StepReport(VALIDATE_DDOT_TRANSACTION_STEP  + " (" + (i+1) + "/" + ddots.size() + ")", HttpStatus.SC_OK,VALIDATE_DDOT_TRANSACTION_STEP_SUCCESS,  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
			} catch (Exception e) {
				if(e instanceof FeignBadResponseWrapper){
					WorkflowController.addStepReport(new StepReport(VALIDATE_DDOT_TRANSACTION_STEP  + " (" + (i+1) + "/" + ddots.size() + ")", ((FeignBadResponseWrapper)e).getStatus(), ((FeignBadResponseWrapper)e).getBody(),  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
				} else {
					WorkflowController.addStepReport(new StepReport(VALIDATE_DDOT_TRANSACTION_STEP  + " (" + (i+1) + "/" + ddots.size() + ")", HttpStatus.SC_INTERNAL_SERVER_ERROR, "{\"error_message\": \"" + e.getMessage() + "\"}",  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
				}
			}
		}
	}

	protected String validateAndTransform(Map<String, Object> ml, boolean isAddTransaction) {
		//Note that this does not inspect the input for cross-field dependencies (like latitude, longitude, and coordinateDatumCode) We will probably need to 
		//handle the case where one, but not all of the fields are present in the update. 
		ObjectMapper mapper = new ObjectMapper();
		String json = "";
				
		ml = legacyValidatorService.doValidation(ml, isAddTransaction);
		
		//Ensure validation occurred before continuing
		if(ml.containsKey("validation") && !ml.get("validation").toString().contains(LegacyValidatorService.VALIDATION_FAILED)){
			ml = transformService.transform(ml);
			
			try {
				json = mapper.writeValueAsString(ml);
			} catch (Exception e) {
				// Unable to determine when this might actually happen, but the api says it can...
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize transformer output.\"}");
			}
		} else {
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"An unkown error occurred during validation.\"}");
		}

		return json;
	}

	protected void addTransaction(Object agencyCode, Object siteNumber, String json) {
		//catch bad adds and exports
		try {
			ResponseEntity<String> cruResp = legacyCruClient.createMonitoringLocation(json);
			int cruStatus = cruResp.getStatusCodeValue();
			WorkflowController.addStepReport(new StepReport(SITE_ADD_STEP, cruStatus, 201 == cruStatus ? SITE_ADD_SUCCESSFULL : cruResp.getBody(), agencyCode, siteNumber));
			exportAdd(agencyCode, siteNumber, cruResp.getBody());
		} catch (Exception e) {
			WorkflowController.addStepReport(new StepReport(SITE_ADD_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, SITE_ADD_FAILED,  agencyCode, siteNumber));
		}

	}

	protected void exportAdd(Object agencyCode, Object siteNumber, String json) {
		try {
			ResponseEntity<String> exportResp = fileExportClient.exportAdd(json);
			int exportStatus = exportResp.getStatusCodeValue();
			WorkflowController.addStepReport(new StepReport(EXPORT_ADD_STEP, exportStatus, 200 == exportStatus ? EXPORT_SUCCESSFULL : exportResp.getBody(), agencyCode, siteNumber));
		} catch (Exception e) {
			WorkflowController.addStepReport(new StepReport(EXPORT_ADD_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, EXPORT_ADD_FAILED,  agencyCode, siteNumber));
		}
	}
	
	protected void updateTransaction(Object agencyCode, Object siteNumber, String json) {
		//catch bad updates and exports (new stepreport)
		try {
			ResponseEntity<String> cruResp = legacyCruClient.patchMonitoringLocation(json);
			int cruStatus = cruResp.getStatusCodeValue();
			WorkflowController.addStepReport(new StepReport(SITE_UPDATE_STEP, cruStatus, 200 == cruStatus ? SITE_UPDATE_SUCCESSFULL : cruResp.getBody(), agencyCode, siteNumber));
			exportUpdate(agencyCode, siteNumber, cruResp.getBody());
		} catch (Exception e){
			WorkflowController.addStepReport(new StepReport(SITE_UPDATE_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, SITE_UPDATE_FAILED,  agencyCode, siteNumber));

		}


	}

	protected void exportUpdate(Object agencyCode, Object siteNumber, String json) {
		try {
			ResponseEntity<String> exportResp = fileExportClient.exportUpdate(json);
			int exportStatus = exportResp.getStatusCodeValue();
			WorkflowController.addStepReport(new StepReport(EXPORT_UPDATE_STEP, exportStatus, 200 == exportStatus ? EXPORT_SUCCESSFULL : exportResp.getBody(), agencyCode, siteNumber));
		} catch (Exception e) {
			WorkflowController.addStepReport(new StepReport(EXPORT_UPDATE_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, EXPORT_UPDATE_FAILED,  agencyCode, siteNumber));

		}
	}
}
