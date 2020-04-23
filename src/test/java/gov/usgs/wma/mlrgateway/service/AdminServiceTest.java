package gov.usgs.wma.mlrgateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.exception.InvalidEmailException;

@ExtendWith(SpringExtension.class)
public class AdminServiceTest extends BaseSpringTest {

	@MockBean
    private LegacyCruClient cruClient;
    
	@MockBean
	private NotificationClient notificationClient;

	private AdminService service;

	@BeforeEach
	public void init() {
		service = new AdminService(cruClient, notificationClient);
    }
    
    @Test
    public void generateSummaryHTMLTest() throws Exception {
        given(cruClient.getLoggedTransactionSummary("2020-01-01", "2020-01-01", null)).willReturn(
            new ResponseEntity<String>(
                "[{\"districtCode\": \"1\", \"insertCount\": \"0\", \"updateCount\": \"0\"}, {\"districtCode\": \"2\", \"insertCount\": \"1\", \"updateCount\": \"1\"}]", 
                HttpStatus.OK
            )
        );

        String bodyHtml = service.generateSummaryHTML("2020-01-01");

        assertTrue(bodyHtml.contains("<tr><th>District Code</th><th>New Locations Added</th><th>Location Modifications</th></tr>"));
        assertTrue(bodyHtml.contains("<tr><td>1</td><td>0</td><td>0</td></tr>"));
        assertTrue(bodyHtml.contains("<tr><td>2</td><td>1</td><td>1</td></tr>"));
        assertFalse(bodyHtml.contains("No transactions were executed on 2020-01-01"));
    }

    @Test
    public void generateSummaryHTMLEmptyTest() throws Exception {
        given(cruClient.getLoggedTransactionSummary("2020-01-01", "2020-01-01", null)).willReturn(
            new ResponseEntity<String>("[]", HttpStatus.OK)
        );

        String bodyHtml = service.generateSummaryHTML("2020-01-01");

        assertFalse(bodyHtml.contains("<tr><th>District Code</th><th>New Locations Added</th><th>Location Modifications</th></tr>"));
        assertTrue(bodyHtml.contains("No transactions were executed on 2020-01-01"));
    }

    @Test
    public void sendSummaryEmailSuccessTest() {
        given(cruClient.getLoggedTransactionSummary("2020-01-01", "2020-01-01", null)).willReturn(
            new ResponseEntity<String>(
                "[{\"districtCode\": \"1\", \"insertCount\": \"0\", \"updateCount\": \"0\"}, {\"districtCode\": \"2\", \"insertCount\": \"1\", \"updateCount\": \"1\"}]", 
                HttpStatus.OK
            )
        );

        try {
            service.sendSummaryEmail("2020-01-01", Arrays.asList("test"));
        } catch(Exception e) {
            fail("Expected no Exceptions, but got " + e.getClass().getName());
        }
    }

    @Test
    public void sendSummaryEmailInvalidEmailTest() {
        given(cruClient.getLoggedTransactionSummary("2020-01-01", "2020-01-01", null)).willReturn(
            new ResponseEntity<String>(
                "[{\"districtCode\": \"1\", \"insertCount\": \"0\", \"updateCount\": \"0\"}, {\"districtCode\": \"2\", \"insertCount\": \"1\", \"updateCount\": \"1\"}]", 
                HttpStatus.OK
            )
        );

        given(notificationClient.sendEmail(any(String.class))).willThrow(
            new FeignBadResponseWrapper(400, new HttpHeaders(), "test_error")
        );

        try {
            service.sendSummaryEmail("2020-01-01", Arrays.asList("test"));
            fail("Expected InvalidEmailException, but got no exception.");
        } catch(InvalidEmailException e) {
            assertEquals("test_error", e.getMessage());
        } catch(Exception e) {
            fail("Expected InvalidEmailException but got " + e.getClass().getName());
        }
    }

    @Test
    public void sendSummaryNotificationExceptionTest() {
        given(cruClient.getLoggedTransactionSummary("2020-01-01", "2020-01-01", null)).willReturn(
            new ResponseEntity<String>(
                "[{\"districtCode\": \"1\", \"insertCount\": \"0\", \"updateCount\": \"0\"}, {\"districtCode\": \"2\", \"insertCount\": \"1\", \"updateCount\": \"1\"}]", 
                HttpStatus.OK
            )
        );

        given(notificationClient.sendEmail(any(String.class))).willThrow(
            new FeignBadResponseWrapper(401, new HttpHeaders(), "test_error")
        );

        try {
            service.sendSummaryEmail("2020-01-01", Arrays.asList("test"));
            fail("Expected FeignBadResponseWrapper, but got no exception.");
        } catch(FeignBadResponseWrapper e) {
            assertEquals("test_error", e.getBody());
        } catch(Exception e) {
            fail("Expected FeignBadResponseWrapper but got " + e.getClass().getName());
        }
    }

    @Test
    public void sendSummaryCRUExceptionTest() {
        given(cruClient.getLoggedTransactionSummary("2020-01-01", "2020-01-01", null)).willThrow(
            new FeignBadResponseWrapper(401, new HttpHeaders(), "test_error")
        );

        try {
            service.sendSummaryEmail("2020-01-01", Arrays.asList("test"));
            fail("Expected FeignBadResponseWrapper, but got no exception.");
        } catch(FeignBadResponseWrapper e) {
            assertEquals("test_error", e.getBody());
        } catch(Exception e) {
            fail("Expected FeignBadResponseWrapper but got " + e.getClass().getName());
        }
    }
}