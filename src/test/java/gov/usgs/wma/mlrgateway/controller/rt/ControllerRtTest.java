package gov.usgs.wma.mlrgateway.controller.rt;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.test.context.junit4.SpringRunner;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import gov.usgs.wma.mlrgateway.Application;
import gov.usgs.wma.mlrgateway.config.MethodSecurityConfig;
import gov.usgs.wma.mlrgateway.config.MultipartSupportConfig;
import gov.usgs.wma.mlrgateway.config.WebSecurityConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(
		classes={Application.class,
				WebSecurityConfig.class,
				MultipartSupportConfig.class,
				MethodSecurityConfig.class},
		webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties={"maintenanceRoles=ROLE_DBA_55",
				"security.oauth2.resource.jwt.keyValue=secret",
				"authPublicKeyUrl=",
				"security.require-ssl=false",
				"server.ssl.enabled=false"}
	)
public abstract class ControllerRtTest {

	@Autowired
	protected TestRestTemplate restTemplate;

	@LocalServerPort
	protected int port;

	@Autowired
	AuthorizationServerTokenServices tokenservice;
	@Autowired
	JwtAccessTokenConverter jwtAccessTokenConverter;

	@Value("${security.oauth2.resource.jwt.keyValue}")
	private String secret;

	public static final String KNOWN_USER = "knownusr";

	public static String createToken(String username, String email, String ... roles) throws Exception {
		String jwt = JWT.create()
				.withClaim("user_name", username)
				.withArrayClaim("authorities", roles)
				.withClaim("email", email)
				.sign(Algorithm.HMAC256("secret"))
				;
		return jwt;
	}

	public static HttpHeaders getAuthorizedHeaders(MediaType mediaType) {
		return getHeaders(mediaType, KNOWN_USER, "known@usgs.gov", "ROLE_DBA_55");
	}

	public static HttpHeaders getUnauthorizedHeaders(MediaType mediaType) {
		return getHeaders(mediaType, KNOWN_USER, "known@usgs.gov", "ROLE_UNKNOWN");
	}

	public static HttpHeaders getHeaders(MediaType mediaType, String username, String email, String ... roles) {
		HttpHeaders headers = getHeaders(mediaType);
		try {
			headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(username, email, roles));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return headers;
	}

	public static HttpHeaders getHeaders(MediaType mediaType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		return headers;
	}

}
