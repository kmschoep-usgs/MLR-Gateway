package gov.usgs.wma.mlrgateway.controller;

import java.time.Clock;
import java.util.Collections;
import java.util.Comparator;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.workflow.UpdatePrimaryKeyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.util.UserSummaryReportBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name="Workflow", description="Display")
@RestController
@RequestMapping("/workflows")
public class WorkflowController extends BaseController {
	private LegacyWorkflowService legacy;
	private UpdatePrimaryKeyWorkflowService primaryKeyUpdate;
	private UserSummaryReportBuilder userSummaryReportbuilder;
	public static final String COMPLETE_WORKFLOW_SUBJECT = "Submitted Ddot Transaction";
	public static final String VALIDATE_DDOT_WORKFLOW_SUBJECT = "Submitted Ddot Validation";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW_SUBJECT = "Submitted Primary Key Update Transaction";
	private final Clock clock;
	
	@Autowired
	public WorkflowController(LegacyWorkflowService legacy, UpdatePrimaryKeyWorkflowService primaryKeyUpdate, NotificationService notificationService, Clock clock) {
		super(notificationService);
		this.legacy = legacy;
		this.primaryKeyUpdate = primaryKeyUpdate;
		this.clock = clock;
	}

	@Operation(description="Perform the entire workflow, including updating the repository and sending transaction file(s) to WSC.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@PreAuthorize("hasPermission(null, null)")
	@PostMapping("/ddots")
	public UserSummaryReport legacyWorkflow(@RequestPart MultipartFile file, HttpServletResponse response, OAuth2Authentication authentication) {
		setReport(new GatewayReport(LegacyWorkflowService.COMPLETE_WORKFLOW
				,file.getOriginalFilename()
				,getUserName(authentication)
				,clock.instant().toString()));
		userSummaryReportbuilder = new UserSummaryReportBuilder();
		try {
			legacy.completeWorkflow(file);
			WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.COMPLETE_WORKFLOW, HttpStatus.SC_OK, true, LegacyWorkflowService.COMPLETE_WORKFLOW_SUCCESS));
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.COMPLETE_WORKFLOW_FAILED, status, false, e.getLocalizedMessage()));
			}
		}

		// Overall Status ignores Notification Status
		response.setStatus(Collections.max(getReport().getWorkflowSteps(), Comparator.comparing(s -> s.getHttpStatus())).getHttpStatus());
		
		//Send Notification
		notificationStep(COMPLETE_WORKFLOW_SUBJECT, "process-" + file.getOriginalFilename(), authentication);

		//Return report
		GatewayReport rtn = getReport();
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}

	@Operation(description="Validate a D dot file, DOES NOT update the repository or send transaction file(s) to WSC.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@PostMapping("/ddots/validate")
	public UserSummaryReport legacyValidationWorkflow(@RequestPart MultipartFile file, HttpServletResponse response, OAuth2Authentication authentication) {
		setReport(new GatewayReport(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW
				,file.getOriginalFilename()
				,getUserName(authentication)
				,clock.instant().toString()));
		userSummaryReportbuilder = new UserSummaryReportBuilder();
		try {
			legacy.ddotValidation(file);
			WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW, HttpStatus.SC_OK, true, LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_SUCCESS));

		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW_FAILED, status, false, e.getLocalizedMessage()));
			}
		}

		// Overall Status ignores Notification Status
		response.setStatus(Collections.max(getReport().getWorkflowSteps(), Comparator.comparing(s -> s.getHttpStatus())).getHttpStatus());
		
		//Send Notification
		notificationStep(VALIDATE_DDOT_WORKFLOW_SUBJECT, "validate-" + file.getOriginalFilename(), authentication);

		//Return report
		GatewayReport rtn = getReport();
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}
	
	@Operation(description="Updates primary key by creating a new location as a copy of the old location with the new site number and sends an A and M transaction file(s) to WSC.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized") })
	@PreAuthorize("hasPermission(null, null)")
	@PostMapping("/primaryKey/update")
	public UserSummaryReport updatePrimaryKeyWorkflow(
			@RequestParam String oldAgencyCode,
			@RequestParam String newAgencyCode,
			@RequestParam String oldSiteNumber,
			@RequestParam String newSiteNumber,
			HttpServletResponse response, 
			OAuth2Authentication authentication) {
		setReport(new GatewayReport(UpdatePrimaryKeyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW
				,null
				,getUserName(authentication)
				,clock.instant().toString()));
		userSummaryReportbuilder = new UserSummaryReportBuilder();
		try {
			primaryKeyUpdate.updatePrimaryKeyWorkflow(oldAgencyCode, oldSiteNumber, newAgencyCode, newSiteNumber);
			WorkflowController.addWorkflowStepReport(new StepReport(UpdatePrimaryKeyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW, HttpStatus.SC_OK, true, UpdatePrimaryKeyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_SUCCESS));

		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addWorkflowStepReport(new StepReport(UpdatePrimaryKeyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addWorkflowStepReport(new StepReport(UpdatePrimaryKeyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED, status, false, e.getLocalizedMessage()));
			}
		}

		// Overall Status ignores Notification Status
		response.setStatus(Collections.max(getReport().getWorkflowSteps(), Comparator.comparing(s -> s.getHttpStatus())).getHttpStatus());
		
		//Send Notification
		notificationStep(PRIMARY_KEY_UPDATE_WORKFLOW_SUBJECT, "update primary key:" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber, authentication);

		//Return report
		GatewayReport rtn = getReport();
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}
}
