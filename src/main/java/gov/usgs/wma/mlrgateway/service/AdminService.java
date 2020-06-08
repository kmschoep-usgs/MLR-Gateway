package gov.usgs.wma.mlrgateway.service;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.client.LegacyCruClient;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.exception.InvalidEmailException;

@Service
public class AdminService {
    public static final String DISTRICT_CODE_KEY = "districtCode";
    public static final String INSERT_COUNT_KEY = "insertCount";
    public static final String UPDATE_COUNT_KEY = "updateCount";

	private LegacyCruClient legacyCruClient;
    private NotificationClient notificationClient;
    
	@Value("${uiHost:}")
	protected String uiDomainName;
    
	@Autowired
	public AdminService(LegacyCruClient legacyCruClient, NotificationClient notificationClient) {
        this.legacyCruClient = legacyCruClient;
        this.notificationClient = notificationClient;
	}

    public void sendSummaryEmail(String date, List<String> recipientList, String bearerToken) throws IOException {
        Map<String, Object> messageMap = new HashMap<>();
        String messageBody = generateSummaryHTML(date, bearerToken);

		//Build Request
		messageMap.put(NotificationClient.MESSAGE_TO_KEY, recipientList);
		messageMap.put(NotificationClient.MESSAGE_SUBJECT_KEY, "Summary of MLR Transactions for " + date);
        messageMap.put(NotificationClient.MESSAGE_HTML_BODY_KEY, messageBody);
        
        try {
            notificationClient.sendEmail(bearerToken, new ObjectMapper().writeValueAsString(messageMap));
        } catch(FeignBadResponseWrapper e) {
            if(e.getStatus() == HttpStatus.SC_BAD_REQUEST) {
                throw new InvalidEmailException(e.getBody());
            } else {
                throw e;
            }
        }
    }

    public String generateSummaryHTML(String date, String bearerToken) throws IOException {
        StringBuilder builder = new StringBuilder();

        String rawResponse = legacyCruClient.getLoggedTransactionSummary(bearerToken, date, date, null).getBody();
        List<Map<String, Object>> summaryList = new ObjectMapper().readValue(rawResponse, ArrayList.class);

        builder.append("<h1>Summary of MLR Transactions executed on " + date + "</h1>");

        if(summaryList != null && !summaryList.isEmpty()) {
            builder.append("<p>The table below shows the daily number of executed transactions sorted by "); 
            builder.append("district code. The table does not list failed or duplicate transactions. ");
            builder.append("If ownership or the authoritative source of a location is transferred to a ");
            builder.append("different district code, that transaction will show up as a location modification ");
            builder.append("under both district codes; and does not infer the given location still exists in ");
            builder.append("both Water Science Centers.</p>");
            builder.append("<table style=\"border-collapse: collapse;\" cellpadding=\"7\" border=\"1\">");
            builder.append("<tr>");
            builder.append("<th>District Code</th>");
            builder.append("<th>New Locations Added</th>");
            builder.append("<th>Location Modifications</th>");
            builder.append("</tr>");

            for(Map<String,Object> summary : summaryList) {
                builder.append("<tr>");
                builder.append("<td>" + summary.get(DISTRICT_CODE_KEY) + "</td>");
                builder.append("<td>" + summary.get(INSERT_COUNT_KEY) + "</td>");
                builder.append("<td>" + summary.get(UPDATE_COUNT_KEY) + "</td>");
                builder.append("</tr>");
            }

            builder.append("</table>");
            builder.append("<p>To view the details of each change visit the Audit Table section of the MLR Website.</p>");
        } else {
            builder.append("<p>No transactions were executed on " + date + " that resulted in any modifications to location data in MLR.</p>");
        }

        builder.append("<br/>MLR Website: <a href=\"").append(uiDomainName).append("\">Click Here</a>");
        return builder.toString();
    }
}
