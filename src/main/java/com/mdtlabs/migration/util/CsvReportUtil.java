package com.mdtlabs.migration.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.mdtlabs.migration.model.Constants;
import com.opencsv.CSVWriter;

public class CsvReportUtil {


    public static void writeDynamicCsv(String fileName,List<String> headers,List<String[]> dataList) throws IOException {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("Data list is empty. Cannot generate CSV.");
        }
        File directory = new File(Constants.CSV_LOG);
        if (!directory.exists()) {
            directory.mkdirs(); // create folder if it doesn't exist
        }
        String filePath = Constants.CSV_LOG+Constants.FORWARD_SLASH+Constants.CSV_LOG+Constants.UNDER_SCORE+fileName +
                new SimpleDateFormat(Constants.CSV_DATE_FORMAT).format(new Date()) + Constants.CSV_FORMAT;
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            writer.writeNext(headers.toArray(new String[0]));

            for (String[] row : dataList) {
                writer.writeNext(row);
            }
        }
    }
}
