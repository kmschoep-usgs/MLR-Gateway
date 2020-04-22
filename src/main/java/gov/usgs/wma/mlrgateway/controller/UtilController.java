package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.util.UserAuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
		return userAuthUtil.getTokenValue(SecurityContextHolder.getContext().getAuthentication());
	}

	@Operation(description="Return the user to the new UI, logged in.")
	@ApiResponses(value={
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@GetMapping("/login")
	public void login(HttpServletResponse response) throws IOException {
		String tokenValue = userAuthUtil.getTokenValue(SecurityContextHolder.getContext().getAuthentication());

		if (tokenValue != null && !tokenValue.isEmpty()) {
            response.sendRedirect(uiDomainName + "?mlrAccessToken=" + tokenValue);
        } else {
			response.sendError(HttpStatus.SC_UNAUTHORIZED);
		}
	}

}
	
