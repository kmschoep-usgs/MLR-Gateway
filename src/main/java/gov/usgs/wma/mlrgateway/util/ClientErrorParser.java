package gov.usgs.wma.mlrgateway.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ClientErrorParser {
	
	private Logger log = LoggerFactory.getLogger(ClientErrorParser.class);
	
	@Autowired
	public ClientErrorParser(){
	}

	public String parseClientError(String key, String clientErrorMessage) {

		ObjectMapper mapper = new ObjectMapper();
		TypeReference<Map<String, List<Object>>> mapType = new TypeReference<Map<String, List<Object>>>() {};
		Map<String, List<Object>> map = new HashMap<>();
		String rtn;
		try {
			map = mapper.readValue(clientErrorMessage, mapType);
			rtn = mapper.writeValueAsString(map.get(key).get(0));
		} catch (IOException e) {
			rtn = clientErrorMessage;
			log.error("An error occurred while trying to parse a client error message: ", e);
		}
		
		return rtn;
	}
}
