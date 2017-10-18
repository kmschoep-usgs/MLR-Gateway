package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import gov.usgs.wma.mlrgateway.config.PropagateBadRequest;

@FeignClient(name="notification", configuration=PropagateBadRequest.class)
public interface NotificationClient {

	@RequestMapping(method=RequestMethod.POST, value="notification/email")
	ResponseEntity<String>  sendEmail(@RequestParam("subject") String subject, @RequestParam("message") String message, @RequestParam("recipient") String recipient);

}
