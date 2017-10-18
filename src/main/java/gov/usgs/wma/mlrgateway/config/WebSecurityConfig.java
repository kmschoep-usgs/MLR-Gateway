package gov.usgs.wma.mlrgateway.config;

import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;

import feign.RequestInterceptor;

@Configuration
@EnableOAuth2Sso
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic().disable()
			.cors().and()
			.authorizeRequests()
				.antMatchers("/swagger-resources/**", "/webjars/**", "/v2/**").permitAll()
				.antMatchers("/health/**", "/hystrix/**", "/hystrix.stream**", "/proxy.stream**", "/favicon.ico").permitAll()
				.anyRequest().fullyAuthenticated()
			.and()
				.logout().permitAll()
			.and()
				.csrf().disable()
		;
	}

	@Bean
	public RequestInterceptor oauth2FeignRequestInterceptor(OAuth2ClientContext oauth2ClientContext, OAuth2ProtectedResourceDetails resource){
		return new OAuth2FeignRequestInterceptor(oauth2ClientContext, resource);
	}


	@Bean
	public TaskScheduler taskScheduler() {
		return new ConcurrentTaskScheduler(); //single threaded by default
	}

}
