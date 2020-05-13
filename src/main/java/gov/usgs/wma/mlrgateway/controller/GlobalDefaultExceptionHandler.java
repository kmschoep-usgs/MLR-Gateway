
package gov.usgs.wma.mlrgateway.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;

@ControllerAdvice
public class GlobalDefaultExceptionHandler {
	private static final Logger LOG = LoggerFactory.getLogger(GlobalDefaultExceptionHandler.class);

	@Value("${SPRING_HTTP_MULTIPART_MAX_FILE_SIZE:1MB}")
	private String MAX_FILE_SIZE;
	
	static final String ERROR_MESSAGE_KEY = "error_message";

	@ExceptionHandler(Exception.class)
	public @ResponseBody Map<String, String> handleUncaughtException(Exception ex, WebRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
		Map<String, String> responseMap = new HashMap<>();
		if (ex instanceof AccessDeniedException) {
			servletResponse.setStatus(HttpStatus.FORBIDDEN.value());
			responseMap.put(ERROR_MESSAGE_KEY, "You are not authorized to perform this action.");
		} else if (ex instanceof MissingServletRequestParameterException
				|| ex instanceof HttpMediaTypeNotSupportedException
				|| ex instanceof HttpMediaTypeNotAcceptableException) {
			servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			responseMap.put(ERROR_MESSAGE_KEY, ex.getLocalizedMessage());
		} else if (ex instanceof HttpMessageNotReadableException) {
			servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			if (ex.getLocalizedMessage().contains("\n")) {
				//This exception's message contains implementation details after the new line, so only take up to that.
				responseMap.put(ERROR_MESSAGE_KEY, ex.getLocalizedMessage().substring(0, ex.getLocalizedMessage().indexOf("\n")));
			} else {
				responseMap.put(ERROR_MESSAGE_KEY, ex.getLocalizedMessage().replaceAll("([a-zA-Z]+\\.)+",""));
			}
		} else if (ex instanceof MultipartException && 
			(
				((MultipartException)ex).getRootCause() instanceof FileSizeLimitExceededException
						|| 
				((MultipartException)ex).getRootCause() instanceof SizeLimitExceededException
			)
		) {
			servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			responseMap.put(ERROR_MESSAGE_KEY, "The file you tried to upload exceeds the maximum allowed size for a single file ("+ MAX_FILE_SIZE + 
				"). Please split your transaction into multiple files or contact the support team for assistance.");
		} else if(ex instanceof OAuth2AuthorizationException) {
			// If we encounter an OAuth2AuthorizationException invalidate the user's session to force re-auth
			servletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
			responseMap.put(ERROR_MESSAGE_KEY, "Your login has expired. Please refresh the page to login again. If you continue " +
				"to experience this error please contact the support team for assistance.");
			try {
				servletRequest.logout();
			} catch(ServletException e) {
				servletRequest.getSession().invalidate();
			}
		} else {
			servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			int hashValue = servletResponse.hashCode();
			//Note: we are giving the user a generic message.  
			//Server logs can be used to troubleshoot problems.
			String msgText = "Something bad happened. Contact us with Reference Number: " + hashValue;
			String errorText = msgText;

			if(ex instanceof FeignBadResponseWrapper) {
				FeignBadResponseWrapper badReq = ((FeignBadResponseWrapper)ex);
				errorText += "\nFeignBadResponse:\nHTTP: " + badReq.getStatus()
					+ "\nBody: " + badReq.getBody() + "\nMessage: " + badReq.getMessage() + "\n'";
			}

			LOG.error(errorText, ex);
			
			responseMap.put(ERROR_MESSAGE_KEY, msgText);
		}
		return responseMap;
	}
}
