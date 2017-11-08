package gov.usgs.wma.mlrgateway.controller;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.service.ExportWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags={"Export Workflow"})
@RestController
public class ExportWorkflowController extends BaseController {

	@Value("${temporaryNotificationEmail}")
	private String temporaryNotificationEmail;

	private ExportWorkflowService export;
	private NotificationService notificationService;
	public static final String COMPLETE_WORKFLOW = "Complete Export Workflow";
	public static final String EXPORT_WORKFLOW_SUBJECT = "MLR Transaction File for requested location";
	public static final String NOTIFICATION_STEP = "Notification";
	public static final String NOTIFICATION_SUCCESSFULL = "Notification sent successfully.";

	@Autowired
	public ExportWorkflowController(ExportWorkflowService export, NotificationService notificationService) {
		this.export = export;
		this.notificationService = notificationService;
	}

	@ApiOperation(value="Perform the entire workflow, including retrieving record from Legacy CRU and returning the Transaction file.")
	@ApiResponses(value={@ApiResponse(code=200, message="OK"),
			@ApiResponse(code=400, message="Bad Request"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Forbidden")})
	@PreAuthorize("hasPermission(null, null)")
	@PostMapping("/legacy/location/{agencyCode}/{siteNumber}")
	public GatewayReport exportWorkflow(@PathVariable("agencyCode") String agencyCode, @PathVariable("siteNumber") String siteNumber, HttpServletResponse response) {
		setReport(new GatewayReport(COMPLETE_WORKFLOW));
		try {
			export.completeWorkflow(agencyCode, siteNumber);
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				ExportWorkflowController.addStepReport(new StepReport(COMPLETE_WORKFLOW, status, ((FeignBadResponseWrapper) e).getBody(), null, null));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				ExportWorkflowController.addStepReport(new StepReport(COMPLETE_WORKFLOW, status, e.getLocalizedMessage(), null, null));
			}
		}
		
		//Send Notification
		notificationStep(EXPORT_WORKFLOW_SUBJECT);
		
		//Return Report
		GatewayReport rtn = getReport();
		response.setStatus(rtn.getStatus());
		remove();
		return rtn;
	}
	
	private int notificationStep(String subject) {
		int status = -1;
		
		//Send Notification
		try {
			notificationService.sendNotification(temporaryNotificationEmail, subject, getReport().toString());
		} catch(Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				 status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addStepReport(new StepReport(NotificationService.NOTIFICATION_STEP, status, ((FeignBadResponseWrapper) e).getBody(), null, null));
			} else {
				status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addStepReport(new StepReport(NotificationService.NOTIFICATION_STEP, status, e.getLocalizedMessage(), null, null));
			}
		}
		
		return status;
	}
}
