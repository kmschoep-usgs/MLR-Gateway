package gov.usgs.wma.mlrgateway.config;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.WebJarsResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	
	@Value("${ui.host}")
	private String uiDomainName;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins(uiDomainName).allowCredentials(true).allowedMethods("GET", "POST","PUT", "DELETE","PATCH");
    }
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**")
				.addResourceLocations("/resources/", "/webjars/")
				.setCacheControl(
						CacheControl.maxAge(30L, TimeUnit.DAYS).cachePublic())
				.resourceChain(true)
				.addResolver(new WebJarsResourceResolver());
	}
	
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}