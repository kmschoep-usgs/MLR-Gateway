package gov.usgs.wma.mlrgateway.util;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.controller.BulkTransactionFilesWorkflowController;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class ParseCSV {
	private Logger log = LoggerFactory.getLogger(ParseCSV.class);
	
	protected static final String STEP_NAME = "Parse CSV File";
	protected static final String SUCCESS_MESSAGE = "CSV file parsed successfully.";
	
	public List<String[]> getMlList(MultipartFile inputCsvFile) throws CsvException, IOException {
		List<String[]> mlList = new ArrayList<>();
		try (Reader reader = new InputStreamReader(inputCsvFile.getInputStream())){
			CSVParser parser = new CSVParserBuilder()
					.withSeparator(',')
					.withIgnoreQuotations(false)
					.build();	
				try(CSVReader csvReader = new CSVReaderBuilder(reader)
					.withSkipLines(1)
					.withCSVParser(parser)
					.build()){
					mlList = csvReader.readAll();
				} catch (Exception e) {
					log.error("Error reading input File: ", e.getMessage());
					throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Error reading input File: " + e.getMessage());
				}
		} catch (Exception e2) {
			log.error("Error reading CSV File: ", e2.getMessage());
			throw new FeignBadResponseWrapper(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Error reading CSV File: " + e2.getMessage());
		}	
		return mlList;
	}
}
	

