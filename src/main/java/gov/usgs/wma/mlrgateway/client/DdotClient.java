package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import feign.Headers;
import gov.usgs.wma.mlrgateway.config.MultipartSupportConfig;
import gov.usgs.wma.mlrgateway.config.PropagateBadRequest;

@FeignClient(name="ddot", configuration={MultipartSupportConfig.class, PropagateBadRequest.class})
public interface DdotClient {

	@RequestMapping(method=RequestMethod.POST, value="ddots")
	@Headers("Content-Type: multipart/form-data")
	ResponseEntity<String> ingestDdot(@RequestPart MultipartFile file);

}
