package com.zwc.sqldataprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.sql.ExportStatement;
import com.zwc.sqldataprocessor.exporter.CsvExporter;
import com.zwc.sqldataprocessor.exporter.Exporter;
import com.zwc.sqldataprocessor.exporter.XlsxExporter;

public class ExportExecutor {
    static XlsxExporter xlsxExporter = new XlsxExporter();
    static CsvExporter csvExporter = new CsvExporter();

    public static List<String> exportedPaths = new ArrayList<>();

    public static void flushAllFiles() {
        xlsxExporter.flushAllFiles();
        csvExporter.flushAllFiles();
    }

    public static String export(String resultName, DataList table, ExportStatement statement) {

        // 自动根据文件名称推断导出格式
        String path = statement.filePath;
        if (path != null && path.toLowerCase().endsWith(xlsxExporter.getExtension())) {
            statement.exportXlsx = true;
        } else if (path != null && path.toLowerCase().endsWith(csvExporter.getExtension())) {
            statement.exportXlsx = false;
        }

        Exporter exporter = statement.exportXlsx ? new XlsxExporter() : new CsvExporter();

        // 自动使用数据集的名称作为导出名称
        if (path == null) {
            path = resultName;
        }

        // 自动添加文件后缀
        if (!path.toLowerCase().endsWith("." + exporter.getExtension())) {
            path = path + "." + exporter.getExtension();
        }

        // 自动补全文件路径
        if (!path.contains("/") && !path.contains("\\")) {
            path = FileHelper.getOutPath(path);
        }

        // 第一次导出要先删除之前的老文件
        if (!exportedPaths.contains(path)) {
            exportedPaths.add(path);
            FileHelper.deleteFile(path);
        }

        // 执行导出
        exporter.export(path, table, statement.sheetName, statement.exportNulls);
        return path;
    }
}
