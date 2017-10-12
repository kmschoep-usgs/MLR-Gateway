package gov.usgs.wma.mlrgateway.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
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
import gov.usgs.wma.mlrgateway.controller.WorkflowController;


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
		ResponseEntity<String> emailResp = new ResponseEntity<String>("test", HttpStatus.OK);
		given(notificationClient.sendEmail(anyString(), anyString(), anyString())).willReturn(emailResp);
		service.sendNotification("test", "test");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
		
		ResponseEntity<String> rtn = notificationClient.sendEmail("test", "test", "test@test.com");
		assertEquals(rtn.getBody(), emailResp.getBody());
	}

}
