package gov.usgs.wma.mlrgateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${mlrServicePassword}")
	private String pwd;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.cors().and()
			.authorizeRequests()
				.antMatchers("/workflows/**").permitAll()
				.antMatchers("/legacy/**").permitAll()
				.antMatchers("/validations/**").permitAll()
				.antMatchers("/monitoringLocations/**").permitAll()
				.antMatchers("/swagger-ui.html", "/swagger-resources/**", "/webjars/**", "/v2/**").permitAll()
				.antMatchers("/health/**", "/hystrix/**", "/hystrix.stream**", "/proxy.stream**", "/favicon.ico").permitAll()
				.anyRequest().fullyAuthenticated()
			.and()
				.formLogin().defaultSuccessUrl("/swagger-ui.html", true)
			.and()
				.logout().logoutSuccessUrl("/swagger-ui.html")
			.and()
				.formLogin().permitAll()
			.and()
				.logout().permitAll()
			.and()
				.csrf().disable()
		;
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth
		.inMemoryAuthentication()
		.withUser("user").password(pwd).roles("ACTUATOR");
	}

}
