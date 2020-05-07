package gov.usgs.wma.mlrgateway.config;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.SameSiteCookies;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer.CookieValue;

/**
 * A Hybrid of Both HeaderHttpSessionIdResolver and CookieHttpSessionIdResolver
 * that allows the use of X-Auth-Token headers (which take precedence) OR a SESSION
 * cookie for session ID retrieval. This allows Spring Security OAuth2 to successfully
 * complete an Authorization Code flow (which requires a SESSION cookie) and clients
 * accessing this Gateway to use the X-Auth-Token header.
 * 
 * The default CookieSerializer in this class is setup to use SameSite=Strict for cookies
 * which encourages the use of X-Auth-Token instead of the session cookie except for
 * first-party interactions such as OAuth2 login.
 * 
 * @see org.springframework.session.web.http.HeaderHttpSessionIdResolver
 * @see org.springframework.session.web.http.CookieHttpSessionIdResolver
 */
public class HybridHttpSessionIdResolver implements HttpSessionIdResolver {

	private static final String HEADER_X_AUTH_TOKEN = "X-Auth-Token";

	private static final String WRITTEN_SESSION_ID_ATTR = CookieHttpSessionIdResolver.class.getName()
		.concat(".WRITTEN_SESSION_ID_ATTR");

	private CookieSerializer cookieSerializer;

	private final String headerName;

	public HybridHttpSessionIdResolver() {
		this.headerName = HEADER_X_AUTH_TOKEN;
		
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("SESSION"); 
		serializer.setCookiePath("/");
		serializer.setUseSecureCookie(true);
		serializer.setSameSite(SameSiteCookies.LAX.getValue());
		this.cookieSerializer = serializer;
	}

	@Override
	public List<String> resolveSessionIds(HttpServletRequest request) {
		String headerValue = request.getHeader(this.headerName);
		List<String> sessionIds = (headerValue != null) ? Collections.singletonList(headerValue) : Collections.emptyList();
		
		if(sessionIds.isEmpty()) {
			sessionIds =  this.cookieSerializer.readCookieValues(request);
		}

		return sessionIds;
	}

	@Override
	public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
		response.setHeader(this.headerName, sessionId);

		if (!sessionId.equals(request.getAttribute(WRITTEN_SESSION_ID_ATTR))) {
			request.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
			this.cookieSerializer.writeCookieValue(new CookieValue(request, response, sessionId));
		}
	}

	@Override
	public void expireSession(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader(this.headerName, "");
		this.cookieSerializer.writeCookieValue(new CookieValue(request, response, ""));
	}

	/**
	* Sets the {@link CookieSerializer} to be used.
	* @param cookieSerializer the cookieSerializer to set. Cannot be null.
	*/
	public void setCookieSerializer(CookieSerializer cookieSerializer) {
		if (cookieSerializer == null) {
			throw new IllegalArgumentException("cookieSerializer cannot be null");
		}
		this.cookieSerializer = cookieSerializer;
	}

}