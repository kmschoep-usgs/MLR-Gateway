package gov.usgs.wma.mlrgateway.config;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.netflix.zuul.context.RequestContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.usgs.wma.mlrgateway.util.UserAuthUtil;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ZuulOAuth2PreFilter.class)
public class ZuulOAuth2PreFilterTest {

    @Autowired
    ZuulOAuth2PreFilter filter;

    @MockBean
    UserAuthUtil userAuthUtil;

    @Test
    public void shouldFilterHappyPathTest() {
        RequestContext context = new RequestContext();
        context.set(SERVICE_ID_KEY, "mlrLegacyCru");
        RequestContext.testSetCurrentContext(context);
        assertTrue(filter.shouldFilter());

        context.set(FORWARD_TO_KEY, "");
        RequestContext.testSetCurrentContext(context);
        assertTrue(filter.shouldFilter());

        context.remove(FORWARD_TO_KEY);
        context.addZuulRequestHeader("RandomHeader", "test");
        RequestContext.testSetCurrentContext(context);
        assertTrue(filter.shouldFilter());
    }

    @Test
    public void shouldNotFilterTest() {
        RequestContext context = new RequestContext();
        RequestContext.testSetCurrentContext(context);
        assertFalse(filter.shouldFilter());

        context.set(SERVICE_ID_KEY, "legacyCru");
        RequestContext.testSetCurrentContext(context);
        assertFalse(filter.shouldFilter());

        context.set(SERVICE_ID_KEY, "");
        RequestContext.testSetCurrentContext(context);
        assertFalse(filter.shouldFilter());

        context.set(SERVICE_ID_KEY, null);
        RequestContext.testSetCurrentContext(context);
        assertFalse(filter.shouldFilter());

        context.set(SERVICE_ID_KEY, "mlrLegacyCru");
        context.set(FORWARD_TO_KEY, "test");
        RequestContext.testSetCurrentContext(context);
        assertFalse(filter.shouldFilter());
        context.remove(FORWARD_TO_KEY);

        context.addZuulRequestHeader(ZuulOAuth2PreFilter.AUTHORIZATION_HEADER, "");
        RequestContext.testSetCurrentContext(context);
        assertFalse(filter.shouldFilter());
    }

    @Test
    public void runFilterWithTokenTest() {
        when(userAuthUtil.getTokenValue(any())).thenReturn("test-token");
        RequestContext context = new RequestContext();
        context.set(SERVICE_ID_KEY, "mlrLegacyCru");
        RequestContext.testSetCurrentContext(context);
        assertNull(RequestContext.getCurrentContext().getZuulRequestHeaders().get(ZuulOAuth2PreFilter.AUTHORIZATION_HEADER));
        filter.run();
        assertEquals("Bearer test-token", RequestContext.getCurrentContext().getZuulRequestHeaders().get(ZuulOAuth2PreFilter.AUTHORIZATION_HEADER));
    }

    @Test
    public void runFilterBlankTokenTest() {
        when(userAuthUtil.getTokenValue(any())).thenReturn("");
        RequestContext context = new RequestContext();
        context.set(SERVICE_ID_KEY, "mlrLegacyCru");
        RequestContext.testSetCurrentContext(context);
        assertNull(RequestContext.getCurrentContext().getZuulRequestHeaders().get(ZuulOAuth2PreFilter.AUTHORIZATION_HEADER));
        filter.run();
        assertNull(RequestContext.getCurrentContext().getZuulRequestHeaders().get(ZuulOAuth2PreFilter.AUTHORIZATION_HEADER));
    }

    @Test
    public void runFilterNoTokenTest() {
        when(userAuthUtil.getTokenValue(any())).thenReturn(null);
        RequestContext context = new RequestContext();
        context.set(SERVICE_ID_KEY, "mlrLegacyCru");
        RequestContext.testSetCurrentContext(context);
        assertNull(RequestContext.getCurrentContext().getZuulRequestHeaders().get(ZuulOAuth2PreFilter.AUTHORIZATION_HEADER));
        filter.run();
        assertNull(RequestContext.getCurrentContext().getZuulRequestHeaders().get(ZuulOAuth2PreFilter.AUTHORIZATION_HEADER));
    }
}