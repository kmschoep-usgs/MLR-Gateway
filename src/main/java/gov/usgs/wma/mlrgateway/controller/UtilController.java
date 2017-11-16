package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.util.ConfigurationValues;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags={"Util Controller"})
@RestController
@RequestMapping("/util")
public class UtilController extends BaseController {	
	@Autowired
	public UtilController() {
		super();
	}

	@ApiOperation(value="Return the logged-in user's JWT token")
	@ApiResponses(value={@ApiResponse(code=200, message="OK"),
			@ApiResponse(code=400, message="Bad Request"),
			@ApiResponse(code=401, message="Unauthorized"),
			@ApiResponse(code=403, message="Forbidden")})
	@GetMapping("/token")
	public String getToken() {
		if(!environmentTier.equals(ConfigurationValues.ENVIRONMENT_PRODUCTION)) {
			if(SecurityContextHolder.getContext().getAuthentication() != null){
				String jwtToken = ((OAuth2AuthenticationDetails) ((OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication()).getDetails()).getTokenValue();
				return jwtToken;
			}
		}
		
		return null;
	}
}
	
