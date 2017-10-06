package gov.usgs.wma.mlrgateway;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import gov.usgs.wma.mlrgateway.service.LegacyWorkflowService;

public class BaseSpringTest {

	public String getCompareFile(String folder, String file) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(new ClassPathResource(folder + file).getInputStream()));
	}


	public static Map<String, Object> getUnknown() {
		Map<String, Object> m = new HashMap<>();
		m.put(LegacyWorkflowService.AGENCY_CODE, "USGS ");
		m.put(LegacyWorkflowService.SITE_NUMBER, "12345678       ");
		return m;
	}

	public static Map<String, Object> getAdd() {
		Map<String, Object> m = new HashMap<>();
		m.put(LegacyWorkflowService.TRANSACTION_TYPE, LegacyWorkflowService.TRANSACTION_TYPE_ADD);
		m.put(LegacyWorkflowService.AGENCY_CODE, "USGS ");
		m.put(LegacyWorkflowService.SITE_NUMBER, "12345678       ");
		return m;
	}

	public static Map<String, Object> getUpdate() {
		Map<String, Object> m = new HashMap<>();
		m.put(LegacyWorkflowService.TRANSACTION_TYPE, LegacyWorkflowService.TRANSACTION_TYPE_UPDATE);
		m.put(LegacyWorkflowService.AGENCY_CODE, "USGS ");
		m.put(LegacyWorkflowService.SITE_NUMBER, "12345678       ");
		return m;
	}

}
