package gov.usgs.wma.mlrgateway.controller.rt;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.test.context.junit4.SpringRunner;

import gov.usgs.wma.mlrgateway.Application;
import gov.usgs.wma.mlrgateway.config.WebSecurityConfig;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes={Application.class, WebSecurityConfig.class}, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ControllerRtTest {

	@Autowired
	protected TestRestTemplate restTemplate;

	@LocalServerPort
	protected int port;

	@Autowired
	AuthorizationServerTokenServices tokenservice;
	@Autowired
	JwtAccessTokenConverter jwtAccessTokenConverter;

	public String addBearerToken(final String username, String... authorities) {
		OAuth2Request oauth2Request = new OAuth2Request(null, "testClient", null, true, null, null, null, null, null);
		Authentication userauth = new TestingAuthenticationToken(username, null, authorities);
		OAuth2Authentication oauth2auth = new OAuth2Authentication(oauth2Request, userauth);
		OAuth2AccessToken token = tokenservice.createAccessToken(oauth2auth);
		return jwtAccessTokenConverter.enhance(token, oauth2auth).getValue();
	}

	public HttpHeaders getNoAuthHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		return headers;
	}

	public HttpHeaders getAuthHeaders(final String username, String... authorities) {
		HttpHeaders headers = getNoAuthHeaders();
		headers.add("Authorization", "Bearer " + addBearerToken(username, authorities));
		return headers;
	}
}
