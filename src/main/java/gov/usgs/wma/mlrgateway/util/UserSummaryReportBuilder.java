package gov.usgs.wma.mlrgateway.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.client.NotificationClient;
import gov.usgs.wma.mlrgateway.controller.BaseController;
import gov.usgs.wma.mlrgateway.workflow.LegacyWorkflowService;

import java.util.List;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.HashMap;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
public class UserSummaryReportBuilder {
	
	private Logger log = LoggerFactory.getLogger(UserSummaryReportBuilder.class);
	
	@Autowired
	public UserSummaryReportBuilder(){
	}

	public UserSummaryReport buildUserSummaryReport(GatewayReport report) {
		UserSummaryReport userSummaryReport = new UserSummaryReport();
		long stepReports = report.getWorkflowSteps().stream()
				.filter(st -> (st.isSuccess() || st.getDetails().toString().contains("warning"))).count();
		
		long successSites = report.getSites().stream()
				.filter(s -> s.getSteps().stream()
						.filter(st -> (st.isSuccess() || st.getDetails().toString().contains("warning")))
						.collect(Collectors.toList()))
				.count();
		
		return userSummaryReport;
	}
	

}
