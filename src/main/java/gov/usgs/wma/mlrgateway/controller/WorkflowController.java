package gov.usgs.wma.mlrgateway.controller;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.service.LegacyWorkflowService;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {

	private LegacyWorkflowService legacy;

	@Autowired
	public WorkflowController(LegacyWorkflowService legacy) {
		this.legacy = legacy;
	}

	@PostMapping("/ddots")
	public String legacyWorkflow(@RequestPart MultipartFile file, HttpServletResponse response) {
		response.setContentType("application/json;charset=UTF-8");
		try {
			return legacy.completeWorkflow(file, response);
		} catch (HystrixBadRequestException e) {
			if (e instanceof FeignBadResponseWrapper) {
				response.setStatus(((FeignBadResponseWrapper) e).getStatus());
				return ((FeignBadResponseWrapper) e).getBody();
			} else {
				response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				return e.getLocalizedMessage();
			}
		}
	}

	@PostMapping("/ddots/validate")
	public String legacyValidationWorkflow(@RequestPart MultipartFile file, HttpServletResponse response) {
		response.setContentType("application/json;charset=UTF-8");
		try {
			return legacy.ddotValidation(file, response);
		} catch (HystrixBadRequestException e) {
			if (e instanceof FeignBadResponseWrapper) {
				response.setStatus(((FeignBadResponseWrapper) e).getStatus());
				return ((FeignBadResponseWrapper) e).getBody();
			} else {
				response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				return e.getLocalizedMessage();
			}
		}
	}

}
