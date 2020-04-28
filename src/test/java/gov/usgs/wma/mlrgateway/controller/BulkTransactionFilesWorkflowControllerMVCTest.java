package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.PermissionEvaluatorImpl;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.workflow.BulkTransactionFilesWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.util.ParseCSV;
import gov.usgs.wma.mlrgateway.util.UserAuthUtil;
import gov.usgs.wma.mlrgateway.config.MethodSecurityConfig;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers={BulkTransactionFilesWorkflowController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({MvcTestConfig.class, PermissionEvaluatorImpl.class, MethodSecurityConfig.class})
public class BulkTransactionFilesWorkflowControllerMVCTest {

	@Autowired
	private MockMvc mvc;
	
	@MockBean
	private ParseCSV parseService;

	@MockBean
	private BulkTransactionFilesWorkflowService bulkTransactions;

	@MockBean
	private NotificationService notificationService;

	@MockBean
	private NotificationClient notificationClient;

	@MockBean
	private LegacyCruClient legacyClient;

	@MockBean
	private UserAuthUtil userAuthUtil;

	@MockBean
	private Clock clockMock;

	private MockMultipartFile file;

	@BeforeEach
	public void init() {
		file = new MockMultipartFile("file", "sites.csv", "text/plain", "".getBytes());
		when(clockMock.instant()).thenReturn(Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC")).instant());
	}
	
	@Test
	@WithMockUser(username = "test", authorities = "test_allowed")
	public void happyPathBulkTransactionFilesWorkflow() throws Exception {
		String bulkTransactionsJson = "{\"name\":\"" + BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW + "\",\"inputFileName\":\"sites.csv\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/bulkTransactionFiles").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(bulkTransactionsJson));

		verify(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(username = "test")
	public void happyPathBulkTransactionFilesWorkflowNoAuthorities() throws Exception {
		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/bulkTransactionFiles").file(file))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType("application/json"));

		verify(bulkTransactions, never()).generateTransactionFilesWorkflow(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(username = "test", authorities = "test_allowed")
	public void badResponse_BulkTransactionFilesWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW + "\",\"inputFileName\":\"sites.csv\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\",\"workflowSteps\":[{\"name\":\"" 
				+ BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED + "\",\"httpStatus\":400,\"success\":false,\"details\":\"{\\\"error\\\": 123}\"}],"
				+ "\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new FeignBadResponseWrapper(HttpStatus.SC_BAD_REQUEST, null, "{\"error\": 123}")).given(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/bulkTransactionFiles").file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));
	}

	@Test
	@WithMockUser(username = "test", authorities = "test_allowed")
	public void serverError_BulkTransactionFilesWorkflow() throws Exception {
		String badJson = "{\"name\":\"" + BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW + "\",\"inputFileName\":\"sites.csv\","
				+ "\"reportDateTime\":\"2010-01-10T10:00:00Z\",\"userName\":\"test\","
				+ "\"workflowSteps\":[{\"name\":\"" + BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED + "\",\"httpStatus\":500,"
				+  "\"success\":false,\"details\":\"wow 456\"}],\"sites\":[],\"numberSiteSuccess\":0,\"numberSiteFailure\":0}";
		willThrow(new RuntimeException("wow 456")).given(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));

		mvc.perform(MockMvcRequestBuilders.multipart("/workflows/bulkTransactionFiles").file(file))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().string(badJson));

		verify(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));
	}
}