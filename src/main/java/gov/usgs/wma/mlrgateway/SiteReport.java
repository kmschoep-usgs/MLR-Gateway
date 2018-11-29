package gov.usgs.wma.mlrgateway;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteReport {

	private String agencyCode;
	private String siteNumber;
	private String transactionType;
	private boolean success;
	private List<StepReport> steps;

	

	public SiteReport() {}

	public SiteReport(String agencyCode, String siteNumber) {
		this.agencyCode = agencyCode;
		this.siteNumber = siteNumber;
		this.success = true;
		steps = new ArrayList<>();
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public String getAgencyCode() {
		return agencyCode;
	}
	public void setAgencyCode(String agencyCode) {
		this.agencyCode = agencyCode;
	}
	public String getSiteNumber() {
		return siteNumber;
	}
	public void setSiteNumber(String siteNumber) {
		this.siteNumber = siteNumber;
	}
	
	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public List<StepReport> getSteps(){
		return steps;
	}

	public void addStepReport(StepReport stepReport) {
		if (false == stepReport.isSuccess()) {
			//Steps that fail should fail the site.
			setSuccess(stepReport.isSuccess());
		}
		steps.add(stepReport);
	}

}
