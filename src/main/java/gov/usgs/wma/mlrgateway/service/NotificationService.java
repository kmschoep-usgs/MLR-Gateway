package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.SiteReport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.HashMap;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
public class NotificationService {

	private NotificationClient notificationClient;
	private Logger log = LoggerFactory.getLogger(NotificationService.class);
	
	@Value("${environmentTier:}")
	private String environmentTier;
	
	public static final String NOTIFICATION_STEP = "Notification";
	public static final String NOTIFICATION_SUCCESSFULL = "Notification sent successfully.";
	public static final String NOTIFICATION_FAILURE = "{\"error_message\": \"Notification failed to send.\"}";
	public static Temporal reportDateTime;
	public static final String ATTACHMENT_FILE_NAME = "mlr-%NAME%-report.json";
	public enum MessageType {error, warning};
	public static final String ERROR_MESSAGE = "error_message";
	public static final String FATAL_ERROR_MESSAGE = "fatal_error_message";
	public static final String WARNING_MESSAGE = "warning_message";
	public static final String VALIDATOR_MESSAGE = "validator_message";
	
	@Autowired
	public NotificationService(NotificationClient notificationClient){
		this.notificationClient = notificationClient;
	}

	public void sendNotification(List<String> recipientList, String subject, String user, String attachmentFileName, UserSummaryReport report) {
		ObjectMapper mapper = new ObjectMapper();
		String messageJson;
		report.setReportDateTime(Instant.now().toString());
		HashMap<String, Object> messageMap = buildRequestMap(recipientList, subject, user, attachmentFileName, report);

		try {
			messageJson = mapper.writeValueAsString(messageMap);
			ResponseEntity<String> notifResp = notificationClient.sendEmail(messageJson);
			BaseController.addWorkflowStepReport(new StepReport(NOTIFICATION_STEP, notifResp.getStatusCodeValue(), true, NOTIFICATION_SUCCESSFULL));
		} catch(Exception e) {
			BaseController.addWorkflowStepReport(new StepReport(NOTIFICATION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, NOTIFICATION_FAILURE));
			log.error(NOTIFICATION_STEP + ": " + e.getMessage());
		}
	}

	protected HashMap<String, Object> buildRequestMap(List<String> recipientList, String subject, String user, String attachmentFileName, UserSummaryReport report) {
		HashMap<String, Object> messageMap = new HashMap<>();

		//Build Request
		messageMap.put(NotificationClient.MESSAGE_TO_KEY, recipientList);
		messageMap.put(NotificationClient.MESSAGE_SUBJECT_KEY, subject);
		messageMap.put(NotificationClient.MESSAGE_TEXT_BODY_KEY, buildMessageBody(report, user));
		messageMap.put(NotificationClient.MESSAGE_ATTACHMENT_KEY, report.toPrettyPrintString());
		messageMap.put(NotificationClient.MESSAGE_ATTACHMENT_FILE_NAME_KEY, buildAttachmentName(attachmentFileName));

		return messageMap;
	}
	
	protected String buildMessageBody(UserSummaryReport report, String user) {
		report.setUserName(user);
		String reportBody = "An MLR Workflow has completed on the " + 
				environmentTier + " environment. The workflow output report is below.\n\n\n";
		reportBody += "User:        " + user + "\n";
		reportBody += "Workflow:    " + report.getName() + "\n";
		reportBody += "Report Date: " + report.getReportDateTime() + "\n"; 		
		reportBody += "The full, raw report output is attached.\n\n";
		reportBody += buildErrorReport(report); 
		
		return reportBody;
	}

	protected String buildAttachmentName(String attachmentFileName) {
		return ATTACHMENT_FILE_NAME.replace("%NAME%", 
			(attachmentFileName != null && !attachmentFileName.isEmpty()) ? attachmentFileName : "output"
		); 
	}
	
	protected String buildErrorReport(UserSummaryReport report){
		String errorReport = "";
		String workflowFailureMsg = "";
		String siteMsg = "";
		if (report.getName() == "Complete Export Workflow"){
			StepReport workflowFailureStep = report.getWorkflowSteps().stream()
					.filter(w -> w.getName() == "Complete Export Workflow" && w.isSuccess() == false)
					.collect(Collectors.toList()).get(0);
			workflowFailureMsg = workflowFailureStep.getName() + " Failed: " + getDetailErrorMessage(workflowFailureStep.getDetails()) + "\n\n";
			errorReport += workflowFailureMsg;
		} else {
			if (report.getWorkflowSteps().size() > 0){
				List<StepReport> workflowFailureSteps = report.getWorkflowSteps().stream()
						.filter(w -> w.getName().contains("workflow"))
						.collect(Collectors.toList());
				if (workflowFailureSteps.size() > 0) {
					workflowFailureMsg = workflowFailureSteps.get(0).getName() + " (" + getDetailErrorMessage(workflowFailureSteps.get(0).getDetails()) + ") : No Transactions were processed.\n\nError details listed below:\n\n";
				} 
				List<StepReport> workflowErrorSteps = report.getWorkflowSteps().stream()
						.filter(w -> !w.getName().contains("workflow"))
						.collect(Collectors.toList());
				if (workflowErrorSteps.size() > 0) {
					workflowFailureMsg += "Workflow-level Errors:\n\n";
					for (StepReport w: workflowErrorSteps) {
						workflowFailureMsg += w.getName() + ": " + getDetailErrorMessage(w.getDetails()) + "\n";
					};
				}
			}
			if (workflowFailureMsg != "") {
				errorReport += workflowFailureMsg;
			} else {
				errorReport += "Status:  " + report.getNumberSiteSuccess().toString() + " Transactions Succeeded, " + report.getNumberSiteFailure().toString() + " Transactions Failed\n\n";
			}
		
			if (report.getSites().size() > 0) {
				siteMsg = parseSiteErrors(report.getSites());
				errorReport += siteMsg;
			}
		}
		
		return errorReport;
	}
	
