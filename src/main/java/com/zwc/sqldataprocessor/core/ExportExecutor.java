package com.zwc.sqldataprocessor.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.exporter.CsvExporter;

public class ExportExecutor {
    static int index = 0;
    public static String export(DataList table, String path) {
        ++index;
        index = index % 10;
        if (path == null) {
            path = String.format("./output%s.csv", index);
        }

        CsvExporter exporter = new CsvExporter();
        byte[] bytes = exporter.export(table);

        try {
            Files.write(Paths.get(path), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return path;
    }
}
