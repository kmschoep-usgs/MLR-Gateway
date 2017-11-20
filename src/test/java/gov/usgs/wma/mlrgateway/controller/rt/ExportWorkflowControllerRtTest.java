package gov.usgs.wma.mlrgateway.controller.rt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class ExportWorkflowControllerRtTest extends ControllerRtTest {

	@Test
	public void postNotAuthenticatedTriesToAuthenticate() {
		HttpEntity<String> entity = new HttpEntity<>("", getHeaders(MediaType.APPLICATION_JSON_UTF8));
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/legacy/location/usgs/12345678", entity, String.class);

		assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
		assertEquals("http://localhost:" + port + "/login", responseEntity.getHeaders().get("Location").get(0));
	}

	@Test
	public void postNotAuthorizedThrowsUnauthorized() {
		HttpEntity<String> entity = new HttpEntity<>("", getUnauthorizedHeaders(MediaType.APPLICATION_JSON_UTF8));
		ResponseEntity<String> responseEntity = restTemplate.postForEntity("/legacy/location/usgs/12345678", entity, String.class);

		assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
	}

}
