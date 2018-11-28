var reportUrl = null;
var uploadRequest = false;

function startLoading(headerText) {
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
	if(json.hasOwnProperty("steps")) {
		reportUrl = URL.createObjectURL(new Blob([JSON.stringify(json, null, 4)], {type: "application/json"}));
		$('.mlr-response-text').hide();
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
	stopLoading($('.mlr-response-header').text() + " - Success", handleResponseJson(response));
}

function generalError (response) {	
	if(response.status > 0){
		if(response.hasOwnProperty("responseJSON")){
			handleResponseJson(response.responseJSON);
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
	
	stopLoading($('.mlr-response-header').text() + " - Failure");
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
		stopLoading(responseHeader + " - Error", "No file selected.");
	}
}

function validateDdot() {
	postDdot("workflows/ddots/validate", "Ddot Validation Response", generalSuccess, generalError);
}

function uploadDdot() {
	postDdot("workflows/ddots", "Ddot Validate and Update Response", generalSuccess, generalError);
}

function exportLocation() {
	postExport("Export Site Response", generalSuccess, generalError);
}

function formatJsonResponse(response) {
	return JSON.stringify(response, null, 4).split("\n").join("<br/>").split(" ").join("&nbsp;");
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