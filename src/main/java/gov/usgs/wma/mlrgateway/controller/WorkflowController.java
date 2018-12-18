package gov.usgs.wma.mlrgateway.controller;

import java.util.Collections;
import java.util.Comparator;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.util.UserSummaryReportBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags={"Workflow"})
@RestController
@RequestMapping("/workflows")
public class WorkflowController extends BaseController {
	private LegacyWorkflowService legacy;
	private UserSummaryReportBuilder userSummaryReportbuilder;
	public static final String COMPLETE_WORKFLOW_SUBJECT = "Submitted Ddot Transaction";
	public static final String VALIDATE_DDOT_WORKFLOW_SUBJECT = "Submitted Ddot Validation";

	@Autowired
	public WorkflowController(LegacyWorkflowService legacy, NotificationService notificationService) {
		super(notificationService);
		this.legacy = legacy;
	}

	@ApiOperation(value="Perform the entire workflow, including updating the repository and sending transaction file(s) to WSC.")
	@ApiResponses(value={@ApiResponse(code=200, message="OK"),
			@ApiResponse(code=400, message="Bad Request"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Forbidden")})
	@PreAuthorize("hasPermission(null, null)")
	@PostMapping("/ddots")
	public UserSummaryReport legacyWorkflow(@RequestPart MultipartFile file, HttpServletResponse response) {
		setReport(new GatewayReport(LegacyWorkflowService.COMPLETE_WORKFLOW, file.getName()));
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

		//Send Notification
		notificationStep(COMPLETE_WORKFLOW_SUBJECT);
		//Return report
		GatewayReport rtn = getReport();
		StepReport maxStatusStep = Collections.max(rtn.getWorkflowSteps(), Comparator.comparing(s -> s.getHttpStatus()));
		response.setStatus(maxStatusStep.getHttpStatus());
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}

	@ApiOperation(value="Validate a D dot file, DOES NOT update the repository or send transaction file(s) to WSC.")
	@ApiResponses(value={@ApiResponse(code=200, message="OK"),
	@ApiResponse(code=400, message="Bad Request"),
	@ApiResponse(code=401, message="Unauthorized")})
	@PostMapping("/ddots/validate")
	public UserSummaryReport legacyValidationWorkflow(@RequestPart MultipartFile file, HttpServletResponse response) {
		setReport(new GatewayReport(LegacyWorkflowService.VALIDATE_DDOT_WORKFLOW, file.getOriginalFilename()));
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

		//Send Notification
		notificationStep(VALIDATE_DDOT_WORKFLOW_SUBJECT);
		//Return report
		GatewayReport rtn = getReport();
		StepReport maxStatusStep = Collections.max(rtn.getWorkflowSteps(), Comparator.comparing(s -> s.getHttpStatus()));
		response.setStatus(maxStatusStep.getHttpStatus());
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}
}
