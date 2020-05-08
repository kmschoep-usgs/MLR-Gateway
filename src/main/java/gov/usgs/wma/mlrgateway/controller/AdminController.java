package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.exception.InvalidEmailException;
import gov.usgs.wma.mlrgateway.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Admin Controller", description="Display")
@RestController
@RequestMapping("/admin")
public class AdminController {
	private Logger log = LoggerFactory.getLogger(AdminController.class);
	private AdminService adminService;

	@Autowired
	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@Operation(description="Generates and sends a transaction summary email for the provided date")
	@ApiResponses(value={
		@ApiResponse(responseCode = "200", description = "OK"),
		@ApiResponse(responseCode = "400", description = "Bad Request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "403", description = "Forbidden") })
	@GetMapping("/sendSummaryEmail")
	public void sendSummaryEmail(
		@RequestParam @Pattern(regexp="\\d\\d\\d\\d-\\d\\d-\\d\\d") String date,
		@RequestParam @NotEmpty List<String> recipientList,
		HttpServletResponse response
	) throws IOException {
		log.info("[SUMMARY EMAIL WORKFLOW]: Starting summary email generation for date: " + date);
		try {
			adminService.sendSummaryEmail(date, recipientList);
			response.setStatus(HttpStatus.OK.value());
		} catch(InvalidEmailException e) {
			response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
		}
	}
}