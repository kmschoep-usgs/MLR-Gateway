package gov.usgs.wma.mlrgateway.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
public class UserSummaryReportTest {

	private UserSummaryReportBuilder builder;
	private String fileName = "test.d";
	private String reportName = "TEST GatewayReport";
	private String userName = "testUser";
	private String reportDateTime = "2018-10-01T19:37:45.439Z";
	private GatewayReport gatewayReport;
	private UserSummaryReport userSummaryReport;
	private StepReport ingestDdotFileSuccess;
	private StepReport ingestDdotFileFailure;
	private StepReport notificationSuccess;
	private StepReport notificationFailure;
	private StepReport workflowSuccess;
	private StepReport workflowFailure;
	private StepReport siteTransformerSuccess;
	private StepReport siteTransformerFailure;
	private StepReport siteValidationSuccess;
	private StepReport siteValidationWarning;
	private StepReport siteValidationFailure;
	private StepReport siteValidationWarningFailure;
	private StepReport siteStationNameDuplicateValidationFailure;
	private StepReport siteStationNameDuplicateValidationSuccess;
	private List<StepReport> stepReports;
	private ObjectMapper mapper;

	@Before
	public void setUp() throws Exception {
		builder = new UserSummaryReportBuilder();
		ingestDdotFileSuccess = new StepReport("Ingest D dot File Good", 200, true, "D dot file parsed successfully.");
		ingestDdotFileFailure = new StepReport("Ingest D dot File Bad", 400, false, "{\"DdotClient#ingestDdot(MultipartFile)\": [{\"error_message\": \"Contains lines exceeding 80 characters: line 3\"}\n]}");
		notificationSuccess = new StepReport("Notification Good", 200, true, "Notification sent successfully");
		notificationFailure = new StepReport("Notification Good Bad", 500, false, "Notification was not sent successfully");
		workflowSuccess = new StepReport("Validate D dot File workflow Succeeded", 200, true, "Validate D dot File workflow Succeeded");
		workflowFailure = new StepReport("Validate D dot File workflow failed", 500, false, "{\"error\":{\"message\": \"Unable to read ingestor output.\"}}");
		siteTransformerSuccess = new StepReport("Transform Site Good", 200, true, "this was a successful transformation");
		siteTransformerFailure = new StepReport("Transform Site Bad", 400, false, "this was a failed transformation");
		siteValidationSuccess = new StepReport("Validate Site Good", 200, true, "this was a successful validation");
		siteValidationWarning = new StepReport("Validate Site Warning", 200, true, "{\"warning_message\": \"Validation Warnings: {'latitude': ['Latitude is out of range for county 067'], 'longitude': ['Longitude is out of range for county 067']}\"}\n");
		siteValidationFailure = new StepReport("Validate Site Error", 400, false, "{\"fatal_error_message\": \"Fatal Errors: {'badField': ['This field is too long', 'This field has the wrong thing in it'], 'badField2': ['This bad field is bad']}\"}\n");
		siteValidationWarningFailure = new StepReport("Validate Site Error", 400, false, "{\"fatal_error_message\": \"Fatal Errors: {'badField': ['This field is too long', 'This field has the wrong thing in it'], 'badField2': ['This bad field is bad']}\"}, \"warning_message\": \"Validation Warnings: {'latitude': ['Latitude is out of range for county 067'], 'longitude': ['Longitude is out of range for county 067']}\"}\n");
		siteStationNameDuplicateValidationSuccess = new StepReport("Validate Duplicate Site Name Good", 200, true, "Monitoring Location Duplicate Name Validation Succeeded");
		siteStationNameDuplicateValidationFailure = new StepReport("Validate Duplicate Site Name Error", 400, false, "{\"LegacyCruClient#validateMonitoringLocation(String)\": [[\"The supplied monitoring location had a duplicate normalized station name (stationIx): 'UNUKRNRSTEWARTBC'.\\nThe following 1 monitoring location(s) had the same normalized station name: [{\\\"id\\\":1,\\\"agencyCode\\\":\\\"CAX01\\\",\\\"siteNumber\\\":\\\"15015590       \\\",\\\"stationName\\\":\\\"UNUK R NR STEWART BC\\\",\\\"stationIx\\\":\\\"UNUKRNRSTEWARTBC\\\",\\\"siteTypeCode\\\":\\\"ST\\\",\\\"decimalLatitude\\\":\\\"56.3510546798\\\",\\\"decimalLongitude\\\":\\\"-130.693374925\\\",\\\"latitude\\\":\\\"562105     \\\",\\\"longitude\\\":\\\"1304130     \\\",\\\"coordinateAccuracyCode\\\":\\\"F\\\",\\\"coordinateDatumCode\\\":\\\"NAD27     \\\",\\\"coordinateMethodCode\\\":\\\"M\\\",\\\"altitude\\\":\\\"        \\\",\\\"altitudeDatumCode\\\":\\\"          \\\",\\\"altitudeMethodCode\\\":\\\" \\\",\\\"altitudeAccuracyValue\\\":\\\"   \\\",\\\"districtCode\\\":\\\"2  \\\",\\\"countryCode\\\":\\\"CA\\\",\\\"stateFipsCode\\\":\\\"96\\\",\\\"countyCode\\\":\\\"0  \\\",\\\"minorCivilDivisionCode\\\":null,\\\"hydrologicUnitCode\\\":\\\" \\\",\\\"basinCode\\\":\\\"  \\\",\\\"nationalAquiferCode\\\":\\\"          \\\",\\\"aquiferCode\\\":\\\"        \\\",\\\"aquiferTypeCode\\\":\\\" \\\",\\\"agencyUseCode\\\":\\\"O\\\",\\\"dataReliabilityCode\\\":\\\" \\\",\\\"landNet\\\":\\\" \\\",\\\"mapName\\\":\\\"ISKUT RIVER BC\\\",\\\"mapScale\\\":\\\"250000 \\\",\\\"nationalWaterUseCode\\\":\\\"  \\\",\\\"primaryUseOfSite\\\":\\\" \\\",\\\"secondaryUseOfSite\\\":\\\" \\\",\\\"tertiaryUseOfSiteCode\\\":\\\" \\\",\\\"primaryUseOfWaterCode\\\":\\\" \\\",\\\"secondaryUseOfWaterCode\\\":\\\" \\\",\\\"tertiaryUseOfWaterCode\\\":\\\" \\\",\\\"topographicCode\\\":\\\" \\\",\\\"dataTypesCode\\\":\\\"NNNNNNNNNNNNNNNNNNNNNNNNNNNNNI\\\",\\\"instrumentsCode\\\":\\\"NNNNNNNNNNNNNNNNNNNNNNNNNNNNNN\\\",\\\"contributingDrainageArea\\\":\\\"        \\\",\\\"drainageArea\\\":\\\"573     \\\",\\\"firstConstructionDate\\\":\\\"        \\\",\\\"siteEstablishmentDate\\\":\\\"        \\\",\\\"holeDepth\\\":\\\"        \\\",\\\"wellDepth\\\":\\\"        \\\",\\\"sourceOfDepthCode\\\":\\\" \\\",\\\"projectNumber\\\":\\\"            \\\",\\\"timeZoneCode\\\":\\\"AKST \\\",\\\"daylightSavingsTimeFlag\\\":\\\"Y\\\",\\\"remarks\\\":\\\"CN STA 08DD001\\\",\\\"siteWebReadyCode\\\":\\\"Y\\\",\\\"gwFileCode\\\":\\\"NNNNNNNNNNNNNNNNNNNN\\\",\\\"created\\\":\\\"1987-10-22 00:10:00\\\",\\\"createdBy\\\":\\\"nwis    \\\",\\\"updated\\\":\\\"2016-05-14 11:05:56\\\",\\\"updatedBy\\\":\\\"nwis    \\\"}]\"]]}");
		gatewayReport = new GatewayReport(reportName, fileName);
		gatewayReport.setUserName(userName);
		gatewayReport.setReportDateTime(reportDateTime);
		userSummaryReport = new UserSummaryReport();
		mapper = new ObjectMapper();
	}

