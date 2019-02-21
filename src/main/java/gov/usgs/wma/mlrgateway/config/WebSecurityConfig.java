package gov.usgs.wma.mlrgateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import feign.RequestInterceptor;

@Configuration
@EnableOAuth2Sso
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${security.oauth2.resource.id}")
	private String resourceId;

	@Autowired
	private ResourceServerTokenServices tokenServices;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic().disable()
			.cors().and()
			.authorizeRequests()
				.antMatchers("/swagger-resources/**", "/webjars/**", "/v2/**").permitAll()
				.antMatchers("/info**", "/health/**", "/hystrix/**", "/hystrix.stream**", "/proxy.stream**", "/favicon.ico", "/swagger-ui.html").permitAll()
				.anyRequest().fullyAuthenticated()
			.and()
				.logout().permitAll()
			.and()
				.csrf().disable()
			.addFilterAfter(oAuth2AuthenticationProcessingFilter(), AbstractPreAuthenticatedProcessingFilter.class);

		;
	}

	@Bean
	public RequestInterceptor oauth2FeignRequestInterceptor(OAuth2ClientContext oauth2ClientContext, OAuth2ProtectedResourceDetails resource){
		return new OAuth2FeignRequestInterceptor(oauth2ClientContext, resource);
	}

	@Autowired
	public void setJwtAccessTokenConverter(JwtAccessTokenConverter jwtAccessTokenConverter) {
		jwtAccessTokenConverter.setAccessTokenConverter(defaultAccessTokenConverter());
	}

	@Bean
	DefaultAccessTokenConverter defaultAccessTokenConverter() {
		return new WaterAuthJwtConverter();
	}

	@Bean
	public TaskScheduler taskScheduler() {
		return new ConcurrentTaskScheduler();
	}

	private OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter() {
		OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter = new OAuth2AuthenticationProcessingFilter();
		oAuth2AuthenticationProcessingFilter.setAuthenticationManager(oauthAuthenticationManager());
		oAuth2AuthenticationProcessingFilter.setStateless(false);

		return oAuth2AuthenticationProcessingFilter;
	}

	private AuthenticationManager oauthAuthenticationManager() {
		OAuth2AuthenticationManager oauthAuthenticationManager = new OAuth2AuthenticationManager();

		oauthAuthenticationManager.setResourceId(resourceId);
		oauthAuthenticationManager.setTokenServices(tokenServices);
		oauthAuthenticationManager.setClientDetailsService(null);

		return oauthAuthenticationManager;
	}

}
