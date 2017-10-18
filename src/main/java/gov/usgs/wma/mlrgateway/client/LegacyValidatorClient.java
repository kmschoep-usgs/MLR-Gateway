package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.usgs.wma.mlrgateway.config.PropagateBadRequest;

@FeignClient(name="legacyValidator", configuration=PropagateBadRequest.class)
public interface LegacyValidatorClient {

	@RequestMapping(method=RequestMethod.POST, value="validators", consumes="application/json")
	ResponseEntity<String> validate(@RequestBody String ml);

}
