package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

@Tag(name="Auth Controller", description="Display")
@RestController
@RequestMapping("/auth")
public class AuthController extends BaseController {

	private UserAuthService userAuthService;

	@Autowired
	public AuthController(UserAuthService userAuthService) {
		this.userAuthService = userAuthService;
	}

	@Operation(description="Return the logged-in user's current short-lived JWT token")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@GetMapping("/jwt")
	public String getJwt(Authentication auth) {
		return userAuthService.getTokenValue(auth);
	}

	@Operation(description="Return the logged-in user's X-Auth-Token")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@GetMapping("/token")
	public String getToken() {
		return RequestContextHolder.currentRequestAttributes().getSessionId();
	}

	@Operation(description="Return the user to the new UI, logged in.")
	@ApiResponses(value={
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@GetMapping("/login")
	public void login(Authentication auth, HttpServletResponse response) throws IOException {
		String jwt = getJwt(auth);
		String cacheBreak = String.valueOf(Instant.now().getEpochSecond());
		if (jwt != null && !jwt.isEmpty()) {
			response.sendRedirect(uiDomainName + "?mlrAccessToken=" + getToken() + "&cacheBreak=" + cacheBreak);
		} else {
			response.sendError(HttpStatus.UNAUTHORIZED.value());
		}
	}

	@Operation(description="Route logged-out users back to login")
	@GetMapping("/reauth")
	public void reauth(HttpServletResponse response) throws IOException {
		response.sendRedirect("/auth/login");
	}
}
	
