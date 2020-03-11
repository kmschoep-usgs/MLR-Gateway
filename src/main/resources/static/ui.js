var reportUrl = null;
var uploadRequest = false;
var action = null;
var multipleDistrictCodeMsg = "Please be aware that you are adding or modifying sites in multiple district codes."

function startLoading(headerText) {
	//document
	$("#ddotForm :input").prop("disabled", true);
	$("#exportForm :input").prop("disabled", true);
	$('.mlr-response').show();
	$('#mlr-loading-spinner').addClass('spinner');
	$('.mlr-response-text').text("");
	$('.mlr-response-text').hide();
	$('#mlr-response-link').attr("href", "");
	$('#mlr-response-link').hide();
	$('.mlr-response-header').text(headerText);
}

function stopLoading(headerText) {
	$("#ddotForm :input").prop("disabled", false);
	$("#exportForm :input").prop("disabled", false);
	$('.mlr-response').show();
	$('#mlr-loading-spinner').removeClass('spinner');
	$('.mlr-response-header').text(headerText);
}

function handleResponseJson(json) {
	if(json.hasOwnProperty("workflowSteps")) {
		reportUrl = URL.createObjectURL(new Blob([JSON.stringify(json, null, 4)], {type: "application/json"}));
		$('.mlr-response-text').html(formatJsonResponse(json));
		$('.mlr-response-text').show();
		$('#mlr-response-link').show();
		$('#mlr-response-link').attr("href", reportUrl);
	} else {
		reportUrl = null;
		$('.mlr-response-text').html(formatJsonResponse(json));
		$('.mlr-response-text').show();
		$('#mlr-response-link').hide();
	}
}

function handleResponseText(text) {
	reportUrl = null;
	$('.mlr-response-text').text(text);
	$('.mlr-response-text').show();
	$('#mlr-response-link').hide();
}

function generalSuccess (response) {
	stopLoading($('.mlr-response-header').text(), handleResponseJson(response));
}

function generalError (response) {	
	if(response.status > 0){
		if(response.hasOwnProperty("responseJSON")){
			if(response.responseJSON.hasOwnProperty("error_message")) {
				handleResponseText(response.responseJSON.error_message);
			} else if(response.responseJSON.hasOwnProperty("error")) {
				handleResponseText(response.responseJSON.message);
			} else {
			handleResponseJson(response.responseJSON);
			}
		} else if(response.status == 401) {
			// Got a 401 before a response map could be built, so warn of a potential expired session.
			handleResponseText("It appears that your session may have expired or you are not logged in. Please refresh " + 
				"the page and try again. If this issue persists please contact the support team.");			
		} else {
			try {
				handleResponseJson(JSON.parse(response.responseText));
			} catch(error) {
				handleResponseText("An unknown error occurred while trying to process your request (status code: " + response.status + "). " +
				"If this issue persists please contact the support team with the status code listed above.");
			}
		}
	} else {
		if(uploadRequest) {
			handleResponseText("Connection Error with the Gateway Service. It's possible that the file you're trying to upload greatly" +
			" exceeds the maximum file upload size allowed by the server. Please try a smaller file or contact the support team.");
		} else {
			handleResponseText("Connection Error with the Gateway Service. If this issue persists please contact the support team.");
		}
	}
	
	stopLoading($('.mlr-response-header').text());
}

function postExport(responseHeader, success, error) {
	var agencyCode = $("#exportAgency").val();
	var siteNumber= $("#exportSite").val();
		
	if(agencyCode !== null && agencyCode.length > 0 && siteNumber !== null && siteNumber.length > 0) {
		//Build URL
		var url = "legacy/location/" + agencyCode + "/" + siteNumber;

		//Adjust UI
		startLoading(responseHeader);

		uploadRequest = false;

		//Perform Export
		$.ajax({
			url: url,
			type: 'POST',
			contentType: false,
			cache: false,
			processData: false,
			success: success,
			error: error
		});
	} else {
		handleResponseText("Site copy parameters incomplete.");
		stopLoading(responseHeader + " - Error");
	}
}

function postDdot(url, responseHeader, success, error) {
	//Grab File to Upload
	var documentData = new FormData($("#ddotForm")[0]);
	
	//Check selected file
	if($('input[type=file]')[0].files.length > 0){
		//Adjust UI
		startLoading(responseHeader);

		uploadRequest = true;

		//Perform Upload
		$.ajax({
			url: url,
			type: 'POST',
			data: documentData,
			enctype: "multipart/form-data'",
			contentType: false,
			cache: false,
			processData: false,
			success: success,
			error: error
		});
	} else {
		handleResponseText("No file selected.");
		stopLoading(responseHeader + " - Error");
	}
}

function parseDdot(buttonAction) {
	action = buttonAction;
	postDdot("util/parse", "Ddot Parse Response", preVerification, generalError);
}
function validateDdot() {
	postDdot("workflows/ddots/validate", "Ddot Validation Response", generalSuccess, generalError);
}

function uploadDdot() {
	postDdot("workflows/ddots", "Ddot Validate and Update Response", generalSuccess, generalError);
}

function preVerification(response) {
	stopLoading($('.mlr-response-header').text());
	var districtCodes = response.districtCodes;
	if (districtCodes.length > 1) {
		alert(multipleDistrictCodeMsg);
	}
	if (action === "validate"){
		validateDdot();
	} else if (action === "upload"){
		uploadDdot();
	}
}

