package com.zwc.sqldataprocessor;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.exporter.CsvExporter;

public class ExportExecutor {
    public static String export(String resultName, DataList table, String path, boolean exportNulls) {

        // 执行导出
        CsvExporter exporter = new CsvExporter();
        byte[] bytes = exporter.export(table, exportNulls);

        // 写入导出文件
        if (path == null) {
            path = String.format("./%s.csv", resultName);
            path = FileHelper.writeOutputFile(path, bytes);
            return path;
        } else if (!path.contains("/") && !path.contains("\\")) {
            path = String.format("./%s.csv", path);
            path = FileHelper.writeOutputFile(path, bytes);
            return path;
        }

        FileHelper.writeFile(path, bytes);
        return path;
    }
}
