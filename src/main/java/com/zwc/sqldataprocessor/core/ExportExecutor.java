package com.zwc.sqldataprocessor.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.exporter.CsvExporter;

public class ExportExecutor {
    public static String export(String resultName, DataList table, String path) {

        // 创建结果目录
        try {
            Path outputDirectoryPath = Paths.get("./output");
            if (!Files.exists(outputDirectoryPath)) {
                Files.createDirectory(outputDirectoryPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 确定导出文件路径
        if (path == null) {
            path = String.format("./output/%s.csv", resultName);
        } else if (!path.contains("/") && !path.contains("\\")) {
            path = String.format("./output/%s.csv", path);
        }

        // 执行导出
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
