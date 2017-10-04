package gov.usgs.wma.mlrgateway;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;

public class GatewayReport {

	private String name;
	private Integer status;
	private List<StepReport> steps;

	public GatewayReport(){}

	public GatewayReport(String name) {
		this.name = name;
		status = HttpStatus.SC_OK;
		steps = new ArrayList<>();
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public List<StepReport> getSteps() {
		return steps;
	}
	public void setSteps(List<StepReport> steps) {
		this.steps = steps;
	}
	public void addStepReport(StepReport stepReport) {
		if (399 < stepReport.getStatus()) {
			status = stepReport.getStatus();
		}
		steps.add(stepReport);
	}

}
