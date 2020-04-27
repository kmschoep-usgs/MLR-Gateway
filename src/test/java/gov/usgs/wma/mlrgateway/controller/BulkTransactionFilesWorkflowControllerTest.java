package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.workflow.BulkTransactionFilesWorkflowService;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.util.UserAuthUtil;

import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@ExtendWith(SpringExtension.class)
public class BulkTransactionFilesWorkflowControllerTest extends BaseSpringTest {

	@MockBean
	private NotificationService notify;
	@MockBean
	private BulkTransactionFilesWorkflowService bulkTransactions;
	@MockBean
	private UserAuthUtil userAuthUtil;
	
	@Bean
	@Primary
	public Clock clock() {
		return Clock.fixed(Instant.parse("2010-01-10T10:00:00Z"), ZoneId.of("UTC"));
	}

	private BulkTransactionFilesWorkflowController controller;
	private MockHttpServletResponse response;
	private Map<String, Serializable> testEmail;
	private UsernamePasswordAuthenticationToken mockAuth = new UsernamePasswordAuthenticationToken("user", "pass");

	@BeforeEach
	public void init() {
		given(userAuthUtil.getUserEmail(any(Authentication.class))).willReturn("test@test");
		
		testEmail = new HashMap<>();
		testEmail.put("email", "localuser@example.gov");
		controller = new BulkTransactionFilesWorkflowController(bulkTransactions, notify, userAuthUtil, clock());
		response = new MockHttpServletResponse();
	}

	@Test
	public void happyPath_BulkTransactionFilesWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "sites.csv", "text/plain", "".getBytes());
		UserSummaryReport rtn = controller.bulkGenerateTransactionFilesWorkflow(file, response, mockAuth);
		assertEquals(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_STEP, rtn.getName() );
		assertEquals(new ArrayList<>(), rtn.getWorkflowSteps());
		assertEquals(new ArrayList<>(), rtn.getSites());
		assertEquals("sites.csv", rtn.getInputFileName());
		verify(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));
	}

	@Test
	public void badBackingServiceRequest_BulkTransactionFilesWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "sites.csv", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(400, null, badText)).given(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));

		UserSummaryReport rtn = controller.bulkGenerateTransactionFilesWorkflow(file, response, mockAuth);
		StepReport bulkTransactionsWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_STEP, rtn.getName());
		assertEquals("400", bulkTransactionsWorkflowStep.getHttpStatus().toString());
		assertEquals(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED, bulkTransactionsWorkflowStep.getName());
		assertEquals(badText, bulkTransactionsWorkflowStep.getDetails());
		assertEquals("sites.csv", rtn.getInputFileName());
		verify(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));
	}

	@Test
	public void serverError_BulkTransactionFilesWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "sites.csv", "text/plain", "".getBytes());
		willThrow(new HystrixBadRequestException(badText)).given(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));
		UserSummaryReport rtn = controller.bulkGenerateTransactionFilesWorkflow(file, response, mockAuth);
		StepReport bulkTransactionsWorkflowStep = rtn.getWorkflowSteps().stream()
				.filter(s -> BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED.equals(s.getName()))
				.findAny().orElse(null);
		assertEquals(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_STEP, rtn.getName());
		assertEquals("500", bulkTransactionsWorkflowStep.getHttpStatus().toString());
		assertEquals(BulkTransactionFilesWorkflowService.BULK_GENERATE_TRANSACTION_FILES_WORKFLOW_FAILED, bulkTransactionsWorkflowStep.getName());
		assertEquals(badText, bulkTransactionsWorkflowStep.getDetails());
		assertEquals("sites.csv", rtn.getInputFileName());
		
		verify(bulkTransactions).generateTransactionFilesWorkflow(any(MultipartFile.class));
	}
}
