package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.config.WaterAuthJwtConverter;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class BaseController {
	@Value("${additionalNotificationRecipients}")
	private String additionalNotificationRecipientsString;
	
	private Logger log = Logger.getLogger(BaseController.class);
	private static ThreadLocal<GatewayReport> gatewayReport = new ThreadLocal<>();

	public static GatewayReport getReport() {
		return gatewayReport.get();
	}

	public static void setReport(GatewayReport report) {
		gatewayReport.set(report);
	}

	public static void addStepReport(StepReport stepReport) {
		GatewayReport report = gatewayReport.get();
		report.addStepReport(stepReport);
		gatewayReport.set(report);
	}

	public static void remove() {
		gatewayReport.remove();
	}
	
	protected int notificationStep(NotificationService notificationService, String subject) {
		int status = -1;
		List<String> notificationRecipientList;
		
		//Send Notification
		try {
			if(additionalNotificationRecipientsString != null && additionalNotificationRecipientsString.length() > 0){
				//Note List returned from Arrays.asList does not implement .add() thus the need for the additional ArrayList<> constructor
				notificationRecipientList = new ArrayList<>(Arrays.asList(StringUtils.split(additionalNotificationRecipientsString, ',')));
			} else {
				notificationRecipientList = new ArrayList<>();
			}
			
			if(SecurityContextHolder.getContext().getAuthentication() != null){
				Map<String, Serializable> oauthExtensions = ((OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication()).getOAuth2Request().getExtensions();
				String userEmail = (String)oauthExtensions.get(WaterAuthJwtConverter.EMAIL_JWT_KEY);
				
				if(userEmail!= null && userEmail.length() > 0){
					notificationRecipientList.add(userEmail);
				} else {
					log.warn("No User Email present in the Web Security Context when sending the Notification Email!");
				}
			} else {
				log.warn("No Authentication present in the Web Security Context when sending the Notification Email!");
			}
			
			notificationService.sendNotification(notificationRecipientList, subject, getReport().toString());
		} catch(Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				 status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addStepReport(new StepReport(NotificationService.NOTIFICATION_STEP, status, ((FeignBadResponseWrapper) e).getBody(), null, null));
			} else {
				status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addStepReport(new StepReport(NotificationService.NOTIFICATION_STEP, status, e.getLocalizedMessage(), null, null));
			}
		}
		
		return status;
	}

}
