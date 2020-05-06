package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.util.UserAuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.time.Instant;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

@Tag(name="Util Controller", description="Display")
@RestController
@RequestMapping("/util")
public class UtilController extends BaseController {

    @Autowired
    UserAuthUtil userAuthUtil;

	@Autowired
	public UtilController() {
		super();
	}

	@Operation(description="Return the logged-in user's JWT token")
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
	public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		OAuth2AccessToken token = userAuthUtil.getRefreshAccessToken(auth);
		String cacheBreak = String.valueOf(Instant.now().getEpochSecond());
		if (token != null && !token.getTokenValue().isEmpty()) {
            response.sendRedirect(uiDomainName + "?mlrAccessToken=" + getToken() + "&cacheBreak=" + cacheBreak);
        } else {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
		}
	}
}
	
