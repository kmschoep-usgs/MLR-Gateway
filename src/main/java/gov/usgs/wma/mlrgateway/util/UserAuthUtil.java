package gov.usgs.wma.mlrgateway.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UserAuthUtil {

    public static final String EMAIL_CLAIM_KEY="email";

    @Autowired
    OAuth2AuthorizedClientService clientService;

    public String getTokenValue(Authentication auth) {
        if (auth != null && auth instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(),
                token.getName()
            );
            return client.getAccessToken().getTokenValue();
        } else if(auth != null && auth instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken token = (JwtAuthenticationToken) auth;
            return token.getToken().getTokenValue();
        }

        return null;
    }

    public String getUserEmail(Authentication auth) {
        if(auth != null && auth instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken token = (JwtAuthenticationToken) auth;
            return token.getToken().containsClaim(EMAIL_CLAIM_KEY) ? 
                token.getToken().getClaimAsString(EMAIL_CLAIM_KEY) : null;
        }

        return null;
    }
}