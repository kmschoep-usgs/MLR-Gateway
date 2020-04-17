package gov.usgs.wma.mlrgateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;


@ExtendWith(SpringExtension.class)
public class SiteReportTest {

	private SiteReport siteReport;
	private StepReport stepReport;
	private final String agencyCode = "USGS ";
	private final String siteNumber = "12345678       ";
	private final String transactionType = "M";
	private ObjectMapper mapper;

	@BeforeEach
	public void init() {
		List<StepReport> steps = new ArrayList<>();
		siteReport = new SiteReport(agencyCode, siteNumber);
		stepReport = new StepReport("test step", 200, true, "step details");
		steps.add(stepReport);
		siteReport.setSteps(steps);
		siteReport.setSuccess(true);
		siteReport.setTransactionType(transactionType);
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath() throws Exception {
		SiteReport clonedSiteReport = new SiteReport(siteReport);
		JSONAssert.assertEquals(mapper.writeValueAsString(clonedSiteReport), mapper.writeValueAsString(siteReport), JSONCompareMode.STRICT);
	}

}
