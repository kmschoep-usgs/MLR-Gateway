package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.config.WaterAuthJwtConverter;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.util.UserSummaryReportBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public abstract class BaseController {
	private NotificationService notificationService;
	
	@Value("${additionalNotificationRecipients:}")
	private String additionalNotificationRecipientsString;
	
	@Value("${environmentTier:}")
	protected String environmentTier;
	protected String SUBJECT_PREFIX = "[%environment%] MLR Report for ";
		
	private Logger log = LoggerFactory.getLogger(BaseController.class);
	private static ThreadLocal<GatewayReport> gatewayReport = new ThreadLocal<>();
	private UserSummaryReport userSummaryReport = new UserSummaryReport();
	private UserSummaryReportBuilder userSummaryReportBuilder = new UserSummaryReportBuilder();
	
	public BaseController() {};
	public BaseController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	public static GatewayReport getReport() {
		return gatewayReport.get();
	}

	public static void setReport(GatewayReport report) {
		gatewayReport.set(report);
	}
	
	public static void addSiteReport(SiteReport siteReport) {
		GatewayReport report = gatewayReport.get();
		report.addSiteReport(siteReport);
		gatewayReport.set(report);
	}
	
	public static void addWorkflowStepReport(StepReport stepReport) {
		GatewayReport report = gatewayReport.get();
		report.addWorkflowStepReport(stepReport);
		gatewayReport.set(report);
	}

	public static void remove() {
		gatewayReport.remove();
	}
	
	protected void notificationStep(String subject, String attachmentFileName) {
		List<String> notificationRecipientList;
		String userName = "Unknown";
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
				userName = SecurityContextHolder.getContext().getAuthentication().getName();
				
				if(userEmail != null && userEmail.length() > 0){
					notificationRecipientList.add(userEmail);
				} else {
					log.warn("No User Email present in the Web Security Context when sending the Notification Email!");
				}
			} else {
				log.warn("No Authentication present in the Web Security Context when sending the Notification Email!");
			}
			String fullSubject = SUBJECT_PREFIX.replace("%environment%", environmentTier != null && environmentTier.length() > 0 ? environmentTier : "") + subject;
			userSummaryReport = userSummaryReportBuilder.buildUserSummaryReport(getReport());
			notificationService.sendNotification(notificationRecipientList, fullSubject, userName, attachmentFileName, userSummaryReport);
		} catch(Exception e) {
			log.error("An error occured while attempting to send the notification email: ", e);
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addWorkflowStepReport(new StepReport(NotificationService.NOTIFICATION_STEP, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addWorkflowStepReport(new StepReport(NotificationService.NOTIFICATION_STEP, status, false, e.getMessage()));
			}
		}
	}
}
