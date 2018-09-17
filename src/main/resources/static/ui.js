function startLoading(headerText) {
	$("#ddotForm :input").prop("disabled", true);
	$("#exportForm :input").prop("disabled", true);
	$('.mlr-response').show();
	$('#mlr-loading-spinner').addClass('spinner');
	$('.mlr-response-text').text("");
	$('.mlr-response-header').text(headerText);
}

function stopLoading(headerText, responseText) {
	$("#ddotForm :input").prop("disabled", false);
	$("#exportForm :input").prop("disabled", false);
	$('.mlr-response').show();
	$('#mlr-loading-spinner').removeClass('spinner');
	$('.mlr-response-header').text(headerText);
	$('.mlr-response-text').html(responseText);
}

function generalSuccess (response) {
	stopLoading($('.mlr-response-header').text() + " - Success", formatJsonResponse(response));
}

function generalError (response) {
	var responseText;
	
	if(response.status > 0){
		if(response.hasOwnProperty("responseJSON")){
			responseText = formatJsonResponse(response.responseJSON);
		} else if(response.hasOwnProperty("responseText")) {
			responseText = formatJsonResponse(JSON.parse(response.responseText));
		} else if(response.status == 401) {
			// Got a 401 before a response map could be built, so warn of a potential expired session.
			responseText = "It appears that your session may have expired or you are not logged in. Please refresh " + 
				"the page and try again. If this issue persists please contact the support team.";
		} else {
			responseText = "An unknown error occurred while trying to process your request (status code: " + response.status + ")." +
				"If this issue persists please contact the support team with the status code listed above.";
		}
	} else {
		responseText = "Connection Error with Gateway Service. If this issue persists please contact the support team.";
	}
	
	stopLoading($('.mlr-response-header').text() + " - Failure", responseText);
}

function postExport(responseHeader, success, error) {
	var agencyCode = $("#exportAgency").val();
	var siteNumber= $("#exportSite").val();
		
	if(agencyCode !== null && agencyCode.length > 0 && siteNumber !== null && siteNumber.length > 0) {
		//Build URL
		
		var url = "legacy/location/" + agencyCode + "/" + siteNumber;

		//Adjust UI
		startLoading(responseHeader);

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
		stopLoading(responseHeader + " - Error", "Site export parameters incomplete.");
	}
}

function postDdot(url, responseHeader, success, error) {
	//Grab File to Upload
	var documentData = new FormData($("#ddotForm")[0]);
	documentData.append('file', $('input[type=file]')[0].files[0]);
	
	//Check selected file
	if($('input[type=file]')[0].files.length > 0){
		//Adjust UI
		startLoading(responseHeader);

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