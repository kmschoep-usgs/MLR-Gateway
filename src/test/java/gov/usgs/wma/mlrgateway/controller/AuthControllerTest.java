package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.service.UserAuthService;

import java.io.IOException;

import javax.servlet.http.HttpSession;

@ExtendWith(SpringExtension.class)
public class AuthControllerTest extends BaseSpringTest {

	@MockBean
	private UserAuthService userAuthService;

    private AuthController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private HttpSession session;
    private ServletRequestAttributes attributes;
	private UsernamePasswordAuthenticationToken mockAuth = new UsernamePasswordAuthenticationToken("user", "pass");

	@BeforeEach
	public void init() {
		controller = new AuthController(userAuthService);
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        session = mock(HttpSession.class);
        given(session.getId()).willReturn("session-id");
        request.setSession(session);
        attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
	}

	@Test
	public void loginSuccessTest() throws IOException {
        given(userAuthService.getTokenValue(mockAuth)).willReturn("token-value");
        controller.login(mockAuth, response);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertTrue(response.getRedirectedUrl().contains("?mlrAccessToken=session-id"));
    }

	@Test
	public void loginEmptyTest() throws IOException {
        given(userAuthService.getTokenValue(mockAuth)).willReturn("");
        given(attributes.getSessionId()).willReturn("session-id");
        controller.login(mockAuth, response);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        given(userAuthService.getTokenValue(mockAuth)).willReturn(null);
        response = new MockHttpServletResponse();
        controller.login(mockAuth, response);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }
    
	@Test
	public void loginErrorTest() throws IOException {
        given(userAuthService.getTokenValue(mockAuth)).willThrow(new ClientAuthorizationRequiredException("test-client"));
        given(attributes.getSessionId()).willReturn("session-id");
        
        try {
            controller.login(mockAuth, response);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
		} catch(ClientAuthorizationRequiredException e) {
			assertTrue(e.getMessage().contains("test-client"));
		} catch(Exception e) {
			fail("Expected ClientAuthorizationRequiredException, but got " + e.getClass().getName());
		}
    }

    @Test
    public void getJwtSuccessTest() {
        given(userAuthService.getTokenValue(mockAuth)).willReturn("token-value");
        assertEquals("token-value", controller.getJwt(mockAuth));
    }

    @Test
    public void getJwtErrorTest() {
        given(userAuthService.getTokenValue(mockAuth)).willThrow(new ClientAuthorizationRequiredException("test-client"));
        
        try {
            controller.getJwt(mockAuth);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
		} catch(ClientAuthorizationRequiredException e) {
			assertTrue(e.getMessage().contains("test-client"));
		} catch(Exception e) {
			fail("Expected ClientAuthorizationRequiredException, but got " + e.getClass().getName());
		}
    }

    @Test
    public void getTokenSuccessTest() {
        given(attributes.getSessionId()).willReturn("session-id");
        assertEquals("session-id", controller.getToken());
    }

    @Test
    public void getTokenErrorTest() {
        given(attributes.getSessionId()).willThrow(new RuntimeException("session-error"));
        try {
            controller.getToken();
            fail("Expected RuntimeException but got no exception.");
		} catch(RuntimeException e) {
			assertTrue(e.getMessage().contains("session-error"));
		} catch(Exception e) {
			fail("Expected RuntimeException, but got " + e.getClass().getName());
		}
    }

    @Test
    public void reauthTest() throws IOException {
        controller.reauth(response);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals("/auth/login", response.getRedirectedUrl());
    }
}
