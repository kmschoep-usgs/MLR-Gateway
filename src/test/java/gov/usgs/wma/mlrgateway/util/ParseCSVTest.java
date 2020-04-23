package gov.usgs.wma.mlrgateway.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

@ExtendWith(SpringExtension.class)
public class ParseCSVTest {
	private ParseCSV parser;
	
	private static String fileContents = "agency_cd,site_no\nUSGS,01234\nblah,05431";
	private static String fileContentsQuotes = "\"agency_cd\",\"site_no\"\n\"USGS\",\"01234\"\n\"blah\",\"05431\"";

	@BeforeEach
	public void init() throws Exception {
		parser = new ParseCSV();
	}

	@Test
	public void success_getList() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", ".csv", "text/plain", fileContents.getBytes());
		List<String[]> mlList = parser.getMlList(file);
		
		assertEquals("USGS", mlList.get(0)[0]);
		assertEquals("01234", mlList.get(0)[1]);
		assertEquals("blah", mlList.get(1)[0]);
		assertEquals("05431", mlList.get(1)[1]);
	}
	
	@Test
	public void success_quotes_getList() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", ".csv", "text/plain", fileContentsQuotes.getBytes());
		List<String[]> mlList = parser.getMlList(file);
		
		assertEquals("USGS", mlList.get(0)[0]);
		assertEquals("01234", mlList.get(0)[1]);
		assertEquals("blah", mlList.get(1)[0]);
		assertEquals("05431", mlList.get(1)[1]);
	}
	
	
}