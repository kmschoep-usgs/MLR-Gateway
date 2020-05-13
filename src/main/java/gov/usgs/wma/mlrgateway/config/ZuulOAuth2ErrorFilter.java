package gov.usgs.wma.mlrgateway.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilter;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;

import com.netflix.zuul.context.RequestContext;
/**
 * Converts a 500 zuul exception to a 401 in the case where the error was caused
 * by an OAuth2AuthorizationException.
 *
 */
@Component
public class ZuulOAuth2ErrorFilter extends SendErrorFilter {

	@Override
	public boolean shouldFilter() {
		return super.shouldFilter() && findZuulException(RequestContext.getCurrentContext().getThrowable()).getThrowable().getCause() instanceof OAuth2AuthorizationException;
	}

	@Override
	public int filterOrder() {
		return super.filterOrder() - 1;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.set(SEND_ERROR_FILTER_RAN, true);
        // Invalidate the bad session that caused the OAuth2AuthorizationException. A valid session should never cause this
		try {
            ctx.getRequest().logout(); 
        } catch(ServletException e) {
            ctx.getRequest().getSession().invalidate();
        }
		try {
			ctx.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, "Your login has expired due to inactivity. Please re-login and try the request again.");
		} catch (IOException e) {
			throw new RuntimeException("Failed to send HTTP error response from Zuul SendError filter.", e);
		}
		return null;
	}
}