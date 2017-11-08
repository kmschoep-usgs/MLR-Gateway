package gov.usgs.wma.mlrgateway.controller.rt;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Ignore
public class WorkflowControllerRtTest extends ControllerRtTest {

	@Test
	public void postNotAuthenticatedTriesToAuthenticate() {
		HttpEntity<String> entity = new HttpEntity<String>("", getNoAuthHeaders());
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/workflow/ddots", entity, String.class);

		assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
		assertEquals("https://localhost:" + port + "/login", responseEntity.getHeaders().get("Location").get(0));
	}

	@Test
	public void postNotAuthorizedThrowsUnauthorized() {
		HttpEntity<String> entity = new HttpEntity<String>("", getAuthHeaders("mlr", "ROLE_ABC"));
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/workflow/ddots", entity, String.class);

		assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
	}

	@Test
	public void postValidateNotAuthenticatedTriesToAuthenticate() {
		HttpEntity<String> entity = new HttpEntity<String>("", getNoAuthHeaders());
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/workflow/ddots/validate", entity, String.class);

		assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
		assertEquals("https://localhost:" + port + "/login", responseEntity.getHeaders().get("Location").get(0));
	}

}
