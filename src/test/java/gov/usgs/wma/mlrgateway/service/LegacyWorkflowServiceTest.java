package gov.usgs.wma.mlrgateway.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.client.FileExportClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.LegacyValidatorClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import net.minidev.json.JSONObject;

@RunWith(SpringRunner.class)
public class LegacyWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private DdotService ddotService;
	@MockBean
	private LegacyCruClient legacyCruClient;
	@MockBean
	private TransformService transformService;
	@MockBean
	private LegacyValidatorClient legacyValidatorClient;
	@MockBean
	private FileExportClient fileExportClient;
	@MockBean
	private NotificationClient notificationClient;

	private LegacyWorkflowService service;
	private MockHttpServletResponse response;
	private ObjectMapper mapper;
	private String reportName = "TEST Legacy Workflow";
	private String legacyJson = "{\"transactionType\":\"A\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}";
	private String legacyValidation = "{\"validation_passed_message\": \"Validations Passed\"}";


	@Before
	public void init() {
		service = new LegacyWorkflowService(ddotService, legacyCruClient, transformService, legacyValidatorClient, fileExportClient, notificationClient);
		response = new MockHttpServletResponse();
		WorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath_transformAndValidate_thenReturnObject() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\""
				+ LegacyWorkflowService.VALIDATION_SUCCESSFULL + "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}]}";
		Map<String, Object> ml = getAdd();
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyValidation, HttpStatus.OK);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);

		JSONAssert.assertEquals(legacyJson.replace("}", ",\"validation\":" + legacyValidation + "}"), service.transformAndValidate(ml), JSONCompareMode.STRICT);
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
	}

	@Test
	public void noTransactionType_completeWorkflow_thenReturnBadRequest() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":400,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.BAD_TRANSACTION_TYPE.replace("%json%", "{}"))
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.NOTIFICATION_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.NOTIFICATION_SUCCESSFULL) + "\"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUnknown();
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyValidation, HttpStatus.OK);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(notificationClient.sendEmail(anyString(), anyString(), anyString())).willReturn(legacyRtn);


		service.completeWorkflow(file);
		System.out.println("\n\n" + mapper.writeValueAsString(WorkflowController.getReport()) + "\n\n");
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void noTransactionType_completeWorkflow_noStopOnError() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":400,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":400,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.BAD_TRANSACTION_TYPE.replace("%json%", "{}"))
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"" + LegacyWorkflowService.VALIDATION_SUCCESSFULL
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":201,\"details\":\"" + LegacyWorkflowService.SITE_ADD_SUCCESSFULL
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + LegacyWorkflowService.EXPORT_SUCCESSFULL
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.NOTIFICATION_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.NOTIFICATION_SUCCESSFULL) + "\"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.multipleWithErrors();
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		Map<String, Object> ml = getAdd();
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyValidation, HttpStatus.OK);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);
		ResponseEntity<String> addRtn = new ResponseEntity<String>(legacyJson, HttpStatus.CREATED);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(addRtn);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);
		given(notificationClient.sendEmail(anyString(), anyString(), anyString())).willReturn(legacyRtn);

		service.completeWorkflow(file);
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void oneAddTransaction_completeWorkflow_thenReturnCreated() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"" + LegacyWorkflowService.VALIDATION_SUCCESSFULL
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":201,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_ADD_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.NOTIFICATION_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.NOTIFICATION_SUCCESSFULL) + "\"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleAdd();
		Map<String, Object> ml = ddotRtn.get(0);
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		ResponseEntity<String> addRtn = new ResponseEntity<String>(legacyJson, HttpStatus.CREATED);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(addRtn);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);
		given(notificationClient.sendEmail(anyString(), anyString(), anyString())).willReturn(legacyRtn);

		service.completeWorkflow(file);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void oneUpdateTransaction_completeWorkflow_thenReturnUpdated() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\"" + LegacyWorkflowService.VALIDATION_SUCCESSFULL
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_UPDATE_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.NOTIFICATION_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.NOTIFICATION_SUCCESSFULL) + "\"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = DdotServiceTest.singleUpdate();
		Map<String, Object> ml = ddotRtn.get(0);
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyCruClient.patchMonitoringLocation(anyString())).willReturn(legacyRtn);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);
		given(fileExportClient.exportUpdate(anyString())).willReturn(legacyRtn);
		given(notificationClient.sendEmail(anyString(), anyString(), anyString())).willReturn(legacyRtn);

		service.completeWorkflow(file);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void ddotValidation_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":[{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":200,\"details\":\""
				+ LegacyWorkflowService.VALIDATION_SUCCESSFULL + "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.NOTIFICATION_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.NOTIFICATION_SUCCESSFULL) + "\"}"
				+ "]}";
		Map<String, Object> ml = getAdd();
		given(transformService.transform(anyMap())).willReturn(ml);
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyValidation, HttpStatus.OK);
		given(legacyValidatorClient.validate(anyString())).willReturn(legacyRtn);
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());
		given(notificationClient.sendEmail(anyString(), anyString(), anyString())).willReturn(legacyRtn);

		service.ddotValidation(file);
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient).validate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void ddotValidation_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.VALIDATION_STEP + "\",\"status\":500,\"details\":\"" + "Transaction validation failed."
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.NOTIFICATION_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.NOTIFICATION_SUCCESSFULL) + "\"}"
				+ "]}";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>("", HttpStatus.OK);
		given(transformService.transform(anyMap())).willThrow(new RuntimeException());
		given(notificationClient.sendEmail(anyString(), anyString(), anyString())).willReturn(legacyRtn);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(DdotServiceTest.singleAdd());

		service.ddotValidation(file);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(transformService).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void addTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":201,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_ADD_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		ResponseEntity<String> addRtn = new ResponseEntity<String>(legacyJson, HttpStatus.CREATED);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(addRtn);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);

		service.addTransaction("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient, never()).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void addTransaction_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_ADD_STEP + "\",\"status\":500,\"details\":\"" + "Site add failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(legacyCruClient.createMonitoringLocation(anyString())).willThrow(new RuntimeException());

		service.addTransaction("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient, never()).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void addExport_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		given(fileExportClient.exportAdd(anyString())).willReturn(legacyRtn);

		service.exportAdd("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient, never()).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void addExport_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_ADD_STEP + "\",\"status\":500,\"details\":\"" + "Export add failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(fileExportClient.exportAdd(anyString())).willThrow(new RuntimeException());

		service.exportAdd("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient, never()).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void updateTransaction_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.SITE_UPDATE_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"},"
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		given(legacyCruClient.patchMonitoringLocation(anyString())).willReturn(legacyRtn);
		given(fileExportClient.exportUpdate(anyString())).willReturn(legacyRtn);

		service.updateTransaction("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
		verify(notificationClient, never()).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void updateTransaction_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.SITE_UPDATE_STEP + "\",\"status\":500,\"details\":\"" + "Site update failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(legacyCruClient.patchMonitoringLocation(anyString())).willThrow(new RuntimeException());

		service.updateTransaction("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient, never()).exportUpdate(anyString());
		verify(notificationClient, never()).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void updateExport_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_UPDATE_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.EXPORT_SUCCESSFULL)
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		given(fileExportClient.exportUpdate(anyString())).willReturn(legacyRtn);

		service.exportUpdate("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
		verify(notificationClient, never()).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void updateExport_throwsException() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.EXPORT_UPDATE_STEP + "\",\"status\":500,\"details\":\"" + "Export update failed"
				+ "\",\"agencyCode\":\"USGS \",\"siteNumber\":\"12345678       \"}"
				+ "]}";
		given(fileExportClient.exportUpdate(anyString())).willThrow(new RuntimeException());

		service.exportUpdate("USGS ", "12345678       ", "{}");
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(ddotService, never()).parseDdot(any(MultipartFile.class));
		verify(transformService, never()).transform(anyMap());
		verify(legacyValidatorClient, never()).validate(anyString());
		verify(legacyCruClient, never()).createMonitoringLocation(anyString());
		verify(legacyCruClient, never()).patchMonitoringLocation(anyString());
		verify(fileExportClient, never()).exportAdd(anyString());
		verify(fileExportClient).exportUpdate(anyString());
		verify(notificationClient, never()).sendEmail(anyString(), anyString(), anyString());
	}

	@Test
	public void sendNotification_callsBackingServices() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":["
				+ "{\"name\":\"" + LegacyWorkflowService.NOTIFICATION_STEP + "\",\"status\":200,\"details\":\"" + JSONObject.escape(LegacyWorkflowService.NOTIFICATION_SUCCESSFULL) + "\"}"
				+ "]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>("", HttpStatus.OK);
		given(notificationClient.sendEmail(anyString(), anyString(), anyString())).willReturn(legacyRtn);

		service.sendNotification();
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
	}
}
