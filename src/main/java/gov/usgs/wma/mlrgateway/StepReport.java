package gov.usgs.wma.mlrgateway;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepReport {

	private String name;
	private Integer httpStatus;
	private boolean isSuccess;
	private String details;

	public StepReport() {}

	public StepReport(String name, Integer httpStatus, boolean isSuccess, String details) {
		this.name = name;
		this.httpStatus = httpStatus;
		this.isSuccess = isSuccess;
		this.details = details;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getHttpStatus() {
		return httpStatus;
	}
	public void setHttpStatus(Integer httpStatus) {
		this.httpStatus = httpStatus;
	}
	public boolean getIsSuccess() {
		return isSuccess;
	}

	public void setIsSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}

}
