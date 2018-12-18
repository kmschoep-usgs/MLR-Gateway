package gov.usgs.wma.mlrgateway;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;


@RunWith(SpringRunner.class)
public class StepReportTest {

	private StepReport stepReport;
	private ObjectMapper mapper;
	

	@Before
	public void init() {
		stepReport = new StepReport("test step", 200, true, "step details");
		mapper = new ObjectMapper();
	}

	@Test
	public void happyPath() throws Exception {
		StepReport clonedStepReport = new StepReport(stepReport);
		JSONAssert.assertEquals(mapper.writeValueAsString(clonedStepReport), mapper.writeValueAsString(stepReport), JSONCompareMode.STRICT);
	}

}
