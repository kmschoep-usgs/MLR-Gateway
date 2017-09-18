package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.service.LegacyWorkflowService;

@RunWith(SpringRunner.class)
@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(secure=false)
public class WorkflowControllerMVCTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private LegacyWorkflowService legacy;

	@MockBean
	private DdotClient ddotClient;

	@MockBean
	private LegacyCruClient legacyClient;

	@MockBean
	private DiscoveryClient dc;

	@MockBean
	private SpringClientFactory scf;

	public String getCompareFile(String folder, String file) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(new ClassPathResource(folder + file).getInputStream()));
	}

	@Test
	public void happyPathLegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String legacyJson = getCompareFile("testData/", "oneAdd.json");
		given(legacy.completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class))).willReturn(legacyJson);

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots")
				.file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(legacyJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void badDdot_LegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badJson = "{\"error\": \"bad Ddot\"}";
		given(legacy.completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class))).willThrow(new FeignBadResponseWrapper(400, null, badJson));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots")
				.file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badJson));

		verify(legacy).completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void serverError_LegacyWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badMsg = "{\"error\": \"bad stuff\"}";
		given(legacy.completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class))).willThrow(new HystrixBadRequestException(badMsg));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots")
				.file(file))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badMsg));

		verify(legacy).completeWorkflow(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void happyPathLegacyValidationWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String legacyJson = getCompareFile("testData/", "oneAdd.json");
		given(legacy.ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class))).willReturn(legacyJson);

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots/validate")
				.file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(legacyJson));

		verify(legacy).ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void badDdot_LegacyValidationWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badJson = "{\"error\": \"bad Ddot\"}";
		given(legacy.ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class))).willThrow(new FeignBadResponseWrapper(400, null, badJson));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots/validate")
				.file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badJson));

		verify(legacy).ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class));
	}

	@Test
	public void serverError_LegacyValidationWorkflow() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());
		String badMsg = "{\"error\": \"bad stuff\"}";
		given(legacy.ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class))).willThrow(new HystrixBadRequestException(badMsg));

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflows/ddots/validate")
				.file(file))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(content().string(badMsg));

		verify(legacy).ddotValidation(any(MultipartFile.class), any(HttpServletResponse.class));
	}

}
