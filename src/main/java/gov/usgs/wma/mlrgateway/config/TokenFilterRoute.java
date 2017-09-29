package gov.usgs.wma.mlrgateway.config;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

@Component
public class TokenFilterRoute extends ZuulFilter {

	@Override
	public String filterType() {
		return "route";
	}

	@Override
	public int filterOrder() {
		return 0;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		if (ctx.getRequest().getHeader(HttpHeaders.AUTHORIZATION) == null) {
			addTokenToHeader(ctx);
		}
		return null;
	}

	private void addTokenToHeader(RequestContext ctx) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication instanceof OAuth2Authentication) {
			ctx.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, buildBearerToken(getToken((OAuth2Authentication) authentication)));
		}
	}

	private String getToken(OAuth2Authentication authentication) {
		return ((OAuth2AuthenticationDetails) authentication.getDetails()).getTokenValue();
	}

//	private RefreshableKeycloakSecurityContext getRefreshableKeycloakSecurityContext(RequestContext ctx) {
//		if (ctx.getRequest().getUserPrincipal() instanceof KeycloakAuthenticationToken) {
//			KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) ctx.getRequest().getUserPrincipal();
//			return (RefreshableKeycloakSecurityContext) token.getCredentials();
//		}
//		return null;
//	}
//
	private String buildBearerToken(String token) {
		return "Bearer " + token;
	}


}
