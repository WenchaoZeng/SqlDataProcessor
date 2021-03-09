package com.zwc.sqldataprocessor.core.importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import java.util.List;
import java.util.stream.Collectors;

import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.entity.DataList.ColumnType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvImporter implements Importer {

    @Override
    public DataList doImport(byte[] content) {
        try {
            Reader streamReader = new InputStreamReader(new ByteArrayInputStream(content));
            CSVParser csvParser = new CSVParser(streamReader, CSVFormat.EXCEL);
            DataList table = new DataList();
            for (CSVRecord record : csvParser.getRecords()) {
                List<String> values = new ArrayList<>();
                record.forEach(value -> values.add(value));
                if (table.columns == null) {
                    table.columns = values;
                    table.columnTypes = values.stream().map(x -> ColumnType.TEXT).collect(Collectors.toList());
                    continue;
                }
                if (values.size() < table.columns.size()) {
                    int count = table.columns.size() - values.size();
                    for (int i = 0; i < count; ++i) {
                        values.add("");
                    }
                }
                List<String> values2 = values.subList(0, table.columns.size());
                table.rows.add(values2);
            }

            return  table;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
