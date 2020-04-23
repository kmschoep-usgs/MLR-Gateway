package gov.usgs.wma.mlrgateway.util;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ParseCSV {

	public List<String[]> getMlList(MultipartFile inputCsvFile) throws CsvException, IOException {
		List<String[]> mlList = new ArrayList<>();
		Reader reader = new InputStreamReader(inputCsvFile.getInputStream());	
		CSVParser parser = new CSVParserBuilder()
			.withSeparator(',')
			.withIgnoreQuotations(false)
			.build();
		CSVReader csvReader = new CSVReaderBuilder(reader)
			.withSkipLines(1)
			.withCSVParser(parser)
			.build();
		
		mlList = csvReader.readAll();
		csvReader.close();
	    reader.close();
		return mlList;
	}
}
	

