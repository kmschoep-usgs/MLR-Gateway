package gov.usgs.wma.mlrgateway.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;

@RunWith(SpringRunner.class)
@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(secure=false)
public class WorkflowControllerTest {

	@Autowired
	private MockMvc mvc;

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
	public void noTransactionType_thenReturnBadRequest() throws Exception {
		String ddotRtn = getCompareFile("testData/", "noTransactionType.json");
		given(ddotClient.injestDdot(any(MultipartFile.class))).willReturn(ddotRtn);

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflow/ddot")
				.file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("{\"error\":{\"message\":\"Unable to determine transactionType.\"},\"data\":" + ddotRtn + "}"));
	}

	@Test
	public void multipleTransactions_thenReturnBadRequest() throws Exception {
		String ddotRtn = getCompareFile("testData/", "multiple.json");
		given(ddotClient.injestDdot(any(MultipartFile.class))).willReturn(ddotRtn);

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflow/ddot")
				.file(file))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("{\"error\":{\"message\":\"Only accepting files with one transaction at this time.\"},\"data\":" + ddotRtn + "}"));
	}

	@Test
	public void oneAddTransaction_thenReturnCreated() throws Exception {
		String ddotRtn = getCompareFile("testData/", "oneAdd.json");
		String legacyJson = getCompareFile("testResult/", "oneAdd.json");
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.CREATED);
		given(ddotClient.injestDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyClient.createMonitoringLocation(anyString())).willReturn(legacyRtn);

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflow/ddot")
				.file(file))
				.andExpect(status().isCreated())
				.andExpect(content().string(legacyJson));
	}

	@Test
	public void oneUpdateTransaction_thenReturnUpdated() throws Exception {
		String ddotRtn = getCompareFile("testData/", "oneUpdate.json");
		String legacyJson = getCompareFile("testResult/", "oneUpdate.json");
		ResponseEntity<String> legacyRtn = new ResponseEntity<String>(legacyJson, HttpStatus.OK);
		given(ddotClient.injestDdot(any(MultipartFile.class))).willReturn(ddotRtn);
		given(legacyClient.updateMonitoringLocation(anyString(), anyString())).willReturn(legacyRtn);

		MockMultipartFile file = new MockMultipartFile("file", "d.", "text/plain", "".getBytes());

		mvc.perform(MockMvcRequestBuilders.fileUpload("/workflow/ddot")
				.file(file))
				.andExpect(status().isOk())
				.andExpect(content().string(legacyJson));
	}

}
