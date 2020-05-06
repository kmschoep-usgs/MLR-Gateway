package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.usgs.wma.mlrgateway.config.PropagateBadRequest;

@FeignClient(name="mlrLegacyValidator", configuration=PropagateBadRequest.class)
public interface LegacyValidatorClient {
	
	public static final String NEW_RECORD_PAYLOAD = "ddotLocation";
	public static final String EXISTING_RECORD_PAYLOAD = "existingLocation";
	public static final String RESPONSE_PASSED_MESSAGE = "validation_passed_message";
	public static final String RESPONSE_WARNING_MESSAGE = "warning_message";
	public static final String RESPONSE_ERROR_MESSAGE = "fatal_error_message";

	@RequestMapping(method=RequestMethod.POST, value="validators/add", consumes="application/json")
	ResponseEntity<String> validateAdd(@RequestBody String payload);
	
	@RequestMapping(method=RequestMethod.POST, value="validators/update", consumes="application/json")
	ResponseEntity<String> validateUpdate(@RequestBody String payload);

}
