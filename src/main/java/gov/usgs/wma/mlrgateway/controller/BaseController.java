package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.StepReport;

public class BaseController {

	private static ThreadLocal<GatewayReport> gatewayReport = new ThreadLocal<>();


	public static GatewayReport getReport() {
		return gatewayReport.get();
	}

	public static void setReport(GatewayReport report) {
		gatewayReport.set(report);
	}

	public static void addStepReport(StepReport stepReport) {
		GatewayReport report = gatewayReport.get();
		report.addStepReport(stepReport);
		gatewayReport.set(report);
	}

	public static void remove() {
		gatewayReport.remove();
	}

}
