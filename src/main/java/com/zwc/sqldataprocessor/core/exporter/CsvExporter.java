package com.zwc.sqldataprocessor.core.exporter;

import java.io.IOException;

import java.util.List;
import com.zwc.sqldataprocessor.core.entity.DataList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CsvExporter {

    public byte[] export(DataList table) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, CSVFormat.DEFAULT);
            csvPrinter.printRecord(table.columns);
            for (List<String> values : table.rows) {
                csvPrinter.printRecord(values);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return stringBuilder.toString().getBytes();
    }

}
