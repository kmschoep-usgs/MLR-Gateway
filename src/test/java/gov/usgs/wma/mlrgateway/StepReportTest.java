package gov.usgs.wma.mlrgateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.ObjectMapper;


@ExtendWith(SpringExtension.class)
public class StepReportTest {

	private StepReport stepReport;
	private ObjectMapper mapper;
	

	@BeforeEach
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
