package gov.usgs.wma.mlrgateway.controller;

import java.time.Clock;
import java.util.Collections;
import java.util.Comparator;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import gov.usgs.wma.mlrgateway.workflow.BulkTransactionFilesWorkflowService;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.util.UserAuthUtil;
import gov.usgs.wma.mlrgateway.util.UserSummaryReportBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name="Workflow", description="Display")
@RestController
@RequestMapping("/workflows")
public class BulkTransactionFilesWorkflowController extends BaseController {
	private BulkTransactionFilesWorkflowService transactionFiles;
	private UserSummaryReportBuilder userSummaryReportbuilder;
	
	public static final String BULK_GENERATE_TRANSACTION_FILES_WORKFLOW = "Submit a CSV file of agency codes and site numbers to bulk generate transaction files";
	public static final String BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_SUBJECT = "Submitted Bulk Generation Transactions file";
	private final Clock clock;
	
	@Autowired
	public BulkTransactionFilesWorkflowController(BulkTransactionFilesWorkflowService transactionFiles, NotificationService notificationService, UserAuthUtil userAuthUtil, Clock clock) {
		super(notificationService, userAuthUtil);
		this.transactionFiles = transactionFiles;
		this.clock = clock;

	}

	@Operation(description="Perform the entire bulk transaction file generation workflow, sending transaction file(s) to WSC.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@PreAuthorize("hasPermission(null, null)")
	@PostMapping("/bulk")
	public UserSummaryReport bulkGenerateTransactionFilesWorkflow(@RequestPart MultipartFile file, HttpServletResponse response, Authentication authentication) {
		setReport(new GatewayReport(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_STEP
				,file.getOriginalFilename()
				,getUserName(authentication)
				,clock.instant().toString()));
		userSummaryReportbuilder = new UserSummaryReportBuilder();
		try {
			transactionFiles.generateTransactionFilesWorkflow(file);
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				BulkTransactionFilesWorkflowController.addWorkflowStepReport(new StepReport(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				BulkTransactionFilesWorkflowController.addWorkflowStepReport(new StepReport(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED, status, false, e.getLocalizedMessage()));
			}
		}

		// Overall Status ignores Notification Status
		response.setStatus(Collections.max(getReport().getWorkflowSteps(), Comparator.comparing(s -> s.getHttpStatus())).getHttpStatus());
		
		//Send Notification
		notificationStep(BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_SUBJECT, "process-" + file.getOriginalFilename(), authentication);

		//Return report
		GatewayReport rtn = getReport();
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}
}
