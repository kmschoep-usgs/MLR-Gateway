package gov.usgs.wma.mlrgateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import gov.usgs.wma.mlrgateway.config.PropagateBadRequest;

@FeignClient(name="mlrNotification", configuration=PropagateBadRequest.class)
public interface NotificationClient {
	public static final String MESSAGE_SUBJECT_KEY = "subject";
	public static final String MESSAGE_TO_KEY = "to";
	public static final String MESSAGE_FROM_KEY = "from";
	public static final String MESSAGE_TEXT_BODY_KEY = "textBody";
	public static final String MESSAGE_HTML_BODY_KEY = "htmlBody";
	public static final String MESSAGE_CC_KEY = "cc";
	public static final String MESSAGE_BCC_KEY = "bcc";
	public static final String MESSAGE_REPLY_TO_KEY = "replyTo";
	public static final String MESSAGE_ATTACHMENT_KEY = "attachment";
	public static final String MESSAGE_ATTACHMENT_FILE_NAME_KEY = "attachmentFileName";


	@RequestMapping(method=RequestMethod.POST, value="notification/email", consumes="application/json")
	ResponseEntity<String> sendEmail(@RequestBody String messageJson);

	@RequestMapping(method=RequestMethod.POST, value="notification/email", consumes="application/json")
	ResponseEntity<String> sendEmail(@RequestHeader("Authorization") String bearerToken, @RequestBody String messageJson);
}
