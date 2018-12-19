package gov.usgs.wma.mlrgateway.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import java.util.Arrays;
import java.util.HashMap;


@RunWith(SpringRunner.class)
public class NotificationServiceTest extends BaseSpringTest {

	@MockBean
	private NotificationClient notificationClient;

	private NotificationService service;
	private String reportName = "TEST NOTIFICATION";
	private String fileName = "test.d";
	private ObjectMapper mapper;
	private MockHttpServletResponse response;
	

	@Before
	public void init() {
		service = new NotificationService(notificationClient);
		response = new MockHttpServletResponse();
		BaseController.setReport(new GatewayReport(reportName, fileName));
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath() throws Exception {
		ResponseEntity<String> emailResp = new ResponseEntity<>("test", HttpStatus.OK);
		List<String> recipientList = new ArrayList<>();
		recipientList.add("test");
		given(notificationClient.sendEmail(anyString())).willReturn(emailResp);
		GatewayReport report = BaseController.getReport();
		service.sendNotification(recipientList, "test", "test", "test", report);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertEquals(report.getUserName(), "test");
		assertNotNull(report.getReportDateTime());
		verify(notificationClient).sendEmail(anyString());
		
		ResponseEntity<String> rtn = notificationClient.sendEmail("test");
		assertEquals(rtn.getBody(), emailResp.getBody());
	}

	@Test
	public void buildRequestMapTest() throws Exception {
		GatewayReport report = new GatewayReport("report-name", "report-file-name");
		report.setReportDateTime("report-date-time");
		List<String> recipientList = Arrays.asList("test-recipient");
		String subject = "test-subject";
		String user = "test-user";
		String attachmentFileName = "test-d.file";
		String expectedBody = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        test-user\n\n" +
			"Workflow:    report-name\n\n" +
			"Report Date: report-date-time\n\n" +
			"The full, raw report output is attached.\n";
		String expectedAttachment = "mlr-test-d.file-report.json";

		HashMap<String, Object> result = service.buildRequestMap(recipientList, subject, user, attachmentFileName, report);
		assertEquals(result.get(NotificationClient.MESSAGE_TO_KEY), recipientList);
		assertEquals(result.get(NotificationClient.MESSAGE_TEXT_BODY_KEY), expectedBody);
		assertEquals(result.get(NotificationClient.MESSAGE_SUBJECT_KEY), subject);
		assertEquals(result.get(NotificationClient.MESSAGE_ATTACHMENT_KEY), report.toPrettyPrintString());
		assertEquals(result.get(NotificationClient.MESSAGE_ATTACHMENT_FILE_NAME_KEY), expectedAttachment);
	}
	
	@Test
	public void notificationService_sendEmail_handlesError() {
		given(notificationClient.sendEmail(anyString())).willThrow(new RuntimeException());
		List<String> recipientList = new ArrayList<>();
		recipientList.add("test");
		service.sendNotification(recipientList, "test", "test", "test", BaseController.getReport());
	}

	@Test
	public void buildMessageBodyTest() throws Exception {
		GatewayReport report = new GatewayReport("report-name", "report-file-name");
		report.setReportDateTime("report-date-time");
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n\n" +
			"Workflow:    report-name\n\n" +
			"Report Date: report-date-time\n\n" +
			"The full, raw report output is attached.\n";

		assertEquals(expected, service.buildMessageBody(report, "report-user"));
	}

	@Test
	public void buildAttachmentNameTest() {
		assertEquals("mlr-test-report.json", service.buildAttachmentName("test"));
		assertEquals("mlr-output-report.json", service.buildAttachmentName(""));
		assertEquals("mlr-output-report.json", service.buildAttachmentName(null));
	}
}
