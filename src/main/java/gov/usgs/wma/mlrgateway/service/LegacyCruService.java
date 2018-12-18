package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.util.ClientErrorParser;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LegacyCruService {

	private final LegacyCruClient legacyCruClient;
	private ClientErrorParser clientErrorParser;
	private Logger log = LoggerFactory.getLogger(LegacyCruService.class);
	private ObjectMapper objectMapper;
	
	public static final String SITE_ADD_STEP = "Site Add";
	public static final String SITE_ADD_SUCCESSFUL = "Site Added Successfully";
	public static final String SITE_ADD_FAILED = "{\"error_message\": \"Site add failed\"}";
	public static final String SITE_UPDATE_STEP = "Site Update";
	public static final String SITE_UPDATE_SUCCESSFUL = "Site Updated Successfully.";
	public static final String SITE_UPDATE_FAILED = "{\"error_message\": \"Site update failed\"}";
	public static final String SITE_GET_STEP = "Location Get by AgencyCode and SiteNumber";
	public static final String SITE_GET_SUCCESSFULL = "Location Get Successful";
	public static final String SITE_GET_DOES_NOT_EXIST_FAILED = "{\"error_message\": \"Requested Location Not Found\"}";
	public static final String SITE_GET_DOES_NOT_EXIST_SUCCESSFUL = "Requested Location Not Found";
	public static final String SITE_GET_STEP_FAILED = "{\"error_message\": \"Unable to read Legacy CRU output.\"}";
	public static final String SITE_NAME_GET_STEP = "Location Get by Name";
	public static final String SITE_NAME_GET_SUCCESSFUL = "Location Get by Name Succeeded";
	public static final String SITE_NAME_GET_FAILED = "{\"error_message\": \"Location Get by Name Failed\"}";
	public static final String SITE_GET_NAME_DOES_EXIST = "Duplicate Location Names Found";

	public LegacyCruService(LegacyCruClient legacyCruClient) {
		this.legacyCruClient = legacyCruClient;
		this.objectMapper = new ObjectMapper();
	}
	
	public String addTransaction(Object agencyCode, Object siteNumber, String json, SiteReport siteReport) {
		clientErrorParser = new ClientErrorParser();
		try {
			ResponseEntity<String> cruResp = legacyCruClient.createMonitoringLocation(json);
			int cruStatus = cruResp.getStatusCodeValue();
			siteReport.addStepReport(new StepReport(SITE_ADD_STEP, cruStatus, 201 == cruStatus ? true : false, 201 == cruStatus ? SITE_ADD_SUCCESSFUL : cruResp.getBody()));
			return cruResp.getBody();
		} catch (Exception e) {
			if(e instanceof FeignBadResponseWrapper){
				siteReport.addStepReport(new StepReport(SITE_ADD_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, clientErrorParser.parseClientError("LegacyCruClient#createMonitoringLocation(String)", ((FeignBadResponseWrapper)e).getBody())));
			} else {
				siteReport.addStepReport(new StepReport(SITE_ADD_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, SITE_ADD_FAILED));
			}
			log.error(SITE_ADD_STEP + ": " + SITE_ADD_FAILED + ":" +  e.getMessage());			
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, SITE_ADD_FAILED);	
		}
	}
	
	public String updateTransaction(Object agencyCode, Object siteNumber, String json, SiteReport siteReport) {
		try {
			ResponseEntity<String> cruResp = legacyCruClient.patchMonitoringLocation(json);
			int cruStatus = cruResp.getStatusCodeValue();
			siteReport.addStepReport(new StepReport(SITE_UPDATE_STEP, cruStatus, 200 == cruStatus ? true : false, 200 == cruStatus ? SITE_UPDATE_SUCCESSFUL : SITE_UPDATE_FAILED));
			return cruResp.getBody();
		} catch (Exception e){
			siteReport.addStepReport(new StepReport(SITE_UPDATE_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, SITE_UPDATE_FAILED));
			log.error(SITE_UPDATE_STEP + ": " + SITE_UPDATE_FAILED + ":" +  e.getMessage());			
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, SITE_UPDATE_FAILED);
		}
	}
	
	public Map<String, Object> getMonitoringLocation(Object agencyCode, Object siteNumber, boolean isAddTransaction, SiteReport siteReport) {
		Map<String, Object> site = new HashMap<>();

		ResponseEntity<String> cruResp = legacyCruClient.getMonitoringLocation((String) agencyCode, (String) siteNumber);
		int cruStatus = cruResp.getStatusCodeValue();
		boolean isSuccess = true;
		String preValMsg = "";
		if (cruStatus == 404) {
			if (isAddTransaction) {
				isSuccess = true;
				preValMsg = "Duplicate agency code/site number check: ";
				siteReport.addStepReport(new StepReport(preValMsg + SITE_GET_STEP, cruStatus, isSuccess, SITE_GET_DOES_NOT_EXIST_SUCCESSFUL));
			} else {
				isSuccess = false;
				siteReport.addStepReport(new StepReport(preValMsg + SITE_GET_STEP, cruStatus, isSuccess, SITE_GET_DOES_NOT_EXIST_FAILED));
			}
			
  		} else {

			try {
				site = objectMapper.readValue(cruResp.getBody(), Map.class);
			} catch (Exception e) {
				siteReport.addStepReport(new StepReport(SITE_GET_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, e.getMessage()));
				log.error(SITE_GET_STEP + ": " + SITE_GET_STEP_FAILED + ":" +  e.getMessage());			
				throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, SITE_GET_STEP_FAILED);
			}

			siteReport.addStepReport(new StepReport(SITE_GET_STEP, cruStatus, 200 == cruStatus ? true : false, 200 == cruStatus ? SITE_GET_SUCCESSFULL : cruResp.getBody()));
		}
		return site;
	}

	/**
	 *
	 * @param ml
	 * @return a List of String error messages. Empty if there were no
	 * validation errors.
	 */
	public String validateMonitoringLocation(Map<String, Object> ml, SiteReport siteReport) {
		String stepReportMessage = "";
		String mlJson = "";

		try {
			mlJson = objectMapper.writeValueAsString(ml);
		} catch (JsonProcessingException ex) {
			siteReport.addStepReport(new StepReport(SITE_NAME_GET_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, SITE_NAME_GET_FAILED));
			log.error(SITE_NAME_GET_STEP + ": " + SITE_NAME_GET_FAILED, ex);
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, SITE_NAME_GET_FAILED);
		}

		ResponseEntity<String> response = legacyCruClient.validateMonitoringLocation(mlJson);
		int cruStatus = response.getStatusCodeValue();
		
		if (200 == cruStatus) {
			stepReportMessage = SITE_GET_DOES_NOT_EXIST_SUCCESSFUL;
		} else {
			stepReportMessage = response.getBody();
		}
		siteReport.addStepReport(new StepReport(SITE_NAME_GET_STEP, cruStatus, 200 == cruStatus ? true : false, stepReportMessage));

		return response.getBody();
	}

	public ObjectMapper getMapper() {
		return objectMapper;
	}

	public void setMapper(ObjectMapper mapper) {
		this.objectMapper = mapper;
	}
}
