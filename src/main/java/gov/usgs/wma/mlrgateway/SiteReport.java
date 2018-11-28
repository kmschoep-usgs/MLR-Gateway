package gov.usgs.wma.mlrgateway;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteReport {

	private boolean isSuccess;
	private String agencyCode;
	private String siteNumber;
	private String transactionType;
	private List<StepReport> steps;

	

	public SiteReport() {}

	public SiteReport(String agencyCode, String siteNumber) {
		this.agencyCode = null == agencyCode ? null : agencyCode;
		this.siteNumber = null == siteNumber ? null : siteNumber;
		this.isSuccess = true;
		steps = new ArrayList<>();
	}

	public boolean getIsSuccess() {
		return isSuccess;
	}

	public void setIsSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
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
		if (false == stepReport.getIsSuccess()) {
			//Only HttpStatus code of 400 and higher should override the current status.
			setIsSuccess(stepReport.getIsSuccess());
		}
		steps.add(stepReport);
	}

}
