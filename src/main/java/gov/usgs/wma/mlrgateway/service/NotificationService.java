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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
public class NotificationService {

	private NotificationClient notificationClient;
	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
	
	@Value("${environmentTier:}")
	private String environmentTier;
	
	public static final String NOTIFICATION_STEP = "Notification";
	public static final String NOTIFICATION_SUCCESSFUL = "Notification sent successfully.";
	public static final String NOTIFICATION_FAILURE = "{\"error_message\": \"Notification failed to send.\"}";
	public static final String ATTACHMENT_FILE_NAME = "mlr-%NAME%-report.json";
	public static final String ERROR_MESSAGE = "error_message";
	public static final String FATAL_ERROR_MESSAGE = "fatal_error_message";
	public static final String WARNING_MESSAGE = "warning_message";
	public static final String VALIDATOR_MESSAGE = "validator_message";
	
	@Autowired
	public NotificationService(NotificationClient notificationClient){
		this.notificationClient = notificationClient;
	}

	public void sendNotification(String recipient, List<String> ccList, String subject, String user, String attachmentFileName, UserSummaryReport report) {
		ObjectMapper mapper = new ObjectMapper();
		String messageJson;
		HashMap<String, Object> messageMap = buildRequestMap(recipient, ccList, subject, user, attachmentFileName, report);

		try {
			messageJson = mapper.writeValueAsString(messageMap);
			ResponseEntity<String> notifResp = notificationClient.sendEmail(messageJson);
			BaseController.addWorkflowStepReport(new StepReport(NOTIFICATION_STEP, notifResp.getStatusCodeValue(), true, NOTIFICATION_SUCCESSFUL));
		} catch(Exception e) {
			BaseController.addWorkflowStepReport(new StepReport(NOTIFICATION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, NOTIFICATION_FAILURE));
			log.error(NOTIFICATION_STEP + ": " + e.getMessage(), e);
		}
	}

	protected HashMap<String, Object> buildRequestMap(String recipient, List<String> ccList, String subject, String user, String attachmentFileName, UserSummaryReport report) {
		HashMap<String, Object> messageMap = new HashMap<>();

		//Build Request
		messageMap.put(NotificationClient.MESSAGE_TO_KEY, Arrays.asList(recipient));
		messageMap.put(NotificationClient.MESSAGE_CC_KEY, ccList);
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
		reportBody += "Input File: " + report.getInputFileName() + "\n";
		reportBody += "The full, raw report output is attached.\n\n";
		reportBody += buildErrorReport(report); 
		log.debug("Report Body:{}", reportBody);
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
			List<StepReport> workflowErrorSteps = report.getWorkflowSteps().stream()
					.filter(w -> w.getName() == "Complete Export Workflow" && w.isSuccess() == false)
					.collect(Collectors.toList());
			if(workflowErrorSteps.size() > 0) {
				String errorPattern = "{0} Failed: {1}\n\n";
				MessageFormat message = new MessageFormat(errorPattern);
				Object[] arguments = {workflowErrorSteps.get(0).getName(), getDetailErrorMessage(workflowErrorSteps.get(0).getDetails())};
				workflowFailureMsg = message.format(arguments);
				errorReport += workflowFailureMsg;
			}
		} else {
			if (report.getWorkflowSteps().size() > 0){
				List<StepReport> workflowFailureSteps = report.getWorkflowSteps().stream()
						.filter(w -> w.getName().contains("workflow"))
						.collect(Collectors.toList());
				if (workflowFailureSteps.size() > 0) {
					String errorPattern = "{0} ({1}) : No Transactions were processed.\n\nError details listed below:\n\n";
					MessageFormat message = new MessageFormat(errorPattern);
					Object[] arguments = {workflowFailureSteps.get(0).getName(), getDetailErrorMessage(workflowFailureSteps.get(0).getDetails())};
					workflowFailureMsg = message.format(arguments);
				} 
				List<StepReport> workflowErrorSteps = report.getWorkflowSteps().stream()
						.filter(w -> !w.getName().contains("workflow"))
						.collect(Collectors.toList());
				if (workflowErrorSteps.size() > 0) {
					workflowFailureMsg += "Workflow-level Errors:\n\n";
					String errorPattern = "{0}: {1}\n";
					MessageFormat message = new MessageFormat(errorPattern);
					for (StepReport w: workflowErrorSteps) {
						Object[] arguments = {w.getName(),getDetailErrorMessage(w.getDetails())};
						workflowFailureMsg += message.format(arguments);
					};
				}
			}
			if (workflowFailureMsg != "") {
				errorReport += workflowFailureMsg;
			} else {
				String messagePattern = "Status:  {0} Transactions Succeeded, {1} Transactions Failed\n\n";
				MessageFormat message = new MessageFormat(messagePattern);
				Object[] arguments = {report.getNumberSiteSuccess().toString(), report.getNumberSiteFailure().toString()};
				errorReport += message.format(arguments);
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
				if (siteStep.getDetails().toString().contains(ERROR_MESSAGE)) {
					errorRow += parseDetailSiteErrorMessage(siteAgencyNumber,siteStep);
				}
				if (siteStep.getDetails().toString().contains(WARNING_MESSAGE)) {
					warningRow += parseDetailSiteWarningMessage(siteAgencyNumber,siteStep);
				}
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
			result = jsonMap.get(ERROR_MESSAGE);
		} catch (IOException e) {
			log.error(NOTIFICATION_STEP + ": error parsing error message; " + e.getMessage());
				result = detailMessage;
		}
		return result;
	}
	
