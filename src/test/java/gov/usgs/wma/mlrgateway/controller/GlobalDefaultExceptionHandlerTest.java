package gov.usgs.wma.mlrgateway.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;

public class GlobalDefaultExceptionHandlerTest {
	@Mock
	WebRequest request;

	private GlobalDefaultExceptionHandler controller = new GlobalDefaultExceptionHandler();

	@BeforeEach
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void handleUncaughtExceptionTest() throws IOException {
		HttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest servRequest = new MockHttpServletRequest();
		String expected = "Something bad happened. Contact us with Reference Number: ";
		Map<String, String> actual = controller.handleUncaughtException(new RuntimeException(), request, servRequest, response);
		assertEquals(expected, actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY).substring(0, expected.length()));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
	}

	@Test
	public void handleAccessDeniedException() throws IOException {
		HttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest servRequest = new MockHttpServletRequest();
		String expected = "You are not authorized to perform this action.";
		Map<String, String> actual = controller.handleUncaughtException(new AccessDeniedException("haha"), request, servRequest, response);
		assertEquals(expected, actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY));
		assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
	}

	@Test
	public void handleMissingServletRequestParameterException() throws IOException {
		HttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest servRequest = new MockHttpServletRequest();
		String expected = "Required String parameter 'parm' is not present";
		Map<String, String> actual = controller.handleUncaughtException(new MissingServletRequestParameterException("parm", "String"), request, servRequest, response);
		assertEquals(expected, actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY));
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
	}

	@Test
	public void handleHttpMediaTypeNotSupportedException() throws IOException {
		HttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest servRequest = new MockHttpServletRequest();
		String expected = "no way";
		Map<String, String> actual = controller.handleUncaughtException(new HttpMediaTypeNotSupportedException(expected), request, servRequest, response);
		assertEquals(expected, actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY));
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
	}
	
	@Test
	public void handleHttpMessageNotReadableException() throws IOException {
		HttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest servRequest = new MockHttpServletRequest();
		String expected = "Some123$Mes\tsage!!.";
		Map<String, String> actual = controller.handleUncaughtException(new HttpMessageNotReadableException(expected, new MockHttpInputMessage("test".getBytes())), request, servRequest, response);
		assertEquals(expected, actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY));
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
	}

	@Test
	public void handleMultilineHttpMessageNotReadableException() throws IOException {
		HttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest servRequest = new MockHttpServletRequest();
		String expected = "ok to see";
		Map<String, String> actual = controller.handleUncaughtException(new HttpMessageNotReadableException("ok to see\nhide this\nand this", new MockHttpInputMessage("test".getBytes())), request, servRequest, response);
		assertEquals(expected, actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY));
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
	}

	@Test
	public void handleOAuth2AuthorizationException() throws IOException, ServletException {
		HttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest servRequest = mock(HttpServletRequest.class);
		HttpSession session = mock(HttpSession.class);
		when(servRequest.getSession()).thenReturn(session);
		Map<String, String> actual = controller.handleUncaughtException(new ClientAuthorizationRequiredException("test-client"), request, servRequest, response);
		assertTrue(actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY).contains(GlobalDefaultExceptionHandler.LOGIN_EXPIRED_MESSAGE));
		assertFalse(actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY).contains("test-client"));
		assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
		verify(servRequest, times(1)).logout();
		verify(session, never()).invalidate();
	}

	@Test
	public void handleOAuth2AuthorizationExceptionLogoutError() throws IOException, ServletException {
		HttpServletResponse response = new MockHttpServletResponse();
		HttpServletRequest servRequest = mock(HttpServletRequest.class);
		HttpSession session = mock(HttpSession.class);
		when(servRequest.getSession()).thenReturn(session);
		doThrow(new ServletException("uh oh")).when(servRequest).logout();
		Map<String, String> actual = controller.handleUncaughtException(new ClientAuthorizationRequiredException("test-client"), request, servRequest, response);
		assertTrue(actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY).contains(GlobalDefaultExceptionHandler.LOGIN_EXPIRED_MESSAGE));
		assertFalse(actual.get(GlobalDefaultExceptionHandler.ERROR_MESSAGE_KEY).contains("test-client"));
		assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
		verify(servRequest, times(1)).logout();
		verify(session, times(1)).invalidate();
	}
}
