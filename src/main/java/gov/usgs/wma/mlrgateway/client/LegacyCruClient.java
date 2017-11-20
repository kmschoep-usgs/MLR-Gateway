package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="legacyCru", decode404=true)
public interface LegacyCruClient {

	@RequestMapping(method=RequestMethod.POST, value="monitoringLocations", consumes="application/json")
	ResponseEntity<String> createMonitoringLocation(@RequestBody String ml);

	@RequestMapping(method=RequestMethod.PUT, value="monitoringLocations/{id}", consumes="application/json")
	ResponseEntity<String> updateMonitoringLocation(@PathVariable("id") String id, @RequestBody String ml);

	@RequestMapping(method=RequestMethod.PATCH, value="monitoringLocations", consumes="application/json")
	ResponseEntity<String> patchMonitoringLocation(@RequestBody String ml);
	
	@RequestMapping(method=RequestMethod.GET, value="monitoringLocations", consumes="application/json")
	ResponseEntity<String> getMonitoringLocation(@RequestParam("agencyCode") String agencyCode, @RequestParam("siteNumber") String siteNumber);

}
