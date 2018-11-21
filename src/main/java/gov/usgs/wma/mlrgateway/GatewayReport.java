package gov.usgs.wma.mlrgateway;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GatewayReport {

	private String name;
	private String inputFileName;
	private StepReport workflowStep;
	private StepReport ddotIngesterStep;
	private StepReport notificationStep;
	private List<SiteReport> sites;
	
	public GatewayReport() {};

	public GatewayReport(String name, String inputFileName) {
		this.name = name;
		this.inputFileName = inputFileName;
		sites = new ArrayList<>();
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

	public StepReport getWorkflowStep() {
		return workflowStep;
	}

	public void setWorkflowStep(StepReport workflowStep) {
		this.workflowStep = workflowStep;
	}

	public StepReport getDdotIngesterStep() {
		return ddotIngesterStep;
	}

	public void setDdotIngesterStep(StepReport ddotIngesterStep) {
		this.ddotIngesterStep = ddotIngesterStep;
	}

	public StepReport getNotificationStep() {
		return notificationStep;
	}

	public void setNotificationStep(StepReport notificationStep) {
		this.notificationStep = notificationStep;
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

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "Unable to get Report";
		}
	}
	
	public String toPrettyPrintString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "Unable to get Report";
		}
	}
}
