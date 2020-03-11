package gov.usgs.wma.mlrgateway.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.BaseSpringTest;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.service.PreVerificationService;
import gov.usgs.wma.mlrgateway.service.PreVerificationServiceTest;

import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;


@RunWith(SpringRunner.class)
public class UtilControllerTest extends BaseSpringTest {

	@MockBean
	private PreVerificationService preVerificationService;

	private UtilController controller;
	private MockHttpServletResponse response;

	@Before
	public void init() {
		controller = new UtilController(preVerificationService);
		response = new MockHttpServletResponse();
	}

	@Test
	public void happyPath_LegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		List<Map<String, Object>> ddotRtn = PreVerificationServiceTest.multipleDistrictCodes();
		given(preVerificationService.parseDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		 
		Map<String, Set<String>> rtn = controller.parseWorkflow(file, response);
		assertEquals(2, rtn.get("districtCodes").size());
		assertTrue(rtn.get("districtCodes").contains("01"));
		assertTrue(rtn.get("districtCodes").contains("55"));
		verify(preVerificationService).parseDdot(any(MultipartFile.class));
	}

	@Test
	public void badBackingServiceRequest_LegacyWorkflow() throws Exception {
		String badText = "This is really bad.";
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		willThrow(new FeignBadResponseWrapper(500, null, badText)).given(preVerificationService).parseDdot(any(MultipartFile.class));

		Map<String, Set<String>> rtn = controller.parseWorkflow(file, response);
		
		verify(preVerificationService).parseDdot(any(MultipartFile.class));
	}

}
