package gov.usgs.wma.mlrgateway.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.HttpSessionIdResolver;

@EnableSpringHttpSession
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${server.session.timeout:}")
	private Integer sessionTimeoutSeconds;
	
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
				.csrf().disable()
		;
	}

	@Bean
	public TaskScheduler taskScheduler() {
		return new ConcurrentTaskScheduler();
	}

	@Bean
	public MapSessionRepository sessionRepository() {
		MapSessionRepository sessionRepo = new MapSessionRepository(new ConcurrentHashMap<>());
		sessionRepo.setDefaultMaxInactiveInterval(sessionTimeoutSeconds);
		return sessionRepo;
	}

	@Bean
	public HttpSessionIdResolver httpSessionIdResolver() {
		return new HybridHttpSessionIdResolver();
	}
}