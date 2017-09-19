package gov.usgs.wma.mlrgateway.service;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.DdotClient;

@RunWith(SpringRunner.class)
public class DdotServiceTest extends BaseSpringTest {

	@MockBean
	private DdotClient ddotClient;

	private DdotService service;

	@Before
	public void init() {
		service = new DdotService(ddotClient);
	}

	@Test
	public void garbageFromDdot_thenReturnInternalServerError() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn("not json");
		try {
			service.parseDdot(file);
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals("{\"error\":{\"message\":\"Unable to read ingestor output.\"}}", ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
	}

	@Test
	public void incorrectDdotSize_thenReturnBadRequest() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String ddotResponse = "[{},{}]";
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn(ddotResponse);
		try {
			service.parseDdot(file);
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.BAD_REQUEST.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals("{\"error\":{\"message\":\"Only accepting files with one transaction at this time.\"},\"data\":"
					+ ddotResponse + "}", ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
	}


	@Test
	public void happyPath() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		ObjectMapper mapper = new ObjectMapper();
		String ddotResponse = mapper.writeValueAsString(singleAdd());
		given(ddotClient.ingestDdot(any(MultipartFile.class))).willReturn(ddotResponse);
		List<Map<?,?>> rtn = service.parseDdot(file);
		assertEquals(1, rtn.size());
		assertThat(rtn.get(0), is(singleAdd().get(0)));
		verify(ddotClient).ingestDdot(any(MultipartFile.class));
	}

	public static List<Map<?,?>> singleUnknown() {
		List<Map<?,?>> lm = new ArrayList<>();
		Map<String, Object> m = new HashMap<>();
		m.put("agencyCode", "USGS ");
		m.put("siteNumber", "12345678       ");
		lm.add(m);
		return lm;
	}
	public static List<Map<?,?>> singleAdd() {
		List<Map<?,?>> lm = new ArrayList<>();
		Map<String, Object> m = new HashMap<>();
		m.put("transactionType", "A");
		m.put("agencyCode", "USGS ");
		m.put("siteNumber", "12345678       ");
		lm.add(m);
		return lm;
	}

	public static List<Map<?,?>> singleUpdate() {
		List<Map<?,?>> lm = new ArrayList<>();
		Map<String, Object> m = new HashMap<>();
		m.put("transactionType", "M");
		m.put("agencyCode", "USGS ");
		m.put("siteNumber", "12345678       ");
		lm.add(m);
		return lm;
	}
}
