package gov.usgs.wma.mlrgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.http.HttpStatus;

@Service
public class NotificationService {

	private NotificationClient notificationClient;
	public static final String NOTIFICATION_STEP = "Notification";
	public static final String NOTIFICATION_SUCCESSFULL = "Notification sent successfully.";
	
	@Autowired
	public NotificationService(NotificationClient notificationClient){
		this.notificationClient = notificationClient;
	}

	public void sendNotification(String recipient, String subject, String reportBody) {
		ObjectMapper mapper = new ObjectMapper();
		String messageJson = "";
		HashMap<String, Object> messageMap = new HashMap<>();
		
		//Build Email Message
		ArrayList<String> toList = new ArrayList<>();
		toList.add(recipient);
		
		messageMap.put(NotificationClient.MESSAGE_TO_KEY, toList);
		messageMap.put(NotificationClient.MESSAGE_SUBJECT_KEY, subject);
		messageMap.put(NotificationClient.MESSAGE_TEXT_BODY_KEY, reportBody);
		
		try {
			messageJson = mapper.writeValueAsString(messageMap);
		} catch(Exception e) {
			// Unable to determine when this might actually happen, but the api says it can...
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "{\"error_message\": \"Unable to serialize notification message.\"}");
		}
		
		ResponseEntity<String> notifResp = notificationClient.sendEmail(messageJson);
		BaseController.addStepReport(new StepReport(NOTIFICATION_STEP, notifResp.getStatusCodeValue(), NOTIFICATION_SUCCESSFULL, null, null));
	}

}
