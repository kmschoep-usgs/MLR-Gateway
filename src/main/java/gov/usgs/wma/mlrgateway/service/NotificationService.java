package gov.usgs.wma.mlrgateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;

@Service
public class NotificationService {

	private NotificationClient notificationClient;
	public static final String NOTIFICATION_STEP = "Notification";
	public static final String NOTIFICATION_SUCCESSFULL = "Notification sent successfully.";
	
	@Autowired
	public NotificationService(NotificationClient notificationClient){
		this.notificationClient = notificationClient;
	}

	public void sendNotification(String subject, String reportBody) {
		ResponseEntity<String> notifResp = notificationClient.sendEmail(subject, reportBody, "drsteini@usgs.gov");
		BaseController.addStepReport(new StepReport(NOTIFICATION_STEP, notifResp.getStatusCodeValue(), NOTIFICATION_SUCCESSFULL, null, null));
	}

}
