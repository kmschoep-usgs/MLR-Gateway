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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.exception.InvalidEmailException;
import gov.usgs.wma.mlrgateway.service.AdminService;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SuppressWarnings("unchecked")
public class AdminControllerTest extends BaseSpringTest {

	@MockBean
	private AdminService adminService;

	private AdminController controller;
	private MockHttpServletResponse response;

	@BeforeEach
	public void init() {
		controller = new AdminController(adminService);
		response = new MockHttpServletResponse();
	}

	@Test
	public void _sendSummaryEmailHappyPath() {
		List<String> recipients = new ArrayList<>();
		recipients.add("test");

		try {
			controller.sendSummaryEmail("2020-01-01", recipients, response);
		} catch(Exception e) {
            fail("Expected no Exceptions, but got " + e.getClass().getName());
        }
	}

	@Test
	public void invalidEmailExceptionCaughtTest() throws Exception {
		String errorMessage = "Invalid email";
		List<String> recipients = new ArrayList<>();
		recipients.add("test");

		willThrow(new InvalidEmailException(errorMessage)).given(adminService).sendSummaryEmail(eq("2020-01-01"), any(List.class));
		
		try {
			controller.sendSummaryEmail("2020-01-01", recipients, response);
		} catch(Exception e) {
			fail("Expected expcetion to be caught but got " + e.getClass().getName());
		}
	}

	@Test
	public void badBackingServiceRequest() throws Exception {
		String badText = "This is really bad.";
		List<String> recipients = new ArrayList<>();
		recipients.add("test");

		willThrow(new FeignBadResponseWrapper(500, null, badText)).given(adminService).sendSummaryEmail(eq("2020-01-01"), any(List.class));
		
		try {
			controller.sendSummaryEmail("2020-01-01", recipients, response);
			fail("Expected FeignBadResponseWrapper but got no exception.");
		} catch(FeignBadResponseWrapper e) {
			assertEquals(500, e.getStatus());
		} catch(Exception e) {
			fail("Expected FeignBadResponseWrapper, but got " + e.getClass().getName());
		}
	}
}
