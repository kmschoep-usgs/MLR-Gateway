package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import feign.Headers;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;

@FeignClient(name="ddot", configuration=DdotClient.MultipartSupportConfig.class)
public interface DdotClient {

	@Configuration
	public class MultipartSupportConfig {
		@Bean
		@Primary
		@Scope("prototype")
		public Encoder feignFormEncoder() {
			return new SpringFormEncoder();
		}
	}

	@RequestMapping(method=RequestMethod.POST, value="ddots")
	@Headers("Content-Type: multipart/form-data")
	String injestDdot(@RequestPart MultipartFile file);

}
