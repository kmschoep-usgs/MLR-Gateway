package gov.usgs.wma.mlrgateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import gov.usgs.wma.mlrgateway.util.UserAuthUtil;

@Component
public class FeignTokenHeaderRequestInterceptor implements RequestInterceptor {
    private static final String AUTHORIZATION_HEADER="Authorization";
    private static final String TOKEN_TYPE = "Bearer";

    @Autowired
    UserAuthUtil userAuthUtil;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String tokenValue = userAuthUtil.getTokenValue(SecurityContextHolder.getContext().getAuthentication());

        requestTemplate.header(AUTHORIZATION_HEADER, String.format("%s %s", TOKEN_TYPE, tokenValue));
    }
}