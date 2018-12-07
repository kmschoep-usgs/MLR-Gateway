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
		List<SiteReport> reportedSites = new ArrayList<SiteReport>();
		List<StepReport> warningFailureSteps = new ArrayList<StepReport>();
		
		try {
			GatewayReport clonedReport = new GatewayReport(report);
			
			List<SiteReport> successfulSites = clonedReport.getSites().stream()
					.filter(s -> s.isSuccess())
					.collect(Collectors.toList());
			
			List<SiteReport> failureSites = clonedReport.getSites().stream()
					.filter(s -> !s.isSuccess())
					.collect(Collectors.toList());
			
			List<SiteReport> warningFailureSites = clonedReport.getSites().stream()
					.filter(s -> s.getSteps().stream()
							.anyMatch(st -> st.getDetails().toString().contains("warning")) || !s.isSuccess())
					.collect(Collectors.toList());
			
			reportedSites.addAll(warningFailureSites);
			
			for (SiteReport site : reportedSites) {
				warningFailureSteps = site.getSteps().stream()
				.filter(st -> st.getDetails().toString().contains("warning") || !st.isSuccess())
				.collect(Collectors.toList());
				// add just the warning/failure steps
				site.setSteps(warningFailureSteps);
			}
			
			List<StepReport> failureWorkflowSteps = report.getWorkflowSteps().stream()
					.filter(w -> !w.isSuccess())
					.collect(Collectors.toList());
			
			userSummaryReport.setInputFileName(clonedReport.getInputFileName());
			userSummaryReport.setName(clonedReport.getName());
			userSummaryReport.setReportDateTime(clonedReport.getReportDateTime());
			userSummaryReport.setUserName(clonedReport.getUserName());
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
