package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.exception.InvalidEmailException;
import gov.usgs.wma.mlrgateway.service.AdminService;
import gov.usgs.wma.mlrgateway.service.UserAuthService;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SuppressWarnings("unchecked")
public class UtilControllerTest extends BaseSpringTest {

	private final String SERVICE_AUTH_TOKEN_VAL = "changeMe";

	@MockBean
	private AdminService adminService;

	@MockBean
	private UserAuthService authService;

	private UtilController controller;
	private MockHttpServletResponse response;
	private MockHttpServletRequest request;

	@BeforeEach
	public void init() {
		controller = new UtilController(adminService, authService);
		controller.setServiceAuthToken(SERVICE_AUTH_TOKEN_VAL);
		response = new MockHttpServletResponse();
		request = new MockHttpServletRequest();
	}

	@Test
	public void sendSummaryEmailHappyPath() {
		List<String> recipients = new ArrayList<>();
		recipients.add("test");

		try {
			controller.sendSummaryEmail("2020-01-01", recipients, SERVICE_AUTH_TOKEN_VAL, request, response);
		} catch(Exception e) {
			fail("Expected no Exceptions, but got " + e.getClass().getName());
		}
	}

	@Test
	public void invalidEmailExceptionTest() throws Exception {
		String errorMessage = "Invalid email";
		List<String> recipients = new ArrayList<>();
		recipients.add("test");

		willThrow(new InvalidEmailException(errorMessage)).given(adminService).sendSummaryEmail(eq("2020-01-01"), any(List.class), any());
		
		try {
			controller.sendSummaryEmail("2020-01-01", recipients, SERVICE_AUTH_TOKEN_VAL, request, response);
			fail("Expected InvalidEmailException but got no exception.");
		} catch(InvalidEmailException e) {
			assertEquals(errorMessage, e.getMessage());
		} catch(Exception e) {
			fail("Expected InvalidEmailException but got " + e.getClass().getName());
		}
	}

	@Test
	public void badBackingServiceRequest() throws Exception {
		String badText = "This is really bad.";
		List<String> recipients = new ArrayList<>();
		recipients.add("test");

		willThrow(new FeignBadResponseWrapper(500, null, badText)).given(adminService).sendSummaryEmail(eq("2020-01-01"), any(List.class), any());
		
		try {
			controller.sendSummaryEmail("2020-01-01", recipients, SERVICE_AUTH_TOKEN_VAL, request, response);
			fail("Expected FeignBadResponseWrapper but got no exception.");
		} catch(FeignBadResponseWrapper e) {
			assertEquals(500, e.getStatus());
		} catch(Exception e) {
			fail("Expected FeignBadResponseWrapper, but got " + e.getClass().getName());
		}
	}

	@Test
	public void invalidServiceTokenTest() {
		List<String> recipients = new ArrayList<>();
		recipients.add("test");

		try {
			controller.sendSummaryEmail("2020-01-01", recipients, "invalid", request, response);
			fail("Expected AccessDeniedException but got no exception.");
		} catch(AccessDeniedException e) {
			// Success case
		} catch(Exception e) {
			fail("Expected AccessDeniedException, but got " + e.getClass().getName());
		}
	}
}