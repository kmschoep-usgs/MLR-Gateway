package gov.usgs.wma.mlrgateway.config;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import gov.usgs.wma.mlrgateway.service.UserAuthService;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

@Component
public class ZuulOAuth2PreFilter extends ZuulFilter {

	public static final String MLR_SERVICE_PREFIX = "mlr";
	public static final String AUTHORIZATION_HEADER = "authorization";

	@Autowired
	private UserAuthService userAuthService;

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return !(ctx.containsKey(FORWARD_TO_KEY) && !ctx.get(FORWARD_TO_KEY).toString().isEmpty())
				&& ctx.getOrDefault(SERVICE_ID_KEY, "").toString().startsWith(MLR_SERVICE_PREFIX)
				&& !ctx.getZuulRequestHeaders().containsKey(AUTHORIZATION_HEADER);
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		String authToken = userAuthService.getTokenValue(SecurityContextHolder.getContext().getAuthentication());

		if(authToken != null && !authToken.isEmpty()) {
			ctx.addZuulRequestHeader(AUTHORIZATION_HEADER, "Bearer " + authToken);
		}
			
		return null;
	}

	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	@Override
	public int filterOrder() {
		return FilterConstants.PRE_DECORATION_FILTER_ORDER + 1;
	}
}
