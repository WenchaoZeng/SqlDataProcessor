package com.zwc.sqldataprocessor;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.exporter.CsvExporter;
import com.zwc.sqldataprocessor.exporter.Exporter;
import com.zwc.sqldataprocessor.exporter.XlsxExporter;

public class ExportExecutor {
    public static String export(String resultName, DataList table, String path, boolean exportNulls, boolean exportXlsx) {

        // 执行导出
        Exporter exporter = exportXlsx ? new XlsxExporter() : new CsvExporter();
        byte[] bytes = exporter.export(table, exportNulls);

        // 写入导出文件
        if (path == null) {
            path = String.format("./%s.%s", resultName, exporter.getExtension());
            path = FileHelper.writeOutputFile(path, bytes);
            return path;
        } else if (!path.contains("/") && !path.contains("\\")) {
            path = String.format("./%s.%s", path, exporter.getExtension());
            path = FileHelper.writeOutputFile(path, bytes);
            return path;
        }

        FileHelper.writeFile(path, bytes);
        return path;
    }
}