	protected String parseSiteErrors(List<SiteReport> siteErrors) {
		String siteReportRows = "";
		String errorRow = "";
		String warningRow = "";
		String siteAgencyNumber = "";
		for (SiteReport site : siteErrors) {
			siteAgencyNumber = site.getAgencyCode().trim() + "-" + site.getSiteNumber().trim();
			for (StepReport siteStep : site.getSteps()) {
				errorRow += getDetailSiteMessage(siteAgencyNumber,siteStep, MessageType.error);
				warningRow += getDetailSiteMessage(siteAgencyNumber,siteStep, MessageType.warning);
			}
		}
		siteReportRows += errorRow + warningRow ;
		return siteReportRows;
		
	};
	
	protected String getDetailErrorMessage(String detailMessage) {
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> jsonMap = new HashMap<>();
		String result = "";
		try {
			jsonMap = objectMapper.readValue(detailMessage,
				new TypeReference<Map<String,String>>(){});
			result = jsonMap.get("error_message");
		} catch (IOException e) {
			log.error(NOTIFICATION_STEP + ": error parsing error message; " + e.getMessage());
				result = detailMessage;
		}
		return result;
	}
	
	protected String getDetailSiteMessage(String site, StepReport siteStep, MessageType messageType) {
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> jsonMap = new HashMap<>();
		Map<String, Map<String, String>> jsonObjMap = new HashMap<>();
		Map<String, Map<String, Map<String, List<String>>>> jsonValMap = new HashMap<>();
		String result = "";
		if ((messageType == MessageType.error) && siteStep.getDetails().toString().contains(ERROR_MESSAGE)) {
			try {
				jsonMap = objectMapper.readValue(siteStep.getDetails(),
					new TypeReference<Map<String,String>>(){});
				if (jsonMap.containsKey(ERROR_MESSAGE)) {
					result += site + ", " + siteStep.getName() + " Fatal Error: " + jsonMap.get(ERROR_MESSAGE);
				}
			} catch (IOException e) {
				log.warn(NOTIFICATION_STEP + ": error message might be object, trying to parse; " + e.getMessage());
				try {
					jsonObjMap = objectMapper.readValue(siteStep.getDetails(),
							new TypeReference<Map<String,Map<String, String>>>(){});
					if (jsonObjMap.containsKey("error_message")) {
						for (Map.Entry<String, String> entry : jsonObjMap.get(ERROR_MESSAGE).entrySet()) {
							result += site + ", " + siteStep.getName() + " Fatal Error: " + entry.getKey() + " - " + entry.getValue() + "\n";
						}
					}
				} catch (IOException e1) {
					log.warn(NOTIFICATION_STEP + ": error parsing object error message, could be validation message; " + e1.getMessage());
					try {
						jsonValMap = objectMapper.readValue(siteStep.getDetails(),
								new TypeReference<Map<String,Map<String, Map<String, List<String>>>>>(){});
						if (jsonValMap.containsKey(VALIDATOR_MESSAGE)){
							if (jsonValMap.get(VALIDATOR_MESSAGE).containsKey(FATAL_ERROR_MESSAGE)) {
								for (Map.Entry<String, List<String>> entry : jsonValMap.get(VALIDATOR_MESSAGE).get(FATAL_ERROR_MESSAGE).entrySet()) {
									result += site + ", " + siteStep.getName() + " Fatal Error: " + entry.getKey() + " - " + entry.getValue().stream().map(Object::toString).collect(Collectors.joining("; ")) + "\n";
								}
							}
						}
					} catch (IOException e2) {
						log.error(NOTIFICATION_STEP + ": error parsing object error message" + e2.getMessage());
						result += siteStep.getDetails();
					}
				}
			}
		} else if (messageType == MessageType.warning && siteStep.getDetails().toString().contains(WARNING_MESSAGE)) {
			try {
				jsonValMap = objectMapper.readValue(siteStep.getDetails(),
						new TypeReference<Map<String,Map<String, Map<String, List<String>>>>>(){});
				if (jsonValMap.containsKey(VALIDATOR_MESSAGE)){
					if (jsonValMap.get(VALIDATOR_MESSAGE).containsKey(WARNING_MESSAGE)) {
						for (Map.Entry<String, List<String>> entry : jsonValMap.get(VALIDATOR_MESSAGE).get(WARNING_MESSAGE).entrySet()) {
							result += site + ", " + siteStep.getName() + " Warning: " + entry.getKey() + " - " + entry.getValue().stream().map(Object::toString).collect(Collectors.joining("; ")) + "\n";
						}
					}
				}
			} catch (IOException e2) {
				log.error(NOTIFICATION_STEP + ": error parsing object warning message" + e2.getMessage());
				result += siteStep.getDetails();
			}
		}
		return result;
	}
}
