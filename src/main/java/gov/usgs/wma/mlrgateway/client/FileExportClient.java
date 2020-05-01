package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.usgs.wma.mlrgateway.config.PropagateBadRequest;

@FeignClient(name="fileExport", configuration=PropagateBadRequest.class)
public interface FileExportClient {

	@RequestMapping(method=RequestMethod.POST, value="file_export/add", consumes="application/json")
	ResponseEntity<String> exportAdd(@RequestBody String ml);

	@RequestMapping(method=RequestMethod.POST, value="file_export/update", consumes="application/json")
	ResponseEntity<String> exportUpdate(@RequestBody String ml);
	
	@RequestMapping(method=RequestMethod.POST, value="file_export/change", consumes="application/json")
	ResponseEntity<String> exportChange(@RequestBody String ml);

}
