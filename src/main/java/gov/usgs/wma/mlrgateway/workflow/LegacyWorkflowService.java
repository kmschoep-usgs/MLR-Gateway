package gov.usgs.wma.mlrgateway.workflow;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.DdotService;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;
import gov.usgs.wma.mlrgateway.service.LegacyValidatorService;
import gov.usgs.wma.mlrgateway.service.LegacyTransformerService;

@Service
public class LegacyWorkflowService {

	private DdotService ddotService;
	private LegacyTransformerService transformService;
	private LegacyValidatorService legacyValidatorService;
	private LegacyCruService legacyCruService;
	private FileExportService fileExportService;

	public static final String AGENCY_CODE = "agencyCode";
	public static final String SITE_NUMBER = "siteNumber";
	public static final String TRANSACTION_TYPE = "transactionType";
	public static final String TRANSACTION_TYPE_ADD = "A";
	public static final String TRANSACTION_TYPE_UPDATE = "M";

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
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);
		
		//2. Process Individual Transactions
		for (int i = 0; i < ddots.size(); i++) {
			Map<String, Object> ml = ddots.get(i);
			try {
				if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {					
					if (((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD)) {
						ml = transformService.transformStationIx(ml);
						ml = legacyValidatorService.doValidation(ml, true);
						ml = transformService.transformGeo(ml);
						json = mlToJson(ml);
						json = legacyCruService.addTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json);
						fileExportService.exportAdd(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json);
						WorkflowController.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP  + " (" + (i+1) + "/" + ddots.size() + ")", HttpStatus.SC_CREATED,COMPLETE_TRANSACTION_STEP_SUCCESS,  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
					} else {
						ml = transformService.transformStationIx(ml);
						ml = legacyValidatorService.doValidation(ml, false);
						ml = transformService.transformGeo(ml);
						json = mlToJson(ml);
						json = legacyCruService.updateTransaction(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json);
						fileExportService.exportUpdate(ml.get(AGENCY_CODE), ml.get(SITE_NUMBER), json);
						WorkflowController.addStepReport(new StepReport(COMPLETE_TRANSACTION_STEP  + " (" + (i+1) + "/" + ddots.size() + ")", HttpStatus.SC_OK,COMPLETE_TRANSACTION_STEP_SUCCESS,  ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
					}
				} else {
					WorkflowController.addStepReport(new StepReport(LegacyValidatorService.VALIDATION_STEP, HttpStatus.SC_BAD_REQUEST, BAD_TRANSACTION_TYPE, ml.get(AGENCY_CODE), ml.get(SITE_NUMBER)));
					throw new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error_message\": \"Validation failed due to a missing transaction type.\"}");
				}
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
		//1. Parse Ddot File
		List<Map<String, Object>> ddots = ddotService.parseDdot(file);
		
		//2. Process Individual Transactions
		for (int i = 0; i < ddots.size(); i++) {
			Map<String, Object> ml = ddots.get(i);
			try {
				if (ml.containsKey(TRANSACTION_TYPE) && ml.get(TRANSACTION_TYPE) instanceof String) {
					ml = transformService.transformStationIx(ml);
					if (((String) ml.get(TRANSACTION_TYPE)).contentEquals(TRANSACTION_TYPE_ADD)) {
						ml = legacyValidatorService.doValidation(ml, true);
					} else {
						ml = legacyValidatorService.doValidation(ml, false);
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
