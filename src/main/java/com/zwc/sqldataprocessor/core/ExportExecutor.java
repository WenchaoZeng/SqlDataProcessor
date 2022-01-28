package com.zwc.sqldataprocessor.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.zwc.sqldataprocessor.Global;
import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.exporter.CsvExporter;

public class ExportExecutor {
    public static String export(String resultName, DataList table, String path) {

        // 执行导出
        CsvExporter exporter = new CsvExporter();
        byte[] bytes = exporter.export(table);

        // 写入导出文件
        if (path == null) {
            path = String.format("./%s.csv", resultName);
            path = Global.writeOutputFile(path, bytes);
            return path;
        } else if (!path.contains("/") && !path.contains("\\")) {
            path = String.format("./%s.csv", path);
            path = Global.writeOutputFile(path, bytes);
            return path;
        }

        Global.writeFile(path, bytes);
        return path;
    }
}
