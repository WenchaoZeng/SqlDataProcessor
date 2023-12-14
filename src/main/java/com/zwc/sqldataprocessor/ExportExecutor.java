package com.zwc.sqldataprocessor;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.sql.ExportStatement;
import com.zwc.sqldataprocessor.exporter.CsvExporter;
import com.zwc.sqldataprocessor.exporter.Exporter;
import com.zwc.sqldataprocessor.exporter.XlsxExporter;

public class ExportExecutor {
    public static String export(String resultName, DataList table, ExportStatement statement) {

        // 执行导出
        Exporter exporter = statement.exportXlsx ? new XlsxExporter() : new CsvExporter();
        byte[] bytes = exporter.export(table, statement.exportNulls);

        // 写入导出文件
        String path = statement.filePath;
        if (path == null) { // 默认文件名为数据集的名称
            path = String.format("./%s.%s", resultName, exporter.getExtension());
            path = FileHelper.writeOutputFile(path, bytes);
            return path;
        } else if (!path.contains("/") && !path.contains("\\")) { // 指定一个文件名称
            path = String.format("./%s.%s", path, exporter.getExtension());
            path = FileHelper.writeOutputFile(path, bytes);
            return path;
        }

        // 指定一个完整的文件路径
        FileHelper.writeFile(path, bytes);
        return path;
    }
}
