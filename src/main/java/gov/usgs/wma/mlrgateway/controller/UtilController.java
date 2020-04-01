package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.service.PreVerificationService;
import gov.usgs.wma.mlrgateway.util.ConfigurationValues;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name="Util Controller", description="Display")
@RestController
@RequestMapping("/util")
public class UtilController extends BaseController {
	private PreVerificationService preVerificationService;
	@Autowired
	public UtilController(PreVerificationService preVerificationService) {
		super();
		this.preVerificationService = preVerificationService;
	}

	@Operation(description="Return the logged-in user's JWT token")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@GetMapping("/token")
	public String getToken() {
		if(environmentTier != null && !environmentTier.equals(ConfigurationValues.ENVIRONMENT_PRODUCTION)) {
			if(SecurityContextHolder.getContext().getAuthentication() != null){
				String jwtToken = ((OAuth2AuthenticationDetails) ((OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication()).getDetails()).getTokenValue();
				return jwtToken;
			}
		}
		
		return null;
	}
	
	@Operation(description="Parse ddot file and return the list of district codes")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "400", description = "Bad Request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden") })
	@PostMapping("/parse")
	public Map<String, Set<String>> parseWorkflow(@RequestPart MultipartFile file, HttpServletResponse response) throws IOException {
		Map<String, Set<String>> parsedReturn = new HashMap<>();
		try {
			List<Map<String, Object>> ddots = preVerificationService.parseDdot(file);
			
			Set<String> districtCodes = ddots.stream()
					.map(d -> d.get("districtCode") != null ? d.get("districtCode").toString() : null)
					.collect( Collectors.toSet() );
			
			Set<String> transactionTypes = ddots.stream()
					.map(d -> d.get("transactionType") != null ? d.get("transactionType").toString() : null)
					.collect( Collectors.toSet() );
			
			parsedReturn.put("districtCodes", districtCodes);
			parsedReturn.put("transactionTypes", transactionTypes);
			
		} catch (Exception e) {
			int status = (e instanceof FeignBadResponseWrapper) ? ((FeignBadResponseWrapper) e).getStatus() : HttpStatus.SC_INTERNAL_SERVER_ERROR;
			response.sendError(status, "DDot Ingester error, Parse Failed");
		}
		return parsedReturn;
	}
}
	
