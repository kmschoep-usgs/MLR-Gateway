function generalDdotSuccess (response) {
	$("#ddotForm :input").prop("disabled", false);
	$('#loading-spinner').removeClass('spinner');
	$('#response-header').text("Upload Response - Success");
	$('#response-text').text(JSON.stringify(response));
}

function generalDdotError (response) {
	$("#ddotForm :input").prop("disabled", false);
	$('#loading-spinner').removeClass('spinner');
	
	if(response.status > 0){
		$('#response-header').text("Upload Response - Failure");
		if(response.hasOwnProperty("responseJSON")){
			$('#response-text').text(JSON.stringify(response.responseJSON));
		} else if(response.hasOwnProperty("responseText")) {
			$('#response-text').text(JSON.stringify(JSON.parse(response.responseText)));
		} else {
			$('#response-text').text(JSON.stringify(response));
		}
	} else {
		$('#response-header').text("Upload Response - Error");
		$('#response-text').text("Connection Error with Gateway Service. If this issue persists please contact the support team.");
	}
}

function postDdot(url, success, error) {
	//Grab File to Upload
	var documentData = new FormData($("#ddotForm")[0]);
	documentData.append('file', $('input[type=file]')[0].files[0]);
	
	//Check selected file
	if($('input[type=file]')[0].files.length > 0){
		//Adjust UI
		$("#ddotForm :input").prop("disabled", true);
		$('#ddot-response').show();
		$('#loading-spinner').addClass('spinner');
		$('#response-text').text("");
		$('#response-header').text("Upload Response");

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
		$('#ddot-response').show();
		$('#response-text').text("No file selected.");
		$('#response-header').text("Upload Response - Error");
	}
}

function validateDdot() {
	postDdot("workflows/ddots/validate", generalDdotSuccess, generalDdotError)
}

function uploadDdot() {
	postDdot("workflows/ddots", generalDdotSuccess, generalDdotError)
}