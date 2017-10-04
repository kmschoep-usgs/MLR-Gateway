package gov.usgs.wma.mlrgateway.controller;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.service.LegacyWorkflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags={"Workflow"})
@RestController
@RequestMapping("/workflows")
public class WorkflowController {

	private static ThreadLocal<GatewayReport> gatewayReport = new ThreadLocal<>();
	private LegacyWorkflowService legacy;
	public static final String COMPLETE_WORKFLOW = "Complete Workflow";
	public static final String VALIDATE_DDOT_WORKFLOW = "Validate Ddot File";

	@Autowired
	public WorkflowController(LegacyWorkflowService legacy) {
		this.legacy = legacy;
	}

	public static GatewayReport getReport() {
		return gatewayReport.get();
	}

	public static void setReport(GatewayReport report) {
		gatewayReport.set(report);
	}

	public static void addStepReport(StepReport stepReport) {
		GatewayReport report = gatewayReport.get();
		report.addStepReport(stepReport);
		gatewayReport.set(report);
	}

	public static void remove() {
		gatewayReport.remove();
	}

	@ApiOperation(value="Perform the entire workflow, including updating the repository and sending transaction file(s) to WSC.")
	@ApiResponses(value={@ApiResponse(code=200, message="OK"),
			@ApiResponse(code=400, message="Bad Request"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Forbidden")})
	@PostMapping("/ddots")
	public GatewayReport legacyWorkflow(@RequestPart MultipartFile file, HttpServletResponse response) {
		setReport(new GatewayReport(COMPLETE_WORKFLOW));
		try {
			legacy.completeWorkflow(file, response);
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addStepReport(new StepReport(COMPLETE_WORKFLOW, status, ((FeignBadResponseWrapper) e).getBody(), null, null));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addStepReport(new StepReport(COMPLETE_WORKFLOW, status, e.getLocalizedMessage(), null, null));
			}
		}
		GatewayReport rtn = getReport();
		response.setStatus(rtn.getStatus());
		remove();
		return rtn;
	}

	@ApiOperation(value="Validate a D dot file, DOES NOT update the repository or send transaction file(s) to WSC.")
	@ApiResponses(value={@ApiResponse(code=200, message="OK"),
			@ApiResponse(code=400, message="Bad Request"),
			@ApiResponse(code=401, message="Unauthorized")})
	@PostMapping("/ddots/validate")
	public GatewayReport legacyValidationWorkflow(@RequestPart MultipartFile file, HttpServletResponse response) {
		setReport(new GatewayReport(VALIDATE_DDOT_WORKFLOW));
		try {
			legacy.ddotValidation(file, response);
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addStepReport(new StepReport(VALIDATE_DDOT_WORKFLOW, status, ((FeignBadResponseWrapper) e).getBody(), null, null));
				response.setStatus(status);
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addStepReport(new StepReport(VALIDATE_DDOT_WORKFLOW, status, e.getLocalizedMessage(), null, null));
				response.setStatus(status);
			}
		}
		GatewayReport rtn = getReport();
		remove();
		return rtn;
	}

}
