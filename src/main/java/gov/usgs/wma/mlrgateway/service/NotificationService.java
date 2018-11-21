package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
	public static final String NOTIFICATION_FAILURE = "Notification failed to send.";
	public static final Temporal REPORT_DATE_TIME = LocalDateTime.now();
	
	@Autowired
	public NotificationService(NotificationClient notificationClient){
		this.notificationClient = notificationClient;
	}

	public void sendNotification(List<String> recipientList, String subject, String user, GatewayReport report) {
		ObjectMapper mapper = new ObjectMapper();
		String messageJson;
		HashMap<String, Object> messageMap = new HashMap<>();
		
		messageMap.put(NotificationClient.MESSAGE_TO_KEY, recipientList);
		messageMap.put(NotificationClient.MESSAGE_SUBJECT_KEY, subject);
		messageMap.put(NotificationClient.MESSAGE_TEXT_BODY_KEY, buildMessageBody(report, user));
		
		try {
			messageJson = mapper.writeValueAsString(messageMap);
			ResponseEntity<String> notifResp = notificationClient.sendEmail(messageJson);
			BaseController.addNotificationStepReport(new StepReport(NOTIFICATION_STEP, notifResp.getStatusCodeValue(), true, NOTIFICATION_SUCCESSFULL));
		} catch(Exception e) {
			BaseController.addNotificationStepReport(new StepReport(NOTIFICATION_STEP, HttpStatus.SC_INTERNAL_SERVER_ERROR, false, NOTIFICATION_FAILURE));
			log.error(NOTIFICATION_STEP + ": " + e.getMessage());
		}
	}
	
	private String buildMessageBody(GatewayReport report, String user) {
		String reportBody = "An MLR Workflow has completed on the " + 
				environmentTier + " environment. The workflow output report is below.\n\n\n";
		reportBody += "User:     " + user + "\n\n";
		reportBody += "Workflow: " + report.getName() + "\n\n";
		reportBody += "Report Date: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(REPORT_DATE_TIME); 
		//reportBody += "Status:   " + ((200 == report.getWorkflowStep().getHttpStatus() || report.getStatus() == 201) ? "Success" : "Failure") + "(" + report.getStatus() + ")\n\n";		
		
		reportBody += "The full, raw report output is included below.\n\n\n";
		reportBody += report.toPrettyPrintString();
		
		return reportBody;
	}

}
