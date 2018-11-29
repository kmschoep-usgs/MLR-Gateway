package gov.usgs.wma.mlrgateway.workflow;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.controller.ExportWorkflowController;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.service.FileExportService;
import gov.usgs.wma.mlrgateway.service.LegacyCruService;

@Service
public class ExportWorkflowService {

	private LegacyCruService legacyCruService;
	private FileExportService fileExportService;

	public static final String AGENCY_CODE = "agencyCode";
	public static final String SITE_NUMBER = "siteNumber";

	@Autowired
	public ExportWorkflowService(LegacyCruService legacyCruService, FileExportService fileExportService) {
		this.legacyCruService = legacyCruService;
		this.fileExportService = fileExportService;
	}

	public void exportWorkflow(String agencyCode, String siteNumber) throws HystrixBadRequestException {
		String json = "{}";
		ObjectMapper mapper = new ObjectMapper();
		SiteReport siteReport = new SiteReport(agencyCode, siteNumber);
		Map<String, Object> site = legacyCruService.getMonitoringLocation(agencyCode, siteNumber, false, siteReport);
		
		if (!site.isEmpty()) {
			try {
				json = mapper.writeValueAsString(site);
			} catch (Exception e) {
				// Unable to determine when this might actually happen, but the api says it can...
				siteReport.addStepReport(new StepReport(FileExportService.EXPORT_ADD_FAILED, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, FileExportService.EXPORT_ADD_FAILED + ": " + "Unable to serialize site as JSON"));
				WorkflowController.addSiteReport(siteReport);
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Unable to serialize site as JSON");
			}
				
			fileExportService.exportAdd(agencyCode, siteNumber, json, siteReport);
			WorkflowController.addSiteReport(siteReport);
			ExportWorkflowController.addSiteReport(siteReport);
		}
	}
}
