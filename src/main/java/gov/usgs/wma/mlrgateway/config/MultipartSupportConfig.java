package gov.usgs.wma.mlrgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;

@Configuration
public class MultipartSupportConfig {

	@Bean
	@Primary
	@Scope("prototype")
	public Encoder feignFormEncoder() {
		return new SpringFormEncoder();
	}

}
