package gov.usgs.wma.mlrgateway.client;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;

import feign.form.spring.SpringFormEncoder;
import feign.codec.Encoder;
import gov.usgs.wma.mlrgateway.config.PropagateBadRequest;

@FeignClient(name="ddot", configuration={DdotClient.MultipartSupportConfig.class, PropagateBadRequest.class})
public interface DdotClient {

	@PostMapping(value="ddots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<String> ingestDdot(@PathVariable(name = "file") MultipartFile file, @RequestHeader(value = "Authorization") String token);

	@PostMapping(value="ddotsOLD", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<String> ingestDdot(@PathVariable(name = "file") MultipartFile file);

	public class MultipartSupportConfig {
		@Autowired
		private ObjectFactory<HttpMessageConverters> messageConverters;

		@Bean
		public Encoder feignFormEncoder () {
			return new SpringFormEncoder(new SpringEncoder(messageConverters));
		}
	}
}
