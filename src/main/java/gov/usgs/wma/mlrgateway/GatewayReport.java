package gov.usgs.wma.mlrgateway;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GatewayReport {

	private String name;
	private String inputFileName;
	private String reportDateTime;
	private String userName;
	private Logger log = LoggerFactory.getLogger(GatewayReport.class);
	private List<StepReport> workflowSteps;
	private List<SiteReport> sites;
	
	public GatewayReport() {};

	public GatewayReport(String name, String inputFileName) {
		this.name = name;
		this.inputFileName = inputFileName;
		sites = new ArrayList<>();
		workflowSteps = new ArrayList<>();
	}
	
	public GatewayReport(GatewayReport gatewayReport) {
		this.name = gatewayReport.name;
		this.inputFileName = gatewayReport.inputFileName;
		this.reportDateTime = gatewayReport.reportDateTime;
		this.userName = gatewayReport.userName;
		this.workflowSteps = gatewayReport.getWorkflowSteps().stream().map(step -> new StepReport(step)).collect(Collectors.toList());
		this.sites = gatewayReport.getSites().stream().map(site -> new SiteReport(site)).collect(Collectors.toList());
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

	public String getReportDateTime() {
		return reportDateTime;
	}

	public void setReportDateTime(String reportDateTime) {
		this.reportDateTime = reportDateTime;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public List<SiteReport> getSites() {
		return sites;
	}

	public void setSites(List<SiteReport> sites) {
		this.sites = sites;
	}
	
	public void addSiteReport(SiteReport siteReport) {
		sites.add(siteReport);
	}
	
	public List<StepReport> getWorkflowSteps() {
		return workflowSteps;
	}

	public void setWorkflowSteps(List<StepReport> workflowSteps) {
		this.workflowSteps = workflowSteps;
	}
	
	public void addWorkflowStepReport(StepReport stepReport) {
		workflowSteps.add(stepReport);
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			log.error("An error occured while trying to cprint the report string: ", e);
			return "Unable to get Report";
		}
	}
	
	public String toPrettyPrintString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			log.error("An error occured while trying to cprint the report string: ", e);
			return "Unable to get Report";
		}
	}
}
