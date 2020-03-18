package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.usgs.wma.mlrgateway.config.PropagateBadRequest;

@FeignClient(name="legacyTransformer", configuration=PropagateBadRequest.class)
public interface LegacyTransformerClient {

	@RequestMapping(method=RequestMethod.POST, value="transformer/decimal_location", consumes="application/json")
	ResponseEntity<String> decimalLocation(@RequestBody String ml);

	@RequestMapping(method=RequestMethod.POST, value="transformer/station_ix", consumes="application/json")
	ResponseEntity<String> stationIx(@RequestBody String ml);

}
