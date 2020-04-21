package gov.usgs.wma.mlrgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic().disable()
			.cors()
			.and()
				.authorizeRequests()
					.antMatchers("/swagger-resources/**", "/webjars/**", "/v2/**").permitAll()
					.antMatchers("/info**", "/actuator/health/**", "/hystrix/**", "/hystrix.stream**", "/proxy.stream**", "/favicon.ico", "/swagger-ui.html").permitAll()
					.anyRequest().fullyAuthenticated()
			.and()
				.logout().permitAll()
			.and()
				.oauth2Login()
			.and()
				.oauth2ResourceServer().jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter()))
			.and()
				.csrf().disable()
		;
	}
	
	@Bean
	public TaskScheduler taskScheduler() {
		return new ConcurrentTaskScheduler();
	}

	private Converter<Jwt, AbstractAuthenticationToken> keycloakJwtConverter() {
		JwtAuthenticationConverter jwtAuthenticationConverter =
				new JwtAuthenticationConverter();
	
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter
				(new KeycloakJWTAuthorityMapper());
	
		return jwtAuthenticationConverter;
	}
}