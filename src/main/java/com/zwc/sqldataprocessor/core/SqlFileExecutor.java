package com.zwc.sqldataprocessor.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import java.util.List;

import com.zwc.sqldataprocessor.Global;
import com.zwc.sqldataprocessor.Log;
import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.core.entity.Sql;
import com.zwc.sqldataprocessor.core.entity.Sql.SqlType;
import com.zwc.sqldataprocessor.core.exporter.CsvExporter;

public class SqlFileExecutor {

    static String defaultResultName = "table";

    public static void exec(String filePath, Consumer<String> logPrinter) {
        try {
            internalExec(filePath, logPrinter);
        } finally {
            DatabaseConfigLoader.closeConnections();
        }
    }

    static void internalExec(String filePath, Consumer<String> logPrinter) {

        logPrinter.accept("执行:  " + filePath);

        List<Sql> sqlList = SqlLoader.loadSql(filePath);
        if (sqlList.size() <= 0) {
            logPrinter.accept("文件里没有可执行的sql命令");
            return;
        }

        Map<String, DataList> tables = new HashMap<>();

        // 执行
        DataList dataList = null;
        String resultName = defaultResultName;
        for (Sql sql : sqlList) {

            logPrinter.accept("==============================");

            long startTime = System.currentTimeMillis();
            resultName = sql.resultName != null ? sql.resultName : defaultResultName;

            // 导入
            if (sql.type == SqlType.IMPORT) {
                logPrinter.accept("导入: " + sql.fileName);
                dataList = ImportExecutor.doImport(sql.fileName);
                tables.put(resultName, dataList);
                printSqlStatus(resultName, dataList, logPrinter, startTime);
            }

            // 数据库查询
            if (sql.type == SqlType.SQL) {
                logPrinter.accept("SQL: " + sql.databaseName);
                logPrinter.accept(sql.sql);
                dataList = SqlExecutor.exec(sql.sql, sql.databaseName, tables);
                tables.put(resultName, dataList);
                printSqlStatus(resultName, dataList, logPrinter, startTime);
            }

            // 导出
            if (sql.type == SqlType.EXPORT) {
                doExport(resultName, dataList, logPrinter, sql.fileName);
            }
        }

        // 导出最后的结果集
        if (sqlList.get(sqlList.size() - 1).type != SqlType.EXPORT) {
            logPrinter.accept("==============================");
            doExport(resultName, dataList, logPrinter, null);
        }

    }

    static void doExport(String resultName, DataList dataList, Consumer<String> logPrinter, String filePath) {
        // 导出
        logPrinter.accept("导出结果集" + resultName);
        String exportPath = ExportExecutor.export(resultName, dataList, filePath);
        logPrinter.accept("导出文件路径为: " + exportPath);

        // 自动打开
        Global.openFile(exportPath);
    }

    static void printSqlStatus(String resultName, DataList dataList, Consumer<String> logPrinter, long startTime) {
        String msg = String.format("结果集: %s, 行数: %s, 耗时: %s毫秒", resultName, dataList.rows.size(), System.currentTimeMillis() - startTime);
        logPrinter.accept(msg);
    }
}
