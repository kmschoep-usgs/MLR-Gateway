package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.service.AdminService;
import gov.usgs.wma.mlrgateway.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Util Controller", description="Display")
@RestController
@RequestMapping("/util")
public class UtilController {
	private Logger log = LoggerFactory.getLogger(UtilController.class);
	private AdminService adminService;
	private UserAuthService userAuthService;

	@NotBlank
	private String serviceAuthToken;

	@Autowired
	public UtilController(AdminService adminService, UserAuthService userAuthService) {
		this.adminService = adminService;
		this.userAuthService = userAuthService;
	}

	@Value("${security.service-auth-token}")
	protected void setServiceAuthToken(String serviceAuthToken) {
		this.serviceAuthToken = serviceAuthToken;
	}

	@Operation(description="Generates and sends a transaction summary email for the provided date")
	@ApiResponses(value={
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@GetMapping("/sendSummaryEmail")
	public void sendSummaryEmail(
		@RequestParam @Pattern(regexp="\\d\\d\\d\\d-\\d\\d-\\d\\d") String date,
		@RequestParam @NotEmpty List<String> recipientList,
		@RequestHeader(name="X-Service-Auth-Token") @NotBlank String authToken,
		HttpServletRequest request,
		HttpServletResponse response
	) throws IOException {
		// Validate Service Auth Token
		if(!serviceAuthToken.equals(authToken)) {
			throw new AccessDeniedException("Missing or invalid Auth header");
		}
		log.info("[SUMMARY EMAIL WORKFLOW]: Starting summary email generation for date: " + date);

		//Generate Access Token
		String bearerToken = userAuthService.getServiceAccountBearerToken(SecurityContextHolder.getContext().getAuthentication(), request, response);

		// Generate and Send Email
		adminService.sendSummaryEmail(date, recipientList, bearerToken);
		response.setStatus(HttpStatus.OK.value());
	}
}