function exportLocation() {
	postExport("Export Site Response", generalSuccess, generalError);
}

function formatJsonResponse(response) {
	var responseStr; 
	var workflowErrorRow = "";
	var siteErrorRows = "";
	var reportDate = new Date(response.reportDateTime);
	
	responseStr =  "<ul>";
	responseStr = "<li>MLR Workflow:&nbsp;&nbsp;" + response.name + "</li>";
	responseStr = responseStr + "<li>User:&nbsp;&nbsp;" + response.userName + "</li>";
	responseStr = responseStr + "<li>Date:&nbsp;&nbsp;" + reportDate + "</li>";
	responseStr = responseStr + "<li>Input File:&nbsp;&nbsp;" + response.inputFileName + "</li>";
	responseStr = responseStr + "</ul>";
	
	if (response.name === 'Complete Export Workflow'){
		var workflowFailureMsg = "";
		response.workflowSteps.forEach(function(w){
			if (w.name === "Complete Export Workflow"){
				if (w.success === false) {
					workflowFailureMsg = "<p><b>" + w.name + " Failed: " + JSON.parse(w.details).error_message + "</b></p>";
				}
			}
		});
		responseStr = responseStr + workflowFailureMsg;
	} else {
		if (response.workflowSteps.length > 0){
			var workflowFailure = response.workflowSteps.filter(function(x){
				return x.name.search("workflow") > 0;
			});
			if (workflowFailure.length > 0) {
				var workflowFailureMsg = "<p><b>" + workflowFailure[0].name + " (" + JSON.parse(workflowFailure[0].details).error_message + ") : No Transactions were processed.</b></p><p>Error details listed below: </p>";
			}
			
			var workflowErrors = response.workflowSteps.filter(function(x){
				return x.name.search("workflow") < 0;
			})
		}
		
		if (workflowFailureMsg != null){
			responseStr = responseStr + workflowFailureMsg 
		} else {
			responseStr = responseStr + "<p>Status:&nbsp;&nbsp;" + response.numberSiteSuccess + " Transactions Succeeded, " + response.numberSiteFailure + " Transactions Failed</p>";		
		}
		
		if (workflowErrors != null) {
			responseStr = responseStr + "<div id=\"error-container\">Workflow-level Errors: <br/><div class=\"error-warning\">";
			workflowErrors.forEach(function(e){
				workflowErrorRow = workflowErrorRow + "<div class=\"steps\">" + e.name + ": </div><div class=\"errors\">" + JSON.parse(e.details).error_message + "</div>";
			})
			workflowErrorRow = workflowErrorRow + "</div>"
		}
		
		if (response.sites.length > 0){
			siteErrorRows = "Site-level Errors and Warnings: <br/><div class=\"error-warning\">";
			siteErrorRows = siteErrorRows + parseSiteErrorRows(response.sites);
			siteErrorRows = siteErrorRows + "</div></div>";
		}
		
		responseStr = responseStr + workflowErrorRow + siteErrorRows;
	}
	return responseStr;
}

function parseSiteErrorRows(errorList){
	var result = "";
	var errorRow = "";
	var warningRow = "";
	errorList.forEach(function(s){
		var site = s.agencyCode.trim() + "-" + s.siteNumber.trim();
		var steps = s.steps;
		steps.forEach(function(st){
			var detailMsg = JSON.parse(st.details);
			if (detailMsg.hasOwnProperty("error_message")){
				if (typeof detailMsg.error_message === "object") {
				Object.keys(detailMsg.error_message).forEach(function(key){
					errorRow = errorRow + "<div class=\"steps\">" + site + " </div><div class=\"errors\">" + st.name + " Fatal Error: " + key + " &mdash; " + detailMsg.error_message[key] + "</div>";					
				})
				} else {
					errorRow = errorRow + "<div class=\"steps\">" + site + " </div><div class=\"errors\">" + st.name + " Fatal Error: " + detailMsg.error_message + "</div>";
				}
			}
			if (detailMsg.hasOwnProperty("validator_message")){
				if (detailMsg.validator_message.hasOwnProperty("fatal_error_message")){
					Object.keys(detailMsg.validator_message.fatal_error_message).forEach(function(key){
						errorRow = errorRow + "<div class=\"steps\">" + site + " </div><div class=\"errors\">" + st.name + " Fatal Error: " + key + " &mdash; " + detailMsg.validator_message.fatal_error_message[key] + "</div>";
					})
				}
				if (detailMsg.validator_message.hasOwnProperty("warning_message")){
					Object.keys(detailMsg.validator_message.warning_message).forEach(function(key){
						warningRow = warningRow + "<div class=\"steps\">" + site + " </div><div class=\"errors\">" + st.name + " Warning: " + key + " &mdash; " + detailMsg.validator_message.warning_message[key] + "</div>";
					})
				}
			}
		})
		
	})
	result = errorRow + warningRow;
	return result;
}

$(function () {
	//initialize bootstrap tooltips
	$('[data-toggle="tooltip"]').tooltip();
	
	$('#exportForm').submit(function(e){
		/**
		 * When a user hits the export button or presses 'Enter' while
		 * focused on an export field, we want our custom form action
		 * to be taken. We prevent the browser from taking its default
		 * action.
		 */
		e.preventDefault();
		exportLocation();
	});
});