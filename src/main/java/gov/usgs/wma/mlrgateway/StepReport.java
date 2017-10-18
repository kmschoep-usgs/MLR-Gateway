package gov.usgs.wma.mlrgateway;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepReport {

	private String name;
	private Integer status;
	private String details;
	private String agencyCode;
	private String siteNumber;

	public StepReport() {}

	public StepReport(String name, Integer status, String details, Object agencyCode, Object siteNumber) {
		this.name = name;
		this.status = status;
		this.details = details;
		this.agencyCode = null == agencyCode ? null : agencyCode.toString();
		this.siteNumber = null == siteNumber ? null : siteNumber.toString();
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
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
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

}
