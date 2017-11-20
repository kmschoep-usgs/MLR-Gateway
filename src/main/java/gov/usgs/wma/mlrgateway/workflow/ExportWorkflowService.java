package gov.usgs.wma.mlrgateway.workflow;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
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
		Map<String, Object> site = legacyCruService.getMonitoringLocation(agencyCode, siteNumber);
		
		if (!site.isEmpty()) {
			try {
				json = mapper.writeValueAsString(site);
			} catch (Exception e) {
				// Unable to determine when this might actually happen, but the api says it can...
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Unable to serialize site as JSON");
			}
				
			fileExportService.exportAdd(agencyCode, siteNumber, json);
		}
	}
}
