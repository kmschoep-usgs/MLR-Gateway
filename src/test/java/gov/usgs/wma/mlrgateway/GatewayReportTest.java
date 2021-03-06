package gov.usgs.wma.mlrgateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.usgs.wma.mlrgateway.controller.BaseController;

import java.util.ArrayList;
import java.util.List;


@ExtendWith(SpringExtension.class)
public class GatewayReportTest {

	private SiteReport siteReport;
	private StepReport stepReport;
	private GatewayReport report;
	private String reportName = "TEST NOTIFICATION";
	private String fileName = "test.d";
	private final String agencyCode = "USGS ";
	private final String siteNumber = "12345678       ";
	private final String transactionType = "M";
	private ObjectMapper mapper;
	
	@BeforeEach
	public void init() {
		List<StepReport> steps = new ArrayList<>();
		List<SiteReport> sites = new ArrayList<>();
		String userName = "userName";
		String reportDate = "01/01/2019";
		BaseController.setReport(new GatewayReport(reportName, fileName, userName, reportDate));
		siteReport = new SiteReport(agencyCode, siteNumber);
		stepReport = new StepReport("test step", 200, true, "step details");
		steps.add(stepReport);
		siteReport.setSteps(steps);
		siteReport.setSuccess(true);
		siteReport.setTransactionType(transactionType);
		sites.add(siteReport);
		report = BaseController.getReport();
		report.setSites(sites);
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath() throws Exception {

		GatewayReport clonedGatewayReport = new GatewayReport(report);
		JSONAssert.assertEquals(mapper.writeValueAsString(clonedGatewayReport), mapper.writeValueAsString(report), JSONCompareMode.STRICT);
	}

}
