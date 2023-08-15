package com.zwc.sqldataprocessor;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import java.util.List;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.Sql;
import com.zwc.sqldataprocessor.entity.Sql.SqlType;

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
        String lastResultName = defaultResultName;
        for (Sql sql : sqlList) {

            logPrinter.accept("==============================");
            long startTime = System.currentTimeMillis();

            // 导入
            if (sql.type == SqlType.IMPORT) {
                logPrinter.accept("导入: " + sql.fileName + (sql.sheetName != null ? ", sheet: " + sql.sheetName : ""));
                dataList = ImportExecutor.doImport(sql.fileName, sql.sheetName);
                lastResultName = sql.resultName != null ? sql.resultName : defaultResultName;
                tables.put(lastResultName, dataList);
                printSqlStatus(lastResultName, dataList, logPrinter, startTime);
            }

            // 数据库查询
            if (sql.type == SqlType.SQL) {
                logPrinter.accept("SQL: " + sql.databaseName);
                logPrinter.accept(sql.sql);
                dataList = SqlExecutor.exec(sql.sql, sql.databaseName, tables);
                lastResultName = sql.resultName != null ? sql.resultName : defaultResultName;
                tables.put(lastResultName, dataList);
                printSqlStatus(lastResultName, dataList, logPrinter, startTime);
            }

            // 导出
            if (sql.type == SqlType.EXPORT || sql.type == SqlType.END) {
                doExport(lastResultName, dataList, logPrinter, sql.fileName, sql.exportNulls);

                // 提前结束
                if (sql.type == SqlType.END) {
                    logPrinter.accept("结束");
                    break;
                }
            }
        }
    }

    static void doExport(String resultName, DataList dataList, Consumer<String> logPrinter, String filePath, boolean exportNulls) {
        // 导出
        logPrinter.accept("导出结果集" + resultName);
        String exportPath = ExportExecutor.export(resultName, dataList, filePath, exportNulls);
        exportPath = Paths.get(exportPath).toFile().getAbsolutePath();
        logPrinter.accept("导出文件路径为: " + exportPath);

        // 自动打开
        FileHelper.openFile(exportPath);
    }

    static void printSqlStatus(String resultName, DataList dataList, Consumer<String> logPrinter, long startTime) {
        String msg = String.format("结果集: %s, 行数: %s, 耗时: %s毫秒", resultName, dataList.rows.size(), System.currentTimeMillis() - startTime);
        logPrinter.accept(msg);
    }
}
