package gov.usgs.wma.mlrgateway.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;
import java.util.ArrayList;
import java.util.List;


@RunWith(SpringRunner.class)
public class NotificationServiceTest extends BaseSpringTest {

	@MockBean
	private NotificationClient notificationClient;

	private NotificationService service;
	private String reportName = "TEST NOTIFICATION";
	private ObjectMapper mapper;
	private MockHttpServletResponse response;
	

	@Before
	public void init() {
		service = new NotificationService(notificationClient);
		response = new MockHttpServletResponse();
		BaseController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath() throws Exception {
		ResponseEntity<String> emailResp = new ResponseEntity<>("test", HttpStatus.OK);
		List<String> recipientList = new ArrayList<>();
		recipientList.add("test");
		given(notificationClient.sendEmail(anyString())).willReturn(emailResp);
		service.sendNotification(recipientList, "test", "test", BaseController.getReport());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		verify(notificationClient).sendEmail(anyString());
		
		ResponseEntity<String> rtn = notificationClient.sendEmail("test");
		assertEquals(rtn.getBody(), emailResp.getBody());
	}
	
	@Test
	public void notificationService_sendEmail_handlesError() {
		given(notificationClient.sendEmail(anyString())).willThrow(new RuntimeException());
		List<String> recipientList = new ArrayList<>();
		recipientList.add("test");
		service.sendNotification(recipientList, "test", "test", BaseController.getReport());
	}

}
