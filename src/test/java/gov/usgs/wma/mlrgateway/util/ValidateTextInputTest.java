package gov.usgs.wma.mlrgateway.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;

@ExtendWith(SpringExtension.class)
public class ValidateTextInputTest {
	private ValidateTextInput validator;
	
	private String validReasonText = "update primary key 123";
	private String validReasonTextBlank = "";

	@BeforeEach
	public void init() throws Exception {
		validator = new ValidateTextInput();
	}

	@Test
	public void success_validText() throws Exception {
		String testStr = validator.validateInput(validReasonText);
		
		assertEquals(validReasonText, testStr);
	}
	
	@Test
	public void success_validTextBlank() throws Exception {
		String testStr = validator.validateInput(validReasonTextBlank);
		
		assertEquals(validReasonTextBlank, testStr);
	}
	
	@Test
	public void success_inValidText() throws Exception {
		String testStr = "";
		try {
			testStr = validator.validateInput("update primary key-");
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals(ValidateTextInput.INTERNAL_ERROR_MESSAGE , ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
		
		try {
			testStr = validator.validateInput("update primary. key");
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals(ValidateTextInput.INTERNAL_ERROR_MESSAGE , ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
		
		try {
			testStr = validator.validateInput("(update primary key)");
			fail("Did not get expected Exception.");
		} catch (Exception e) {
			if (e instanceof FeignBadResponseWrapper) {
				assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ((FeignBadResponseWrapper) e).getStatus());
				JSONAssert.assertEquals(ValidateTextInput.INTERNAL_ERROR_MESSAGE , ((FeignBadResponseWrapper) e).getBody(), JSONCompareMode.STRICT);
			}
		}
	}
	
	
}