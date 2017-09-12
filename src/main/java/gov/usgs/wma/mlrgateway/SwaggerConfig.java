package gov.usgs.wma.mlrgateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.google.common.base.Predicates;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Profile("default")
public class SwaggerConfig {

	@Bean
	public Docket gatewayApi() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select() 
				.paths(Predicates.or(PathSelectors.ant("/workflow/**"), PathSelectors.ant("/info/**"), PathSelectors.ant("/health/**")))
				.build();
	}
}
