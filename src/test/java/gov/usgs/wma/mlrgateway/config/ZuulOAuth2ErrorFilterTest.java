package gov.usgs.wma.mlrgateway.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.usgs.wma.mlrgateway.service.UserAuthService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ZuulOAuth2ErrorFilter.class})
public class ZuulOAuth2ErrorFilterTest {

    @Autowired
    ZuulOAuth2ErrorFilter filter;

    @MockBean
    ZuulException exceptionWrapper;

    @MockBean
    UserAuthService userAuthService;

    @Test
    public void shouldFilterHappyPathTest() {
        RequestContext context = new RequestContext();
        
        ClientAuthorizationRequiredException clientAuthException = new ClientAuthorizationRequiredException("test");
        when(exceptionWrapper.getCause()).thenReturn(clientAuthException);
        context.setThrowable(exceptionWrapper);
        RequestContext.testSetCurrentContext(context);
        assertTrue(filter.shouldFilter());

        OAuth2AuthorizationException genericOAuthException = new OAuth2AuthorizationException(mock(OAuth2Error.class));
        when(exceptionWrapper.getCause()).thenReturn(genericOAuthException);
        context.setThrowable(exceptionWrapper);
        RequestContext.testSetCurrentContext(context);
        assertTrue(filter.shouldFilter());
    }

    @Test
    public void shouldNotFilterTest() {
        RequestContext context = new RequestContext();
        RequestContext.testSetCurrentContext(context);
        assertFalse(filter.shouldFilter());

        RuntimeException genericException = new RuntimeException("mock");
        when(exceptionWrapper.getCause()).thenReturn(genericException);
        context.setThrowable(exceptionWrapper);
        RequestContext.testSetCurrentContext(context);
        assertFalse(filter.shouldFilter());
    }

    @Test
    public void runFilterSuccessTest() throws IOException, ServletException {
        RequestContext context = new RequestContext();
        OAuth2AuthorizationException genericOAuthException = new OAuth2AuthorizationException(mock(OAuth2Error.class));
        when(exceptionWrapper.getCause()).thenReturn(genericOAuthException);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        context.setThrowable(exceptionWrapper);
        context.setRequest(mockRequest);
        context.setResponse(mockResponse);
        RequestContext.testSetCurrentContext(context);
        assertTrue(filter.shouldFilter());
        filter.run();
        assertFalse(filter.shouldFilter());
        verify(mockRequest, times(0)).getSession();
        verify(mockRequest, times(1)).logout();
        verify(mockResponse, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
    }

    @Test
    public void runFilterLogoutErrorTest() throws IOException, ServletException {
        RequestContext context = new RequestContext();
        OAuth2AuthorizationException genericOAuthException = new OAuth2AuthorizationException(mock(OAuth2Error.class));
        when(exceptionWrapper.getCause()).thenReturn(genericOAuthException);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        doThrow(new ServletException("test")).when(mockRequest).logout();
        when(mockRequest.getSession()).thenReturn(mock(HttpSession.class));
        context.setThrowable(exceptionWrapper);
        context.setRequest(mockRequest);
        context.setResponse(mockResponse);
        RequestContext.testSetCurrentContext(context);
        assertTrue(filter.shouldFilter());
        filter.run();
        assertFalse(filter.shouldFilter());
        verify(mockRequest, times(1)).getSession();
        verify(mockRequest, times(1)).logout();
        verify(mockResponse, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
    }

    @Test
    public void runFilterSendErrorErrorTest() throws IOException, ServletException {
        RequestContext context = new RequestContext();
        OAuth2AuthorizationException genericOAuthException = new OAuth2AuthorizationException(mock(OAuth2Error.class));
        when(exceptionWrapper.getCause()).thenReturn(genericOAuthException);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        doThrow(new IOException("test")).when(mockResponse).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        when(mockRequest.getSession()).thenReturn(mock(HttpSession.class));
        context.setThrowable(exceptionWrapper);
        context.setRequest(mockRequest);
        context.setResponse(mockResponse);
        RequestContext.testSetCurrentContext(context);
        assertTrue(filter.shouldFilter());

        try {
            filter.run();
            fail("Expected RuntimeException but got no exception.");
        } catch(RuntimeException e) {
            assertTrue(e.getMessage().contains("Failed to send HTTP error response"));
        } catch(Exception e) {
            fail("Expected RuntimeException but got " + e.getClass().getName());
        }
        
        assertFalse(filter.shouldFilter());
        verify(mockRequest, times(0)).getSession();
        verify(mockRequest, times(1)).logout();
        verify(mockResponse, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
    }
}