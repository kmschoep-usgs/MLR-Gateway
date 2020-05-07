package gov.usgs.wma.mlrgateway.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import feign.RequestInterceptor;
import gov.usgs.wma.mlrgateway.util.UserAuthUtil;

@Configuration
public class OAuth2Config {

	private static final Logger LOG = LoggerFactory.getLogger(OAuth2Config.class);

	private final Duration accessTokenExpireSkew = Duration.ofMinutes(1);

	@Autowired
	private UserAuthUtil userAuthUtil;

	@Bean
	RequestInterceptor oauth2FeignRequestInterceptor() {
		return requestTemplate -> {
			String tokenValue = userAuthUtil.getTokenValue(SecurityContextHolder.getContext().getAuthentication());

			if(tokenValue != null && !tokenValue.isEmpty()) {
				requestTemplate.header("Authorization", "Bearer " + tokenValue);
			} else {
				LOG.warn("Attempted feign client request with no valid access token.");
			}
		};
	}
	
	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
		ClientRegistrationRepository clientRegistrationRepository,
		OAuth2AuthorizedClientRepository authorizedClientRepository
	) {
		OAuth2AuthorizedClientProvider authorizedClientProvider = 
			OAuth2AuthorizedClientProviderBuilder.builder()
				.refreshToken(configurer -> configurer.clockSkew(accessTokenExpireSkew))
				.authorizationCode()
				.clientCredentials(configurer -> configurer.clockSkew(accessTokenExpireSkew))
				.password(configurer -> configurer.clockSkew(accessTokenExpireSkew))
				.build();
			
			DefaultOAuth2AuthorizedClientManager authorizedClientManager =
				new DefaultOAuth2AuthorizedClientManager(
					clientRegistrationRepository, authorizedClientRepository
				);
			authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

			return authorizedClientManager;
	}
}