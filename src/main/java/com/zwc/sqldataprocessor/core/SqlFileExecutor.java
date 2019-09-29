package com.zwc.sqldataprocessor.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import java.util.List;
import com.zwc.sqldataprocessor.Log;
import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.core.entity.Sql;
import com.zwc.sqldataprocessor.core.exporter.CsvExporter;

public class SqlFileExecutor {

    static String defaultResultName = "table";

    public static void exec(String filePath, Consumer<String> logPrinter) {

        logPrinter.accept("执行:  " + filePath);

        List<Sql> sqlList = SqlLoader.loadSql(filePath);
        List<DatabaseConfig> configList = DatabaseConfigLoader.loadDatabaseConfigs();
        Map<String, DataList> tables = new HashMap<>();

        // 执行
        for (Sql sql : sqlList) {

            long startTime = System.currentTimeMillis();
            DataList dataList = null;
            String resultName = sql.resultName != null ? sql.resultName : defaultResultName;

            // 导入
            if (sql.fileName != null) {
                logPrinter.accept("导入: " + sql.fileName);
                dataList = ImportExecutor.doImport(sql.fileName);
                tables.put(resultName, dataList);
            }

            // 数据库查询
            if (sql.databaseName != null) {
                logPrinter.accept("SQL: " + sql.databaseName);
                logPrinter.accept(sql.sql);
                dataList = SqlExecutor.exec(sql.sql, sql.databaseName, configList, tables);
                tables.put(resultName, dataList);
            }

            String msg = String.format("子结果: %s, 行数: %s, 耗时: %s毫秒", resultName, dataList.rows.size(), System.currentTimeMillis() - startTime);
            logPrinter.accept(msg);
        }

        // 获取最终结果集
        DataList finalDataList = tables.get(defaultResultName);
        if (finalDataList == null || finalDataList.rows.size() <= 0) {
            logPrinter.accept("最终结果集为空");
            return;
        }

        // 导出
        logPrinter.accept("导出结果...\n");
        String exportPath = ExportExecutor.export(finalDataList);

        // 自动打开
        try {
            Runtime.getRuntime().exec("open " + exportPath);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }


    }
}
