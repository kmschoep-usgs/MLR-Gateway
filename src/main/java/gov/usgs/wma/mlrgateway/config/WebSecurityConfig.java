package gov.usgs.wma.mlrgateway.config;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.CompositeFilter;

@Configuration
@EnableOAuth2Client
@EnableOAuth2Sso
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

//	@Value("${security.oauth2.resource.id}")
//	private String resourceId;
//
//	@Autowired
//	private ResourceServerTokenServices tokenServices;

	@Autowired
	OAuth2ClientContext oauth2ClientContext;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic().disable()
			.anonymous().disable()
			.cors().and()
			.authorizeRequests()
				.antMatchers("/swagger-resources/**", "/webjars/**", "/v2/**").permitAll()
				.antMatchers("/info**", "/health/**", "/hystrix/**", "/hystrix.stream**", "/proxy.stream**", "/favicon.ico").permitAll()
				.anyRequest().fullyAuthenticated()
			.and()
				.logout().permitAll()
			.and()
				.csrf().disable()
			.addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class)
//			.addFilterAfter(oAuth2AuthenticationProcessingFilter(), AbstractPreAuthenticatedProcessingFilter.class)
		;
	}

	private Filter ssoFilter() {
		CompositeFilter filter = new CompositeFilter();
		List<Filter> filters = new ArrayList<>();
		filters.add(ssoFilter(nwis(), "/login/nwis"));
		filter.setFilters(filters);
		return filter;
	}

	private Filter ssoFilter(ClientResources client, String path) {
		OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(path);
		OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
		filter.setRestTemplate(template);
		UserInfoTokenServices tokenServices = new UserInfoTokenServices(
				client.getResource().getUserInfoUri(), client.getClient().getClientId());
		tokenServices.setRestTemplate(template);
		filter.setTokenServices(tokenServices);
		return filter;
	}

	@Bean
	@ConfigurationProperties("nwis")
	public ClientResources nwis() {
		return new ClientResources();
	}

//	@Bean
//	public RequestInterceptor oauth2FeignRequestInterceptor(OAuth2ClientContext oauth2ClientContext, OAuth2ProtectedResourceDetails resource){
//		return new OAuth2FeignRequestInterceptor(oauth2ClientContext, resource);
//	}
//
//	@Autowired
//	public void setJwtAccessTokenConverter(JwtAccessTokenConverter jwtAccessTokenConverter) {
//		jwtAccessTokenConverter.setAccessTokenConverter(defaultAccessTokenConverter());
//	}
//
//	@Bean
//	DefaultAccessTokenConverter defaultAccessTokenConverter() {
//		return new WaterAuthJwtConverter();
//	}
//
//	@Bean
//	public TaskScheduler taskScheduler() {
//		return new ConcurrentTaskScheduler();
//	}
//
//	private OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter() {
//		OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter = new OAuth2AuthenticationProcessingFilter();
//		oAuth2AuthenticationProcessingFilter.setAuthenticationManager(oauthAuthenticationManager());
//		oAuth2AuthenticationProcessingFilter.setStateless(false);
//
//		return oAuth2AuthenticationProcessingFilter;
//	}
//
//	private AuthenticationManager oauthAuthenticationManager() {
//		OAuth2AuthenticationManager oauthAuthenticationManager = new OAuth2AuthenticationManager();
//
//		oauthAuthenticationManager.setResourceId(resourceId);
//		oauthAuthenticationManager.setTokenServices(tokenServices);
//		oauthAuthenticationManager.setClientDetailsService(null);
//
//		return oauthAuthenticationManager;
//	}

}


class ClientResources {

	@NestedConfigurationProperty
	private AuthorizationCodeResourceDetails client = new AuthorizationCodeResourceDetails();

	@NestedConfigurationProperty
	private ResourceServerProperties resource = new ResourceServerProperties();

	public AuthorizationCodeResourceDetails getClient() {
		return client;
	}

	public ResourceServerProperties getResource() {
		return resource;
	}
}

