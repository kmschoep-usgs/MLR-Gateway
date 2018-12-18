package gov.usgs.wma.mlrgateway;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryReport extends GatewayReport {

	private Integer numberSiteSuccess;
	private Integer numberSiteFailure;
	
	public UserSummaryReport() {}

	public Integer getNumberSiteSuccess() {
		return numberSiteSuccess;
	}

	public void setNumberSiteSuccess(Integer numberSiteSuccess) {
		this.numberSiteSuccess = numberSiteSuccess;
	}

	public Integer getNumberSiteFailure() {
		return numberSiteFailure;
	}

	public void setNumberSiteFailure(Integer numberSiteFailure) {
		this.numberSiteFailure = numberSiteFailure;
	}


}
