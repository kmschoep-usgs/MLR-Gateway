package gov.usgs.wma.mlrgateway.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.config.WaterAuthJwtConverter;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GetSecurityContext {
	
	private Logger log = LoggerFactory.getLogger(GetSecurityContext.class);
	
	@Autowired
	public GetSecurityContext(){
	}

	private Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	
	public String getUserName() {
		String userName = "Unknown";
		if(authentication != null){
			userName = authentication.getName();
		} else {
			log.warn("No Authentication present in the Web Security Context when getting user name.");
		}
		return userName;
	}

	
	public String getUserEmail() {
		String userEmail = "";
		if(authentication != null){
			Map<String, Serializable> oauthExtensions = ((OAuth2Authentication) authentication).getOAuth2Request().getExtensions();
			userEmail = (String)oauthExtensions.get(WaterAuthJwtConverter.EMAIL_JWT_KEY);
		} else {
			log.warn("No Authentication present in the Web Security Context when getting user email!");
		}
		return userEmail;
	}
}
