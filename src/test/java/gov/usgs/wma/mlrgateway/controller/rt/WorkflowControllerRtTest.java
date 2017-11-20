package gov.usgs.wma.mlrgateway.controller.rt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

public class WorkflowControllerRtTest extends ControllerRtTest {

	@Test
	public void postNotAuthenticatedTriesToAuthenticate() {
		LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
		parameters.add("file", new ClassPathResource("testData/test.ddot"));

		HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, getHeaders(MediaType.MULTIPART_FORM_DATA));
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/workflows/ddots", entity, String.class);

		assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
		assertEquals("http://localhost:" + port + "/login", responseEntity.getHeaders().get("Location").get(0));
	}

	@Test
	public void postNotAuthorizedThrowsUnauthorized() {
		LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
		parameters.add("file", new ClassPathResource("testData/test.ddot"));

		HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, getUnauthorizedHeaders(MediaType.MULTIPART_FORM_DATA));
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/workflows/ddots", entity, String.class);

		assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
	}

	@Test
	public void postValidateNotAuthenticatedTriesToAuthenticate() {
		LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
		parameters.add("file", new ClassPathResource("testData/test.ddot"));

		HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, getHeaders(MediaType.MULTIPART_FORM_DATA));
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/workflows/ddots/validate", entity, String.class);

		assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
		assertEquals("http://localhost:" + port + "/login", responseEntity.getHeaders().get("Location").get(0));
	}

}
