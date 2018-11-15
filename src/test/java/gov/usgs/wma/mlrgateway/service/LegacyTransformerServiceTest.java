package gov.usgs.wma.mlrgateway.service;

import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.client.LegacyTransformerClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import net.minidev.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
public class LegacyTransformerServiceTest extends BaseSpringTest {

	@MockBean
	private LegacyTransformerClient legacyTransformerClient;

	private LegacyTransformerService service;
	private String reportName = "TEST DDOT";
	private ObjectMapper mapper;
	private String legacyJson = "{\"" + LegacyWorkflowService.TRANSACTION_TYPE + "\":\"" + LegacyWorkflowService.TRANSACTION_TYPE_ADD
			+ "\",\"" + LegacyWorkflowService.AGENCY_CODE + "\": \"USGS \",\"" + LegacyWorkflowService.SITE_NUMBER + "\": \"12345678       \"";
	private String jsonGeo = ", \"" + LegacyTransformerService.LATITUDE + "\": \" 400000    \", \"" + LegacyTransformerService.LONGITUDE + "\": \" 1000000    \", \""
			+ LegacyTransformerService.COORDINATE_DATUM_CODE + "\": \"NAD27      \", \"decimalLatitude\" : 40, \"decimalLongitude\": -100";
	private String jsonIX = ", \"" + LegacyTransformerService.STATION_NAME + "\": \"Station#_Name1$\", \"stationIx\" : \"STATIONNAME1\"";
	private String legacyJsonGeo = legacyJson + jsonGeo + "}";
	private String legacyJsonIX = legacyJson + jsonIX + "}";
	private final String transformerJsonIx = "{\"stationIx\" : \"STATIONNAME1\"}";
	private final String transformerJsonGeo = "{\"decimalLatitude\" : 40, \"decimalLongitude\": -100}";
	

	@Before
	public void init() {
		service = new LegacyTransformerService(legacyTransformerClient);
		WorkflowController.setReport(new GatewayReport(reportName));
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath_transformGeo_thenReturnTransformedGeo() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":[{\"name\":\"" + LegacyTransformerService.STEP_NAME + "\",\"status\":200,\"details\":\""
				+ JSONObject.escape(LegacyTransformerService.GEO_SUCCESS) + "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(transformerJsonGeo, HttpStatus.OK);
		given(legacyTransformerClient.decimalLocation(anyString())).willReturn(legacyRtn);

		Map<String, Object> rtn = service.transformGeo(addGeo(getAdd()));

		String actualTransformed = mapper.writeValueAsString(rtn);
		String actualMsg = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(legacyJsonGeo, actualTransformed, JSONCompareMode.STRICT);
		JSONAssert.assertEquals(msg, actualMsg, JSONCompareMode.STRICT);
		verify(legacyTransformerClient).decimalLocation(anyString());
	}

	@Test
	public void nullReturnTransform_transformGeo_thenReturnError() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":[{\"name\":\"" + LegacyTransformerService.STEP_NAME + "\",\"status\":500,\"details\":\""
				+ JSONObject.escape(LegacyTransformerService.GEO_FAILURE) + "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>("", HttpStatus.BAD_REQUEST);
		given(legacyTransformerClient.decimalLocation(anyString())).willReturn(legacyRtn);

		try {
			Map<String, Object> rtn = service.transformGeo(addGeo(getAdd()));
			fail("transformGeo did not throw an error");
		} catch(FeignBadResponseWrapper e) {}

		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyTransformerClient).decimalLocation(anyString());
	}

	@Test
	public void happyPath_transformIX_thenReturnTransformedGeo() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":[{\"name\":\"" + LegacyTransformerService.STEP_NAME + "\",\"status\":200,\"details\":\""
				+ JSONObject.escape(LegacyTransformerService.STATION_IX_SUCCESS) + "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(transformerJsonIx, HttpStatus.OK);
		given(legacyTransformerClient.stationIx(anyString())).willReturn(legacyRtn);

		Map<String, Object> rtn = service.transformStationIx(addIX(getAdd()));

		JSONAssert.assertEquals(legacyJsonIX, mapper.writeValueAsString(rtn), JSONCompareMode.STRICT);
		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyTransformerClient).stationIx(anyString());
	}

	@Test
	public void nullReturnTransform_transformIX_thenReturnError() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":500,\"steps\":[{\"name\":\"" + LegacyTransformerService.STEP_NAME + "\",\"status\":500,\"details\":\""
				+ JSONObject.escape(LegacyTransformerService.STATION_IX_FAILURE) + "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>("", HttpStatus.BAD_REQUEST);
		given(legacyTransformerClient.decimalLocation(anyString())).willReturn(legacyRtn);
		
		try {
			Map<String, Object> rtn = service.transformStationIx(addIX(getAdd()));
			fail("transformStationIx did not throw an error");
		} catch(FeignBadResponseWrapper e) {}

		JSONAssert.assertEquals(msg, mapper.writeValueAsString(WorkflowController.getReport()), JSONCompareMode.STRICT);
		verify(legacyTransformerClient).stationIx(anyString());
	}

	@Test
	public void happyPathIx_transform_thenReturnTransformed() throws Exception {
		String msg = "{\"name\":\"" + reportName + "\",\"status\":200,\"steps\":[{\"name\":\"" + LegacyTransformerService.STEP_NAME + "\",\"status\":200,\"details\":\""
				+ JSONObject.escape(LegacyTransformerService.STATION_IX_SUCCESS) + "\",\"agencyCode\": \"USGS \",\"siteNumber\": \"12345678       \"}]}";
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(transformerJsonIx, HttpStatus.OK);
		given(legacyTransformerClient.stationIx(anyString())).willReturn(legacyRtn);

		Map<String, Object> rtn = service.transformStationIx(addIX(getAdd()));

		String actualTransformed = mapper.writeValueAsString(rtn);
		JSONAssert.assertEquals(legacyJsonIX, actualTransformed, JSONCompareMode.STRICT);
		String actualMsg = mapper.writeValueAsString(WorkflowController.getReport());
		JSONAssert.assertEquals(msg, actualMsg, JSONCompareMode.STRICT);
		verify(legacyTransformerClient, never()).decimalLocation(anyString());
		verify(legacyTransformerClient).stationIx(anyString());
	}
	
	@Test
	public void ifLocationLacksStationName_thenReturnAsIs() {
		Map<String, Object> transformed = service.transformStationIx(addGeo(getAdd()));
		assertEquals(addGeo(getAdd()), transformed);
	}
	
	@Test
	public void ifLocationLacksGeo_thenReturnAsIs() {
		Map<String, Object> transformed = service.transformGeo(addIX(getAdd()));
		assertEquals(addIX(getAdd()), transformed);
	}
	
	public static Map<String, Object> addGeo(Map<String, Object> baseMap) {
		Map<String, Object> rtn = new HashMap<>();
		rtn.putAll(baseMap);
		rtn.put(LegacyTransformerService.LATITUDE, " 400000    ");
		rtn.put(LegacyTransformerService.LONGITUDE, " 1000000    ");
		rtn.put(LegacyTransformerService.COORDINATE_DATUM_CODE, "NAD27      ");
		return rtn;
	}

	public static Map<String, Object> addIX(Map<String, Object> baseMap) {
		Map<String, Object> rtn = new HashMap<>();
		rtn.putAll(baseMap);
		rtn.put(LegacyTransformerService.STATION_NAME, "Station#_Name1$");
		return rtn;
	}

}
