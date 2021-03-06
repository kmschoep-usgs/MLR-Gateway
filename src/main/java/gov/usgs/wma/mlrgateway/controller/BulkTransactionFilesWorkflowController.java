package gov.usgs.wma.mlrgateway.controller;

import java.time.Clock;
import java.util.Collections;
import java.util.Comparator;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.workflow.BulkTransactionFilesWorkflowService;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.service.UserAuthService;
import gov.usgs.wma.mlrgateway.util.UserSummaryReportBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name="Workflow", description="Display")
@RestController
@RequestMapping("/workflows")
public class BulkTransactionFilesWorkflowController extends BaseController {
	private Logger log = LoggerFactory.getLogger(BulkTransactionFilesWorkflowController.class);
	private BulkTransactionFilesWorkflowService transactionFiles;
	private UserSummaryReportBuilder userSummaryReportbuilder;
	
	
	private final Clock clock;
	
	@Autowired
	public BulkTransactionFilesWorkflowController(BulkTransactionFilesWorkflowService transactionFiles, NotificationService notificationService, UserAuthService userAuthService, Clock clock) {
		super(notificationService, userAuthService);
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
	@PostMapping(path = "/bulkTransactionFiles", consumes = "multipart/form-data")
	public UserSummaryReport bulkGenerateTransactionFilesWorkflow(@RequestPart MultipartFile file, HttpServletResponse response, Authentication authentication) {
		userAuthService.validateToken(authentication);
		log.info("[BULK TRANSACTION WORKFLOW]: Starting bulk transaction workflow for: User: " + userAuthService.getUserName(authentication) + " | File: " + file.getOriginalFilename());
		setReport(new GatewayReport(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW
				,file.getOriginalFilename()
				,userAuthService.getUserName(authentication)
				,clock.instant().toString()));
		userSummaryReportbuilder = new UserSummaryReportBuilder();
		try {
			transactionFiles.generateTransactionFilesWorkflow(file);
			BulkTransactionFilesWorkflowController.addWorkflowStepReport(new StepReport(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW, HttpStatus.SC_OK, true, FileExportService.EXPORT_SUCCESSFULL));
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
		notificationStep(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_SUBJECT, "process-" + file.getOriginalFilename(), authentication, true);

		//Return report
		GatewayReport rtn = getReport();
		UserSummaryReport userSummaryReport = userSummaryReportbuilder.buildUserSummaryReport(rtn);
		remove();
		return userSummaryReport;
	}
}
