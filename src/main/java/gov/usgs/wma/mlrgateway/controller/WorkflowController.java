package gov.usgs.wma.mlrgateway.controller;

import java.time.Clock;
import java.util.Collections;
import java.util.Comparator;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.service.UserAuthService;
import gov.usgs.wma.mlrgateway.util.UserSummaryReportBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name="Workflow", description="Display")
@RestController
@Validated
@RequestMapping("/workflows")
public class WorkflowController extends BaseController {
	private Logger log = LoggerFactory.getLogger(WorkflowController.class);
	private LegacyWorkflowService legacy;
	private UserSummaryReportBuilder userSummaryReportbuilder;
	public static final String COMPLETE_WORKFLOW_SUBJECT = "Submitted Ddot Transaction";
	public static final String VALIDATE_DDOT_WORKFLOW_SUBJECT = "Submitted Ddot Validation";
	public static final String PRIMARY_KEY_UPDATE_WORKFLOW_SUBJECT = "Submitted Primary Key Update Transaction";
	private final Clock clock;
	private Boolean enablePrimaryKeyUpdate;
	
	@Autowired
	public WorkflowController(LegacyWorkflowService legacy, NotificationService notificationService, UserAuthService userAuthService, Clock clock) {
		super(notificationService, userAuthService);
		this.legacy = legacy;
		this.clock = clock;
	}
	
	@Value("${enablePrimaryKeyUpdate:}")
	protected void setEnablePrimaryKeyUpdate(Boolean enablePrimaryKeyUpdate) {
		this.enablePrimaryKeyUpdate = enablePrimaryKeyUpdate;
	}

	@Operation(description="Perform the entire workflow, including updating the repository and sending transaction file(s) to WSC.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@PreAuthorize("hasPermission(null, null)")
	@PostMapping(path = "/ddots", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public UserSummaryReport legacyWorkflow(@RequestPart MultipartFile file, HttpServletResponse response, Authentication authentication) {
		userAuthService.validateToken(authentication);
		log.info("[VALIDATE AND UPDATE WORKFLOW]: Starting full validate and update workflow for: User: " + userAuthService.getUserName(authentication) + " | File: " + file.getOriginalFilename());
		setReport(new GatewayReport(LegacyWorkflowService.COMPLETE_WORKFLOW
				,file.getOriginalFilename()
				,userAuthService.getUserName(authentication)
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
		notificationStep(COMPLETE_WORKFLOW_SUBJECT, "process-" + file.getOriginalFilename(), authentication, true);

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
	@PostMapping(path = "/ddots/validate", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public UserSummaryReport legacyValidationWorkflow(@RequestPart MultipartFile file, HttpServletResponse response, Authentication authentication) {
		userAuthService.validateToken(authentication);
		setReport(new GatewayReport(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW
				,file.getOriginalFilename()
				,userAuthService.getUserName(authentication)
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
		notificationStep(VALIDATE_DDOT_WORKFLOW_SUBJECT, "validate-" + file.getOriginalFilename(), authentication, false);

		//Return report
		GatewayReport rtn = getReport();
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}
	
	@Operation(description="Updates primary key (agency code and/or site number) of a monitoring location.")
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
			@RequestParam @Pattern(regexp = "^[a-zA-Z0-9 ]*$", message="Invalid characters submitted in reasonText. Only alpha-numeric characters are allowed.")  @Size(max = 64) String reasonText,
			HttpServletResponse response, 
			Authentication authentication) 
	{
		if (!enablePrimaryKeyUpdate) {
			throw new UnsupportedOperationException("Feature not enabled");
		}
		userAuthService.validateToken(authentication);
		log.info("[PK CHANGE WORKFLOW]: Starting primary key change workflow for: User: " + userAuthService.getUserName(authentication) + " | Location: [" + oldAgencyCode + " - " + oldSiteNumber + "] --> [" + newAgencyCode + " - " + newSiteNumber + "]");
		setReport(new GatewayReport(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW
				,null
				,userAuthService.getUserName(authentication)
				,clock.instant().toString()));
		userSummaryReportbuilder = new UserSummaryReportBuilder();
		try {
			legacy.updatePrimaryKeyWorkflow(oldAgencyCode, oldSiteNumber, newAgencyCode, newSiteNumber, reasonText);
			WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW, HttpStatus.SC_OK, true, LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_SUCCESS));

		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addWorkflowStepReport(new StepReport(LegacyWorkflowService.PRIMARY_KEY_UPDATE_WORKFLOW_FAILED, status, false, e.getLocalizedMessage()));
			}
		}

		// Overall Status ignores Notification Status
		response.setStatus(Collections.max(getReport().getWorkflowSteps(), Comparator.comparing(s -> s.getHttpStatus())).getHttpStatus());
		
		//Send Notification
		String attachmentName = "update primary key:" + oldAgencyCode + "-" + oldSiteNumber + " to " + newAgencyCode + "-" + newSiteNumber;
		notificationStep(PRIMARY_KEY_UPDATE_WORKFLOW_SUBJECT, attachmentName, authentication, true);

		//Return report
		GatewayReport rtn = getReport();
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}
}