	protected String parseDetailSiteErrorMessage(String site, StepReport siteStep) {
		String result = "";
		try {
			Map<String, String> jsonMap = new HashMap<>();
			jsonMap = new ObjectMapper().readValue(siteStep.getDetails(),
				new TypeReference<Map<String,String>>(){});
			if (jsonMap.containsKey(ERROR_MESSAGE)) {
				String errorPattern = "{0}, {1} Fatal Error: {2}\n";
				MessageFormat message = new MessageFormat(errorPattern);
				Object[] arguments = {site, siteStep.getName(), jsonMap.get(ERROR_MESSAGE)};
				result += message.format(arguments);
				}
		} catch (IOException e) {
			log.warn(NOTIFICATION_STEP + ": error message might be object, trying to parse; " + e.getMessage());
			try {
				Map<String, Map<String, String>> jsonObjMap = new HashMap<>();
				jsonObjMap = new ObjectMapper().readValue(siteStep.getDetails(),
						new TypeReference<Map<String,Map<String, String>>>(){});
				if (jsonObjMap.containsKey("error_message")) {
					String errorPattern = "{0}, {1} Fatal Error: {2} - {3}\n";
					MessageFormat message = new MessageFormat(errorPattern);
					for (Map.Entry<String, String> entry : jsonObjMap.get(ERROR_MESSAGE).entrySet()) {
						Object[] arguments = {site, siteStep.getName(), entry.getKey(),entry.getValue()};
						result += message.format(arguments);
					}
				}
			} catch (IOException e1) {
				log.warn(NOTIFICATION_STEP + ": error parsing object error message, could be validation message; " + e1.getMessage());
				try {
					Map<String, Map<String, Map<String, List<String>>>> jsonValMap = new HashMap<>();
					jsonValMap = new ObjectMapper().readValue(siteStep.getDetails(),
							new TypeReference<Map<String,Map<String, Map<String, List<String>>>>>(){});
					String errorPattern = "{0}, {1} Fatal Error: {2} - {3}\n";
					MessageFormat message = new MessageFormat(errorPattern);
					if (jsonValMap.containsKey(VALIDATOR_MESSAGE)){
						if (jsonValMap.get(VALIDATOR_MESSAGE).containsKey(FATAL_ERROR_MESSAGE)) {
							for (Map.Entry<String, List<String>> entry : jsonValMap.get(VALIDATOR_MESSAGE).get(FATAL_ERROR_MESSAGE).entrySet()) {
								Object[] arguments = {site, siteStep.getName(), entry.getKey(),entry.getValue().stream().map(Object::toString).collect(Collectors.joining("; "))};
								result += message.format(arguments);
							}
						}
					}
				} catch (IOException e2) {
					log.error(NOTIFICATION_STEP + ": error parsing object error message" + e2.getMessage());
					result += siteStep.getDetails();
				}
			}
		}
		return result;
	}
	protected String parseDetailSiteWarningMessage(String site, StepReport siteStep) {
		String result = "";
		try {
			Map<String, Map<String, Map<String, List<String>>>> jsonValMap = new HashMap<>();
			jsonValMap = new ObjectMapper().readValue(siteStep.getDetails(),
					new TypeReference<Map<String,Map<String, Map<String, List<String>>>>>(){});
			String warningPattern = "{0}, {1} Warning: {2} - {3}\n";
			MessageFormat message = new MessageFormat(warningPattern);
			if (jsonValMap.containsKey(VALIDATOR_MESSAGE)){
				if (jsonValMap.get(VALIDATOR_MESSAGE).containsKey(WARNING_MESSAGE)) {
					for (Map.Entry<String, List<String>> entry : jsonValMap.get(VALIDATOR_MESSAGE).get(WARNING_MESSAGE).entrySet()) {
						Object[] arguments = {site, siteStep.getName(), entry.getKey(),entry.getValue().stream().map(Object::toString).collect(Collectors.joining("; "))};
						result += message.format(arguments);
					}
				}
			}
		} catch (IOException e2) {
			log.error(NOTIFICATION_STEP + ": error parsing object warning message" + e2.getMessage());
			result += siteStep.getDetails();
		}
		return result;
	}
}
