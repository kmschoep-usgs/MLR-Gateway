package gov.usgs.wma.mlrgateway.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

public class WaterAuthJwtConverter extends DefaultAccessTokenConverter  {
	public static final String EMAIL_JWT_KEY="email";
	
	@Override
	public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
		OAuth2Authentication oauth = super.extractAuthentication(map);
		OAuth2Request request = oauth.getOAuth2Request();
		Authentication auth = oauth.getUserAuthentication();
		
		if(auth != null && auth.getPrincipal() != null && auth.isAuthenticated()) {
			Map<String, Serializable> extensions = new HashMap<>();
			 
			extensions.put(EMAIL_JWT_KEY, String.valueOf(map.get(EMAIL_JWT_KEY)));
			
			OAuth2Request extendedRequest = new OAuth2Request(
				request.getRequestParameters(),
				request.getClientId(),
				request.getAuthorities(),
				request.isApproved(),
				request.getScope(),
				request.getResourceIds(),
				request.getRedirectUri(),
				request.getResponseTypes(),
				extensions
			);
			
			return new OAuth2Authentication(extendedRequest, auth);
		}
		
		return null;
	}
}