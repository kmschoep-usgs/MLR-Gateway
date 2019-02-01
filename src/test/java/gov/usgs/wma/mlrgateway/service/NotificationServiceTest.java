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
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
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
	private UserSummaryReport userSummaryReport;
	private MockHttpServletResponse response;
	

	@Before
	public void init() {
		service = new NotificationService(notificationClient);
		response = new MockHttpServletResponse();
		BaseController.setReport(new GatewayReport(reportName, fileName));
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		userSummaryReport = new UserSummaryReport();
		userSummaryReport.setWorkflowSteps(workflowSteps);
		userSummaryReport.setName(reportName);
		userSummaryReport.setInputFileName(fileName);
		userSummaryReport.setSites(sites);
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath() throws Exception {
		ResponseEntity<String> emailResp = new ResponseEntity<>("test", HttpStatus.OK);
		List<String> recipientList = new ArrayList<>();
		recipientList.add("test");
		given(notificationClient.sendEmail(anyString())).willReturn(emailResp);
		UserSummaryReport report = userSummaryReport;
		report.setNumberSiteFailure(0);
		report.setNumberSiteSuccess(0);
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
		userSummaryReport.setReportDateTime("report-date-time");
		userSummaryReport.setNumberSiteFailure(0);
		userSummaryReport.setNumberSiteSuccess(0);
		List<String> recipientList = Arrays.asList("test-recipient");
		String subject = "test-subject";
		String user = "test-user";
		String attachmentFileName = "test-d.file";
		String expectedBody = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        test-user\n" +
			"Workflow:    TEST NOTIFICATION\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 0 Transactions Failed\n\n";
		String expectedAttachment = "mlr-test-d.file-report.json";

		HashMap<String, Object> result = service.buildRequestMap(recipientList, subject, user, attachmentFileName, userSummaryReport);
		assertEquals(result.get(NotificationClient.MESSAGE_TO_KEY), recipientList);
		assertEquals(result.get(NotificationClient.MESSAGE_TEXT_BODY_KEY), expectedBody);
		assertEquals(result.get(NotificationClient.MESSAGE_SUBJECT_KEY), subject);
		assertEquals(result.get(NotificationClient.MESSAGE_ATTACHMENT_KEY), userSummaryReport.toPrettyPrintString());
		assertEquals(result.get(NotificationClient.MESSAGE_ATTACHMENT_FILE_NAME_KEY), expectedAttachment);
	}
	
	@Test
	public void notificationService_sendEmail_handlesError() {
		userSummaryReport.setNumberSiteFailure(0);
		userSummaryReport.setNumberSiteSuccess(0);
		given(notificationClient.sendEmail(anyString())).willThrow(new RuntimeException());
		List<String> recipientList = new ArrayList<>();
		recipientList.add("test");
		service.sendNotification(recipientList, "test", "test", "test", userSummaryReport);
	}

	@Test
	public void buildMessageBodyTest() throws Exception {
		userSummaryReport.setReportDateTime("report-date-time");
		userSummaryReport.setNumberSiteFailure(0);
		userSummaryReport.setNumberSiteSuccess(0);
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    TEST NOTIFICATION\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 0 Transactions Failed\n\n";

		assertEquals(expected, service.buildMessageBody(userSummaryReport, "report-user"));
	}
	
	@Test
	public void buildMessageBodyFailedExportTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		StepReport workflowStep = new StepReport("Complete Export Workflow", 404, false, "{\"error_message\": \"Requested Location Not Found\"}");
		workflowSteps.add(workflowStep);
		report.setName("Complete Export Workflow");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Complete Export Workflow\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Complete Export Workflow Failed: Requested Location Not Found\n\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}
	
	@Test
	public void buildMessageBodyFailedWorkflowTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		StepReport workflowFailureStep = new StepReport("Validate D dot File workflow failed", 404, false, "{\"error_message\": \"Unable to read ingestor output.\"}");
		StepReport workflowErrorStep = new StepReport("Ingest D dot File", 400, false, "{\"error_message\":\"Contains lines with invalid site number format: lines 2, 3, 4, 5, 6, 7, 8, 9, 10, 11.\"}");
		List<SiteReport> sites = new ArrayList<>();
		workflowSteps.add(workflowFailureStep);
		workflowSteps.add(workflowErrorStep);
		report.setName("Validate D dot File");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Validate D dot File\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Validate D dot File workflow failed (Unable to read ingestor output.) : No Transactions were processed.\n\n" +
			"Error details listed below:\n\n" +
			"Workflow-level Errors:\n\n" +
			"Ingest D dot File: Contains lines with invalid site number format: lines 2, 3, 4, 5, 6, 7, 8, 9, 10, 11.\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}
	
	@Test
	public void buildMessageBodyFailedDuplicateStationNameTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		List<StepReport> siteSteps = new ArrayList<>();
		StepReport siteErrorStep = new StepReport("Validate Duplicate Monitoring Location Name", 406, false, "{\"error_message\":{\"stationIx\":\"Duplicate normalized station name locations found for 'GILBERTLAKESPRING4NESIDENRWESTBENDWI': USGS-432452088151501, stateFipsCode: 55\",\"duplicate_site\":\"Duplicate Agency Code and Site Number found in MLR.\"}}");
		SiteReport siteReport = new SiteReport("USGS", "12345678    ");
		siteSteps.add(siteErrorStep);
		siteReport.setSteps(siteSteps);
		sites.add(siteReport);
		report.setName("Validate D dot File");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		report.setNumberSiteSuccess(0);
		report.setNumberSiteFailure(1);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Validate D dot File\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 1 Transactions Failed\n\n" +
			"USGS-12345678, Validate Duplicate Monitoring Location Name Fatal Error: stationIx - Duplicate normalized station name locations found for 'GILBERTLAKESPRING4NESIDENRWESTBENDWI': USGS-432452088151501, stateFipsCode: 55\n" + 
			"USGS-12345678, Validate Duplicate Monitoring Location Name Fatal Error: duplicate_site - Duplicate Agency Code and Site Number found in MLR.\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}

	@Test
	public void buildMessageBodyFailedOneValidationWarningTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		List<StepReport> siteSteps = new ArrayList<>();
		StepReport siteErrorStep = new StepReport("Validate", 400, false, "{\"validator_message\": {\"warning_message\": {\"latitude\": [\"latitude warning 1\", \"Latitude warning 2\"]}}\n}");
		SiteReport siteReport = new SiteReport("USGS", "12345678    ");
		siteSteps.add(siteErrorStep);
		siteReport.setSteps(siteSteps);
		sites.add(siteReport);
		report.setName("Validate D dot File");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		report.setNumberSiteSuccess(0);
		report.setNumberSiteFailure(1);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Validate D dot File\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 1 Transactions Failed\n\n" +
			"USGS-12345678, Validate Warning: latitude - latitude warning 1; Latitude warning 2\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}
	
	@Test
	public void buildMessageBodyFailedMultipleValidationWarningTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		List<StepReport> siteSteps = new ArrayList<>();
		StepReport siteErrorStep = new StepReport("Validate", 400, false, "{\"validator_message\": {\"warning_message\": {\"latitude\": [\"Latitude is out of range for county 067\"], \"longitude\": [\"Longitude is out of range for county 067\"]}}\n}");
		SiteReport siteReport = new SiteReport("USGS", "12345678    ");
		siteSteps.add(siteErrorStep);
		siteReport.setSteps(siteSteps);
		sites.add(siteReport);
		report.setName("Validate D dot File");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		report.setNumberSiteSuccess(0);
		report.setNumberSiteFailure(1);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Validate D dot File\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 1 Transactions Failed\n\n" +
			"USGS-12345678, Validate Warning: latitude - Latitude is out of range for county 067\n" +
			"USGS-12345678, Validate Warning: longitude - Longitude is out of range for county 067\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}
	
	@Test
	public void buildMessageBodyFailedOneValidationErrorTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		List<StepReport> siteSteps = new ArrayList<>();
		StepReport siteErrorStep = new StepReport("Validate", 400, false, "{\"validator_message\": {\"fatal_error_message\": {\"latitude\": [\"Invalid Degree/Minute/Second Value\", \"Latitude is out of range for state 55\"]}}\n}");
		SiteReport siteReport = new SiteReport("USGS", "12345678    ");
		siteSteps.add(siteErrorStep);
		siteReport.setSteps(siteSteps);
		sites.add(siteReport);
		report.setName("Validate D dot File");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		report.setNumberSiteSuccess(0);
		report.setNumberSiteFailure(1);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Validate D dot File\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 1 Transactions Failed\n\n" +
			"USGS-12345678, Validate Fatal Error: latitude - Invalid Degree/Minute/Second Value; Latitude is out of range for state 55\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}
	
	@Test
	public void buildMessageBodyFailedMultipleValidationErrorTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		List<StepReport> siteSteps = new ArrayList<>();
		StepReport siteErrorStep = new StepReport("Validate", 400, false, "{\"validator_message\": {\"fatal_error_message\": {\"latitude\": [\"Invalid Degree/Minute/Second Value\", \"Latitude is out of range for state 55\"], \"longitude\": [\"Longitude is out of range for state 55\"]}}\n}");
		SiteReport siteReport = new SiteReport("USGS", "12345678    ");
		siteSteps.add(siteErrorStep);
		siteReport.setSteps(siteSteps);
		sites.add(siteReport);
		report.setName("Validate D dot File");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		report.setNumberSiteSuccess(0);
		report.setNumberSiteFailure(1);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Validate D dot File\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 1 Transactions Failed\n\n" +
			"USGS-12345678, Validate Fatal Error: latitude - Invalid Degree/Minute/Second Value; Latitude is out of range for state 55\n" +
			"USGS-12345678, Validate Fatal Error: longitude - Longitude is out of range for state 55\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}
	
	@Test
	public void buildMessageBodyFailedValidationErrorWarningTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		List<StepReport> siteSteps = new ArrayList<>();
		StepReport siteErrorStep = new StepReport("Validate", 400, false, "{\"validator_message\": {\"fatal_error_message\": {\"latitude\": [\"Invalid Degree/Minute/Second Value\", \"Latitude is out of range for state 55\"], \"longitude\": [\"Longitude is out of range for state 55\"]}, \"warning_message\": {\"latitude\": [\"Latitude is out of range for county 067\"], \"longitude\": [\"Longitude is out of range for county 067\"]}}\n}");
		SiteReport siteReport = new SiteReport("USGS", "12345678    ");
		siteSteps.add(siteErrorStep);
		siteReport.setSteps(siteSteps);
		sites.add(siteReport);
		report.setName("Validate D dot File");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		report.setNumberSiteSuccess(0);
		report.setNumberSiteFailure(1);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Validate D dot File\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 1 Transactions Failed\n\n" +
			"USGS-12345678, Validate Fatal Error: latitude - Invalid Degree/Minute/Second Value; Latitude is out of range for state 55\n" +
			"USGS-12345678, Validate Fatal Error: longitude - Longitude is out of range for state 55\n" +
			"USGS-12345678, Validate Warning: latitude - Latitude is out of range for county 067\n" +
			"USGS-12345678, Validate Warning: longitude - Longitude is out of range for county 067\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}
	
	@Test
	public void buildMessageBodyFailedTwoSitesOneValidationWarningErrorTest() throws Exception {
		UserSummaryReport report = new UserSummaryReport();
		List<StepReport> workflowSteps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		List<StepReport> siteWarningSteps = new ArrayList<>();
		List<StepReport> siteErrorSteps = new ArrayList<>();
		StepReport siteWarningStep = new StepReport("Validate", 400, false, "{\"validator_message\": {\"warning_message\": {\"latitude\": [\"latitude warning 1\", \"Latitude warning 2\"]}}\n}");
		StepReport siteErrorStep = new StepReport("Validate", 400, false, "{\"validator_message\": {\"fatal_error_message\": {\"latitude\": [\"Invalid Degree/Minute/Second Value\", \"Latitude is out of range for state 55\"]}}\n}");
		SiteReport siteReport1 = new SiteReport("USGS", "12345678    ");
		SiteReport siteReport2 = new SiteReport("USGS", "12345679    ");
		siteWarningSteps.add(siteWarningStep);
		siteReport1.setSteps(siteWarningSteps);
		siteErrorSteps.add(siteErrorStep);
		siteReport2.setSteps(siteErrorSteps);
		sites.add(siteReport1);
		sites.add(siteReport2);
		report.setName("Validate D dot File");
		report.setUserName("report-user");
		report.setInputFileName("report-file-name");
		report.setReportDateTime("report-date-time");
		report.setWorkflowSteps(workflowSteps);
		report.setSites(sites);
		report.setNumberSiteSuccess(0);
		report.setNumberSiteFailure(1);
		
		String expected = "An MLR Workflow has completed on the null environment. The workflow output report is below.\n\n\n" +
			"User:        report-user\n" +
			"Workflow:    Validate D dot File\n" +
			"Report Date: report-date-time\n" +
			"The full, raw report output is attached.\n\n" +
			"Status:  0 Transactions Succeeded, 1 Transactions Failed\n\n" +
			"USGS-12345679, Validate Fatal Error: latitude - Invalid Degree/Minute/Second Value; Latitude is out of range for state 55\n" +
			"USGS-12345678, Validate Warning: latitude - latitude warning 1; Latitude warning 2\n";
		
		String result = service.buildMessageBody(report, "report-user");

		assertEquals(expected, result);
	}
	
	@Test
	public void buildAttachmentNameTest() {
		assertEquals("mlr-test-report.json", service.buildAttachmentName("test"));
		assertEquals("mlr-output-report.json", service.buildAttachmentName(""));
		assertEquals("mlr-output-report.json", service.buildAttachmentName(null));
	}
}
