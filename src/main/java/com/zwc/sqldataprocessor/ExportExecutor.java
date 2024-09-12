package com.zwc.sqldataprocessor;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.sql.ExportStatement;
import com.zwc.sqldataprocessor.exporter.CsvExporter;
import com.zwc.sqldataprocessor.exporter.Exporter;
import com.zwc.sqldataprocessor.exporter.XlsxExporter;

public class ExportExecutor {
    static XlsxExporter xlsxExporter = new XlsxExporter();
    static CsvExporter csvExporter = new CsvExporter();

    public static String export(String resultName, DataList table, ExportStatement statement) {

        // 自动根据文件名称推断导出格式
        String path = statement.filePath;
        if (path != null && path.toLowerCase().endsWith(xlsxExporter.getExtension())) {
            statement.exportXlsx = true;
        } else if (path != null && path.toLowerCase().endsWith(csvExporter.getExtension())) {
            statement.exportXlsx = false;
        }

        // 执行导出
        Exporter exporter = statement.exportXlsx ? new XlsxExporter() : new CsvExporter();
        byte[] bytes = exporter.export(table, statement.exportNulls);

        // 使用数据集的名称作为导出名称
        if (path == null) {
            path = String.format("./%s.%s", resultName, exporter.getExtension());
            path = FileHelper.writeOutputFile(path, bytes);
            return path;
        }

        // 自动添加文件后缀
        if (!path.toLowerCase().endsWith(exporter.getExtension())) {
            path = String.format("%s.%s", path, exporter.getExtension());
        }

        // 仅指定一个文件名称
        if (!path.contains("/") && !path.contains("\\")) {
            path = "./"  + path;
            path = FileHelper.writeOutputFile(path, bytes);
            return path;
        }

        // 指定一个完整的文件路径
        FileHelper.writeFile(path, bytes);
        return path;
    }
}