	@Test
	public void successWorkflow_goodSite() throws Exception {
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess));
		
		SiteReport siteReport1 = new SiteReport();
		siteReport1.setSuccess(true);
		siteReport1.setAgencyCode("USGS");
		siteReport1.setSiteNumber("12345678");
		siteReport1.setTransactionType("A");
		siteReport1.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport1);
		
		SiteReport siteReport2 = new SiteReport();
		siteReport2.setSuccess(true);
		siteReport2.setAgencyCode("USGS");
		siteReport2.setSiteNumber("12345679");
		siteReport2.setTransactionType("A");
		siteReport2.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport2);
		
		gatewayReport.addWorkflowStepReport(ingestDdotFileSuccess);
		gatewayReport.addWorkflowStepReport(workflowSuccess);
		gatewayReport.addWorkflowStepReport(notificationSuccess);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(fileName, userSummaryReport.getInputFileName());
		assertEquals(reportName, userSummaryReport.getName());
		assertEquals(userName, userSummaryReport.getUserName());
		assertEquals(reportDateTime, userSummaryReport.getReportDateTime());
		assertEquals(0, userSummaryReport.getSites().size());
		assertEquals(2, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(0, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(0, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}
	
	@Test
	public void successWorkflow_goodWarningSite() throws Exception {
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess, siteValidationWarning));
		
		SiteReport siteReport1 = new SiteReport();
		siteReport1.setSuccess(true);
		siteReport1.setAgencyCode("USGS");
		siteReport1.setSiteNumber("12345678");
		siteReport1.setTransactionType("A");
		siteReport1.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport1);
		
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess));
		SiteReport siteReport2 = new SiteReport();
		siteReport2.setSuccess(true);
		siteReport2.setAgencyCode("USGS");
		siteReport2.setSiteNumber("12345679");
		siteReport2.setTransactionType("A");
		siteReport2.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport2);
		
		gatewayReport.addWorkflowStepReport(ingestDdotFileSuccess);
		gatewayReport.addWorkflowStepReport(workflowSuccess);
		gatewayReport.addWorkflowStepReport(notificationSuccess);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(1, userSummaryReport.getSites().size());
		assertEquals(1, userSummaryReport.getSites().get(0).getSteps().size());
		assertEquals(2, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(0, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(0, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}
	
	@Test
	public void successWorkflow_goodErrorWarningSite() throws Exception {
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess, siteValidationWarning));
		
		SiteReport siteReport1 = new SiteReport();
		siteReport1.setSuccess(true);
		siteReport1.setAgencyCode("USGS");
		siteReport1.setSiteNumber("12345678");
		siteReport1.setTransactionType("A");
		siteReport1.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport1);
		
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerFailure, siteStationNameDuplicateValidationSuccess, siteValidationFailure));
		SiteReport siteReport2 = new SiteReport();
		siteReport2.setSuccess(false);
		siteReport2.setAgencyCode("USGS");
		siteReport2.setSiteNumber("12345679");
		siteReport2.setTransactionType("A");
		siteReport2.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport2);
		
		gatewayReport.addWorkflowStepReport(ingestDdotFileSuccess);
		gatewayReport.addWorkflowStepReport(workflowSuccess);
		gatewayReport.addWorkflowStepReport(notificationSuccess);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(2, userSummaryReport.getSites().size());
		assertEquals(1, userSummaryReport.getSites().get(0).getSteps().size());
		assertEquals(2, userSummaryReport.getSites().get(1).getSteps().size());
		assertEquals(1, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(1, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(0, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}

	@Test
	public void successWorkflow_sameErrorWarningSite() throws Exception {
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess, siteValidationWarning));
		
		SiteReport siteReport1 = new SiteReport();
		siteReport1.setSuccess(true);
		siteReport1.setAgencyCode("USGS");
		siteReport1.setSiteNumber("12345678");
		siteReport1.setTransactionType("A");
		siteReport1.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport1);
		
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerFailure, siteStationNameDuplicateValidationSuccess, siteValidationWarning));
		SiteReport siteReport2 = new SiteReport();
		siteReport2.setSuccess(false);
		siteReport2.setAgencyCode("USGS");
		siteReport2.setSiteNumber("12345679");
		siteReport2.setTransactionType("A");
		siteReport2.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport2);
		
		gatewayReport.addWorkflowStepReport(ingestDdotFileSuccess);
		gatewayReport.addWorkflowStepReport(workflowSuccess);
		gatewayReport.addWorkflowStepReport(notificationSuccess);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(2, userSummaryReport.getSites().size());
		assertEquals(1, userSummaryReport.getSites().get(0).getSteps().size());
		assertEquals(2, userSummaryReport.getSites().get(1).getSteps().size());
		assertEquals(1, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(1, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(0, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}
	
	
	@Test
	public void successWorkflow_sameValErrorWarningSite() throws Exception {
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess, siteValidationWarningFailure));
		
		SiteReport siteReport1 = new SiteReport();
		siteReport1.setSuccess(true);
		siteReport1.setAgencyCode("USGS");
		siteReport1.setSiteNumber("12345678");
		siteReport1.setTransactionType("A");
		siteReport1.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport1);
		
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerFailure, siteStationNameDuplicateValidationSuccess, siteValidationWarning));
		SiteReport siteReport2 = new SiteReport();
		siteReport2.setSuccess(false);
		siteReport2.setAgencyCode("USGS");
		siteReport2.setSiteNumber("12345679");
		siteReport2.setTransactionType("A");
		siteReport2.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport2);
		
		gatewayReport.addWorkflowStepReport(ingestDdotFileSuccess);
		gatewayReport.addWorkflowStepReport(workflowSuccess);
		gatewayReport.addWorkflowStepReport(notificationSuccess);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(2, userSummaryReport.getSites().size());
		assertEquals(1, userSummaryReport.getSites().get(0).getSteps().size());
		assertEquals(2, userSummaryReport.getSites().get(1).getSteps().size());
		assertEquals(1, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(1, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(0, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}
	
	@Test
	public void failureWorkflow_noSite() throws Exception {
		
		gatewayReport.addWorkflowStepReport(ingestDdotFileFailure);
		gatewayReport.addWorkflowStepReport(workflowFailure);
		gatewayReport.addWorkflowStepReport(notificationSuccess);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(0, userSummaryReport.getSites().size());
		assertEquals(0, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(0, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(2, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}
	
	@Test
	public void failureWorkflowStep_goodSite() throws Exception {
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess));
		
		SiteReport siteReport1 = new SiteReport();
		siteReport1.setSuccess(true);
		siteReport1.setAgencyCode("USGS");
		siteReport1.setSiteNumber("12345678");
		siteReport1.setTransactionType("A");
		siteReport1.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport1);
		
		SiteReport siteReport2 = new SiteReport();
		siteReport2.setSuccess(true);
		siteReport2.setAgencyCode("USGS");
		siteReport2.setSiteNumber("12345679");
		siteReport2.setTransactionType("A");
		siteReport2.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport2);
		gatewayReport.addWorkflowStepReport(ingestDdotFileSuccess);
		gatewayReport.addWorkflowStepReport(workflowSuccess);
		gatewayReport.addWorkflowStepReport(notificationFailure);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(0, userSummaryReport.getSites().size());
		assertEquals(2, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(0, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(1, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}
	
	@Test
	public void failureWorkflowStep_goodWarningSite() throws Exception {
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess, siteValidationWarning));
		
		SiteReport siteReport1 = new SiteReport();
		siteReport1.setSuccess(true);
		siteReport1.setAgencyCode("USGS");
		siteReport1.setSiteNumber("12345678");
		siteReport1.setTransactionType("A");
		siteReport1.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport1);
		
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess));
		
		SiteReport siteReport2 = new SiteReport();
		siteReport2.setSuccess(true);
		siteReport2.setAgencyCode("USGS");
		siteReport2.setSiteNumber("12345679");
		siteReport2.setTransactionType("A");
		siteReport2.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport2);
		gatewayReport.addWorkflowStepReport(ingestDdotFileSuccess);
		gatewayReport.addWorkflowStepReport(workflowSuccess);
		gatewayReport.addWorkflowStepReport(notificationFailure);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(1, userSummaryReport.getSites().size());
		assertEquals(1, userSummaryReport.getSites().get(0).getSteps().size());
		assertEquals(2, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(0, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(1, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}
	
	@Test
	public void failureWorkflowStep_goodWarningErrorSite() throws Exception {
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationSuccess, siteValidationSuccess, siteValidationWarning));
		
		SiteReport siteReport1 = new SiteReport();
		siteReport1.setSuccess(true);
		siteReport1.setAgencyCode("USGS");
		siteReport1.setSiteNumber("12345678");
		siteReport1.setTransactionType("A");
		siteReport1.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport1);
		
		stepReports = new ArrayList<>();
		stepReports.addAll(Arrays.asList(siteTransformerSuccess, siteStationNameDuplicateValidationFailure, siteValidationFailure));
		
		SiteReport siteReport2 = new SiteReport();
		siteReport2.setSuccess(false);
		siteReport2.setAgencyCode("USGS");
		siteReport2.setSiteNumber("12345679");
		siteReport2.setTransactionType("A");
		siteReport2.setSteps(stepReports);
		gatewayReport.addSiteReport(siteReport2);
		gatewayReport.addWorkflowStepReport(ingestDdotFileSuccess);
		gatewayReport.addWorkflowStepReport(workflowSuccess);
		gatewayReport.addWorkflowStepReport(notificationFailure);
		
		String gatewayReportBefore = mapper.writeValueAsString(gatewayReport);
		
		userSummaryReport = builder.buildUserSummaryReport(gatewayReport);
		
		String gatewayReportAfter = mapper.writeValueAsString(gatewayReport);
		
		assertEquals(2, userSummaryReport.getSites().size());
		assertEquals(1, userSummaryReport.getSites().get(0).getSteps().size());
		assertEquals(2, userSummaryReport.getSites().get(1).getSteps().size());
		assertEquals(1, userSummaryReport.getNumberSiteSuccess().intValue());
		assertEquals(1, userSummaryReport.getNumberSiteFailure().intValue());
		assertEquals(1, userSummaryReport.getWorkflowSteps().size());
		assertEquals(gatewayReportAfter, gatewayReportBefore);
	}
}