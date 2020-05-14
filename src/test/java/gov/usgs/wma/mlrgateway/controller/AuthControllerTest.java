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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestAttributes;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.util.UserAuthUtil;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
public class AuthControllerTest extends BaseSpringTest {

	@MockBean
	private UserAuthUtil userAuthUtil;

	private AuthController controller;
    private MockHttpServletResponse response;
    private RequestAttributes attributes;
	private UsernamePasswordAuthenticationToken mockAuth = new UsernamePasswordAuthenticationToken("user", "pass");

	@BeforeEach
	public void init() {
		controller = new AuthController(userAuthUtil);
        response = new MockHttpServletResponse();
        attributes = mock(RequestAttributes.class);
	}

	@Test
	public void loginSuccessTest() throws IOException {
        given(userAuthUtil.getTokenValue(mockAuth)).willReturn("token-value");
        given(attributes.getSessionId()).willReturn("session-id");
        controller.login(mockAuth, attributes, response);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertTrue(response.getRedirectedUrl().contains("?mlrAccessToken=session-id"));
    }

	@Test
	public void loginEmptyTest() throws IOException {
        given(userAuthUtil.getTokenValue(mockAuth)).willReturn("");
        given(attributes.getSessionId()).willReturn("session-id");
        controller.login(mockAuth, attributes, response);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        given(userAuthUtil.getTokenValue(mockAuth)).willReturn(null);
        response = new MockHttpServletResponse();
        controller.login(mockAuth, attributes, response);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }
    
	@Test
	public void loginErrorTest() throws IOException {
        given(userAuthUtil.getTokenValue(mockAuth)).willThrow(new ClientAuthorizationRequiredException("test-client"));
        given(attributes.getSessionId()).willReturn("session-id");
        
        try {
            controller.login(mockAuth, attributes, response);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
		} catch(ClientAuthorizationRequiredException e) {
			assertTrue(e.getMessage().contains("test-client"));
		} catch(Exception e) {
			fail("Expected ClientAuthorizationRequiredException, but got " + e.getClass().getName());
		}
    }

    @Test
    public void getJwtSuccessTest() {
        given(userAuthUtil.getTokenValue(mockAuth)).willReturn("token-value");
        assertEquals("token-value", controller.getJwt(mockAuth));
    }

    @Test
    public void getJwtErrorTest() {
        given(userAuthUtil.getTokenValue(mockAuth)).willThrow(new ClientAuthorizationRequiredException("test-client"));
        
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
        assertEquals("session-id", controller.getToken(attributes));
    }

    @Test
    public void getTokenErrorTest() {
        given(attributes.getSessionId()).willThrow(new RuntimeException("session-error"));
        try {
            controller.getToken(attributes);
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
