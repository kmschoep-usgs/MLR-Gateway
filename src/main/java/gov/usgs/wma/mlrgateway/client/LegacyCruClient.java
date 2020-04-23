package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.openfeign.FeignClient;
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

	@RequestMapping(method=RequestMethod.POST, value="monitoringLocations/validate", consumes="application/json")
	ResponseEntity<String> validateMonitoringLocation(@RequestBody String ml);
	
	@RequestMapping(method=RequestMethod.GET, value="monitoringLocations/loggedActions")
	ResponseEntity<String> getLoggedActions(
		@RequestParam("agencyCode") String agencyCode, 
		@RequestParam("siteNumber") String siteNumber,
		@RequestParam("startDate") String startDate,
		@RequestParam("endDate") String endDate
	);
		
	@RequestMapping(method=RequestMethod.GET, value="monitoringLocations/loggedTransactions")
	ResponseEntity<String> getLoggedTransactions(
		@RequestParam("agencyCode") String agencyCode, 
		@RequestParam("siteNumber") String siteNumber,
		@RequestParam("startDate") String startDate,
		@RequestParam("endDate") String endDate,
		@RequestParam("username") String username,
		@RequestParam("action") String action,
		@RequestParam("districtCode") String districtCode
	);
		
	@RequestMapping(method=RequestMethod.GET, value="monitoringLocations/loggedTransactions/count")
	ResponseEntity<Integer> getLoggedTransactionCount(
		@RequestParam("agencyCode") String agencyCode, 
		@RequestParam("siteNumber") String siteNumber,
		@RequestParam("startDate") String startDate,
		@RequestParam("endDate") String endDate,
		@RequestParam("username") String username,
		@RequestParam("action") String action,
		@RequestParam("districtCode") String districtCode
	);
		
	@RequestMapping(method=RequestMethod.GET, value="monitoringLocations/loggedTransactions/summary")
	ResponseEntity<String> getLoggedTransactionSummary(
		@RequestParam("startDate") String startDate,
		@RequestParam("endDate") String endDate,
		@RequestParam("districtCode") String districtCode
	);
}
