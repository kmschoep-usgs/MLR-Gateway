package gov.usgs.wma.mlrgateway.util;

import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.SiteReport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.controller.BaseController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserSummaryReportBuilder {
	
	private Logger log = LoggerFactory.getLogger(UserSummaryReportBuilder.class);
	
	@Autowired
	public UserSummaryReportBuilder(){
	}

	public UserSummaryReport buildUserSummaryReport(GatewayReport report) {

		UserSummaryReport userSummaryReport = new UserSummaryReport();
		List<SiteReport> reportedSites = new ArrayList<>();
		
		try {
			List<SiteReport> successfulSites = report.getSites().stream()
					.filter(s -> s.isSuccess())
					.collect(Collectors.toList());
			
			List<SiteReport> failureSites = report.getSites().stream()
					.filter(s -> !s.isSuccess())
					.collect(Collectors.toList());
			
			List<SiteReport> warningFailureSites = report.getSites().stream()
					.filter(s -> s.getSteps().stream()
							.anyMatch(st -> st.getDetails().toString().contains("warning")) || !s.isSuccess())
					.collect(Collectors.toList());
			
			reportedSites.addAll(warningFailureSites);
			
			for (SiteReport site : reportedSites) {
				List<StepReport> warningFailureSteps = site.getSteps().stream()
				.filter(st -> st.getDetails().toString().contains("warning") || !st.isSuccess())
				.collect(Collectors.toList());
				// add just the warning/failure steps
				site.setSteps(warningFailureSteps);
			}
			
			List<StepReport> failureWorkflowSteps = report.getWorkflowSteps().stream()
					.filter(w -> !w.isSuccess())
					.collect(Collectors.toList());
			
			userSummaryReport.setInputFileName(report.getInputFileName());
			userSummaryReport.setName(report.getName());
			userSummaryReport.setReportDateTime(report.getReportDateTime());
			userSummaryReport.setUserName(report.getUserName());
			userSummaryReport.setNumberSiteSuccess(successfulSites.size());
			userSummaryReport.setNumberSiteFailure(failureSites.size());
			userSummaryReport.setWorkflowSteps(failureWorkflowSteps);
			
			userSummaryReport.setSites(reportedSites);
		} catch(Exception e) {
			log.error("User Summary Report builder failed: " + e.getMessage());
		}
		return userSummaryReport;
	}
}
