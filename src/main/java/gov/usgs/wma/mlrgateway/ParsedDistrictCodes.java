package gov.usgs.wma.mlrgateway;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParsedDistrictCodes {

	private List<String> districtCodes;

	public ParsedDistrictCodes() {}

	public ParsedDistrictCodes(List<String> districtCodes) {
		this.setDistrictCodes(districtCodes);
	}

	public List<String> getDistrictCodes() {
		return districtCodes;
	}

	public void setDistrictCodes(List<String> districtCodes) {
		this.districtCodes = districtCodes;
	}

	

}
