package gov.usgs.wma.mlrgateway.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.DdotClient;

@RunWith(SpringRunner.class)
public class PreVerificationServiceTest extends BaseSpringTest {

	@MockBean
	private DdotClient ddotClient;

	private PreVerificationService service;

	@Before
	public void init() {
		service = new PreVerificationService(ddotClient);
	}

	@Test
	public void garbageFromDdot_thenReturnInternalServerError() throws Exception {

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		ResponseEntity<String> ddotRtn = new ResponseEntity<> ("not json", HttpStatus.OK);
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		try {
			service.parseDdot(file);
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals(DdotService.INTERNAL_ERROR_MESSAGE , ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}

		verify(ddotClient).ingestDdot(any(MultipartFile.class));
	}
	
	@Test
	public void failedDdotValidation_thenReturnInternalServerError() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badFile = "{\"DdotClient#ingestDdot(MultipartFile)\": [{\"error_message\":\"Contains lines exceeding 80 characters: line 3\"}\n]}";
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("thing", "value");
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willThrow(new FeignBadResponseWrapper(500, httpHeaders, badFile));
		try {
			service.parseDdot(file);
			fail("Did not get expected Exception.");
		} catch (FeignBadResponseWrapper e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals(DdotService.INTERNAL_ERROR_MESSAGE , ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}

		verify(ddotClient).ingestDdot(any(MultipartFile.class));
	}

	@Test
	public void happyPath() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		ObjectMapper mapper = new ObjectMapper();
		ResponseEntity<String> ddotRtn = new ResponseEntity<> (mapper.writeValueAsString(singleAdd()), HttpStatus.OK);
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		List<Map<String, Object>> rtn = service.parseDdot(file);
		
		assertEquals(1, rtn.size());
		assertThat(rtn.get(0), is(getAdd()));
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
		
	}

	public static List<Map<String, Object>> singleUnknown() {
		List<Map<String, Object>> lm = new ArrayList<>();
		lm.add(getUnknown());
		return lm;
	}

	public static List<Map<String, Object>> singleAdd() {
		List<Map<String, Object>> lm = new ArrayList<>();
		lm.add(getAdd());
		return lm;
	}

	public static List<Map<String, Object>> singleUpdate() {
		List<Map<String, Object>> lm = new ArrayList<>();
		lm.add(getUpdate());
		return lm;
	}

	public static List<Map<String, Object>> multipleDistrictCodes() {
		List<Map<String, Object>> lm = new ArrayList<>();
		lm.add(getAddMultipleDistrictCodes("1234", "01"));
		lm.add(getAddMultipleDistrictCodes("52346", "55"));
		return lm;
	}

}
