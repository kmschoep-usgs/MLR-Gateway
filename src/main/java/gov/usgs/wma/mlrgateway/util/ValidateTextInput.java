package gov.usgs.wma.mlrgateway.util;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ValidateTextInput {
	private Logger log = LoggerFactory.getLogger(ValidateTextInput.class);
	public static final String INTERNAL_ERROR_MESSAGE = "Invalid characters submitted in reasonText. Only alpha-numeric characters are allowed.";
	
	public String validateInput(String StrInput) {
		Boolean valid = true;
		if (!StrInput.isEmpty()) {
			valid = StrInput.matches("^[a-zA-Z0-9 ]*$");
		}
		if (!valid) {
			log.error(INTERNAL_ERROR_MESSAGE);
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, INTERNAL_ERROR_MESSAGE);
		}
		return StrInput;
	}
}
	

