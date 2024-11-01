package com.zwc.sqldataprocessor.exporter;

import java.io.IOException;

import java.util.List;

import com.zwc.sqldataprocessor.FileHelper;
import com.zwc.sqldataprocessor.entity.DataList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CsvExporter implements Exporter {

    public void export(String filePath, DataList table, String sheetName, boolean exportNulls) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, CSVFormat.DEFAULT);
            csvPrinter.printRecord(table.columns);
            String[] csvValues = new String[table.columns.size()];
            for (List<String> values : table.rows) {
                for (int columnIndex = 0; columnIndex < csvValues.length; ++columnIndex) {
                    String value = values.get(columnIndex);
                    csvValues[columnIndex] = value == null && exportNulls ? "<null>" : value;
                }
                csvPrinter.printRecord(csvValues);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FileHelper.writeFile(filePath, stringBuilder.toString());
    }

    @Override
    public String getExtension() {
        return "csv";
    }

}
