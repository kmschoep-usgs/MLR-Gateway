package gov.usgs.wma.mlrgateway.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
public class UserAuthUtil {

    @Value("${security.token.claims.email:email}")
    protected String EMAIL_CLAIM_KEY;

    @Value("${security.token.claims.username:preferred_username}")
    protected String USER_NAME_CLAIM_KEY;

    @Autowired
    OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    OAuth2AuthorizedClientManager authorizedClientManager;
    

    public String getTokenValue(Authentication auth) {
        OAuth2AccessToken token = getRefreshAccessToken(auth);
        return token != null ? token.getTokenValue() : null;
    }

    public String getUserEmail(Authentication auth) {
        if (auth != null && auth instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
            return token.getPrincipal().getAttribute(EMAIL_CLAIM_KEY);
        }
        return null;
    }

    public String getUserName(Authentication auth) {
        if (auth != null && auth instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
            return token.getPrincipal().getAttribute(USER_NAME_CLAIM_KEY);
        }
        return null;
    }

    /**
     * Returns the OAuth2AccessToken from the provided Authentication. If the token is expired
     * (taking into account configured time skew) a new token is requested from the client
     * using the stored refresh token (if one exists). If a new token cannot be retrieved
     * then `null` is returned.
     * @param auth The Authentication to use for extracting and refreshing the token
     * @return The valid OAuth2AccessToken (or null if refresh failed)
     */
    public OAuth2AccessToken getRefreshAccessToken(Authentication auth) {
        if (auth != null && auth instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(),
                token.getName()
            );
            OAuth2AccessToken accessToken = authorizedClientManager.authorize(OAuth2AuthorizeRequest
                .withAuthorizedClient(client).principal(token).build()).getAccessToken();
            return accessToken;
        }

        return null;
    }
}