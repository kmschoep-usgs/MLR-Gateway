package gov.usgs.wma.mlrgateway.controller;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.service.ExportWorkflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(tags={"Export Workflow"})
@RestController
@RequestMapping("/workflows")
public class ExportWorkflowController {

	private static ThreadLocal<GatewayReport> gatewayReport = new ThreadLocal<>();
	private ExportWorkflowService export;
	public static final String COMPLETE_WORKFLOW = "Complete Export Workflow";

	@Autowired
	public ExportWorkflowController(ExportWorkflowService export) {
		this.export = export;
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

	@ApiOperation(value="Perform the entire workflow, including retrieving record from Legacy CRU and returning the Transaction file.")
	@ApiResponses(value={@ApiResponse(code=200, message="OK"),
			@ApiResponse(code=400, message="Bad Request"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Forbidden")})
	@PostMapping("/file_export/add")
	public GatewayReport exportWorkflow(@RequestParam("agencyCode") String agencyCode, @RequestParam("siteNumber") String siteNumber, HttpServletResponse response) {
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
		GatewayReport rtn = getReport();
		response.setStatus(rtn.getStatus());
		remove();
		return rtn;
	}

}
