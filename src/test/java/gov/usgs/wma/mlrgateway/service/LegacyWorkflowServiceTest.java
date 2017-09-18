package gov.usgs.wma.mlrgateway.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;

@RunWith(SpringRunner.class)
public class LegacyWorkflowServiceTest extends BaseSpringTest {

	@MockBean
	private DdotService ddotService;

	@MockBean
	private LegacyCruClient legacyCruClient;

	private LegacyWorkflowService service;
	private MockHttpServletResponse response;

	@Before
	public void init() {
		service = new LegacyWorkflowService(ddotService, legacyCruClient);
		response = new MockHttpServletResponse();
	}

	@Test
	public void noTransactionType_thenReturnBadRequest() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<?,?>> ddotRtn = DdotServiceTest.singleUnknown();
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		try {
			service.completeWorkflow(file, response);
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.BAD_REQUEST.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals("{\"error\":{\"message\":\"Unable to determine transactionType.\"},\"data\":{\"siteNumber\":\"12345678       \", \"agencyCode\":\"USGS \"}}", ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
		verify(ddotService).parseDdot(any(MultipartFile.class));
	}

	@Test
	public void oneAddTransaction_thenReturnCreated() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<?,?>> ddotRtn = DdotServiceTest.singleAdd();
		String legacyJson = "{\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.CREATED);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyCruClient.createMonitoringLocation(anyString())).willReturn(legacyRtn);


		String rtn = service.completeWorkflow(file, response);
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
		JSONAssert.assertEquals(legacyJson, rtn, JSONCompareMode.STRICT);

		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(legacyCruClient).createMonitoringLocation(anyString());
	}

	@Test
	public void oneUpdateTransaction_thenReturnUpdated() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<?,?>> ddotRtn = DdotServiceTest.singleUpdate();
		String legacyJson = "{\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyCruClient.updateMonitoringLocation(anyString(), anyString())).willReturn(legacyRtn);


		String rtn = service.completeWorkflow(file, response);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONAssert.assertEquals(legacyJson, rtn, JSONCompareMode.STRICT);

		verify(ddotService).parseDdot(any(MultipartFile.class));
		verify(legacyCruClient).updateMonitoringLocation(anyString(), anyString());
	}

	@Test
	public void ddotValidation_callsDdot() {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		given(ddotService.parseDdot(any(MultipartFile.class))).willReturn(null);

		assertEquals("{}", service.ddotValidation(file, response));
		verify(ddotService).parseDdot(any(MultipartFile.class));
	}

}
