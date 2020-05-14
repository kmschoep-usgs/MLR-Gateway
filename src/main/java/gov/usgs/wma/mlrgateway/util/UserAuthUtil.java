package gov.usgs.wma.mlrgateway.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UserAuthUtil {

	@Value("${security.token.claims.email:email}")
	protected String EMAIL_CLAIM_KEY;

	@Value("${security.token.claims.username:preferred_username}")
	protected String USER_NAME_CLAIM_KEY;

	protected OAuth2AuthorizedClientService authorizedClientService;
	protected OAuth2AuthorizedClientManager authorizedClientManager;

	@Autowired
	public UserAuthUtil(OAuth2AuthorizedClientService authorizedClientService, OAuth2AuthorizedClientManager authorizedClientManager) {
		this.authorizedClientManager = authorizedClientManager;
		this.authorizedClientService = authorizedClientService;
	}

	/**
	 * Validates the OAuth2 Access Token associated with the provided Authentication is valid.
	 * Throws OAuth2AuthorizationException if re-auth is needed.
	 * @param auth Authentication context to check for validitiy
	 */
	public void validateToken(Authentication auth) {
		if (auth != null && auth instanceof OAuth2AuthenticationToken) {
			getRefreshAuthorizedClient((OAuth2AuthenticationToken) auth);
		} else {
			throw new ClientAuthorizationRequiredException("Current user context is not oauth2 authenticated.");
		}
	} 

	public String getTokenValue(Authentication auth) {
		if (auth != null && auth instanceof OAuth2AuthenticationToken) {
			return getRefreshAuthorizedClient((OAuth2AuthenticationToken) auth)
				.getAccessToken().getTokenValue();
		}

		return null;
	}

	public String getUserEmail(Authentication auth) {
		return getOAuth2UserAttribute(auth, EMAIL_CLAIM_KEY);
	}

	public String getUserName(Authentication auth) {
		return getOAuth2UserAttribute(auth, USER_NAME_CLAIM_KEY);
	}

	/**
	 * Returns the String value of the requested attribute from the saved OAuth2 access token.
	 * If the attribute is not found or the authentication is invalid then null is returned.
	 * @param auth The Authentication to use for extracting the user attribute
	 * @param attr The case-sensitive attribute name to retrieve
	 * @return The extracted value if found, otherwise null
	 */
	public String getOAuth2UserAttribute(Authentication auth, String attr) {
		if (auth != null && auth instanceof OAuth2AuthenticationToken) {
			OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
			return token.getPrincipal().getAttribute(attr);
		}
		return null;
	}

	/**
	 * Returns an authorized OAuth2 client (and re-authorizes using the refresh_token if necessary)
	 * @param auth The Authentication to use for extracting and refreshing the OAuth2AuthorizedClient
	 * @return The refreshed OAuth2AuthorizedClient
	 * @throws ClientAuthorizationRequiredException if refreshing the the client failed.
	 */
	public OAuth2AuthorizedClient getRefreshAuthorizedClient(OAuth2AuthenticationToken auth) {		
		if (auth != null) {
			OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
				auth.getAuthorizedClientRegistrationId(),
				auth.getName()
			);

			// Refresh client authorization if needed
			if(client != null) {
				client = authorizedClientManager.authorize(
					OAuth2AuthorizeRequest.withAuthorizedClient(client).principal(auth).build()
				);
			}
			
			// Ensure client is authorized
			if(client == null) {
				throw new ClientAuthorizationRequiredException(auth.getAuthorizedClientRegistrationId());
			}

			return client;
		}

		return null;
	}
}