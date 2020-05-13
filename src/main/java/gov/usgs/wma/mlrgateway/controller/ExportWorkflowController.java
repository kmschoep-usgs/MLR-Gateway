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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.workflow.ExportWorkflowService;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.util.UserAuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name="Export Workflow", description="Display")
@RestController
public class ExportWorkflowController extends BaseController {
	private Logger log = LoggerFactory.getLogger(ExportWorkflowController.class);
	private ExportWorkflowService export;
	public static final String COMPLETE_WORKFLOW = "Complete Export Workflow";
	public static final String EXPORT_WORKFLOW_SUBJECT = "Transaction File Generation for Requested Location";
	private final Clock clock;
	
	@Autowired
	public ExportWorkflowController(ExportWorkflowService export, NotificationService notificationService, UserAuthUtil userAuthUtil, Clock clock) {
		super(notificationService, userAuthUtil);
		this.export = export;
		this.clock = clock;
	}

	@Operation(description="Perform the entire workflow, including retrieving record from Legacy CRU and returning the Transaction file.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@PreAuthorize("hasPermission(null, null)")
	@PostMapping("/legacy/location/{agencyCode}/{siteNumber}")
	public GatewayReport exportWorkflow(@PathVariable("agencyCode") String agencyCode, @PathVariable("siteNumber") String siteNumber, HttpServletResponse response, Authentication authentication) {
		userAuthUtil.validateToken(authentication);
		log.info("[COPY WORKFLOW]: Starting copy to NWIS Hosts workflow for: User: " + userAuthUtil.getUserName(authentication) + " | Location: [" + agencyCode + " - " + siteNumber + "]");
		setReport(new GatewayReport(COMPLETE_WORKFLOW
				,null
				,userAuthUtil.getUserName(authentication)
				,clock.instant().toString()));
		try {
			export.exportWorkflow(agencyCode, siteNumber);
			ExportWorkflowController.addWorkflowStepReport(new StepReport(COMPLETE_WORKFLOW, HttpStatus.SC_OK, true, FileExportService.EXPORT_SUCCESSFULL));
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				ExportWorkflowController.addWorkflowStepReport(new StepReport(COMPLETE_WORKFLOW, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				ExportWorkflowController.addWorkflowStepReport(new StepReport(COMPLETE_WORKFLOW, status, false,  e.getMessage()));
			}
		}

		// Overall Status ignores Notification Status
		response.setStatus(Collections.max(getReport().getWorkflowSteps(), Comparator.comparing(s -> s.getHttpStatus())).getHttpStatus());
		
		//Send Notification
		notificationStep(EXPORT_WORKFLOW_SUBJECT, "export-" + agencyCode + "-" + siteNumber, authentication, false);
		
		//Return Report
		GatewayReport rtn = getReport();
		
		remove();
		return rtn;
	}	
}
