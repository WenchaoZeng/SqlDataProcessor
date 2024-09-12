package com.zwc.sqldataprocessor;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import java.util.List;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.sql.EndStatement;
import com.zwc.sqldataprocessor.entity.sql.ExportStatement;
import com.zwc.sqldataprocessor.entity.sql.ImportStatement;
import com.zwc.sqldataprocessor.entity.sql.SqlStatement;
import com.zwc.sqldataprocessor.entity.sql.Statement;
import org.apache.commons.lang3.StringUtils;

public class SqlFileExecutor {

    public static void exec(String filePath, Consumer<String> logPrinter) {
        try {
            internalExec(filePath, logPrinter);
        } finally {
            DatabaseConfigLoader.closeConnections();
        }
    }

    static void internalExec(String filePath, Consumer<String> logPrinter) {

        logPrinter.accept("执行:  " + filePath);

        List<Statement> statements = SqlLoader.loadSql(filePath);
        if (statements.size() <= 0) {
            logPrinter.accept("文件里没有可执行的sql命令");
            return;
        }

        Map<String, DataList> tables = new HashMap<>();

        // 执行
        String lastResultName = null;
        for (Statement statement : statements) {

            logPrinter.accept("==============================");
            long startTime = System.currentTimeMillis();

            // 导入
            if (statement instanceof ImportStatement) {
                ImportStatement importStatement = (ImportStatement)statement;
                logPrinter.accept("导入: " + importStatement.filePath + (StringUtils.isNotBlank(importStatement.sheetName) ? ", sheet: " + importStatement.sheetName : ""));
                DataList dataList = ImportExecutor.doImport(importStatement);
                tables.put(importStatement.resultName, dataList);
                lastResultName = importStatement.resultName;
                printStatus(lastResultName, dataList, logPrinter, startTime);
            }

            // 数据库查询
            if (statement instanceof SqlStatement) {
                SqlStatement sqlStatement = (SqlStatement) statement;
                logPrinter.accept("执行SQL (DB名: " + sqlStatement.databaseName + ")");
                logPrinter.accept(sqlStatement.sql);
                DataList dataList = SqlExecutor.exec(sqlStatement, tables);
                lastResultName = sqlStatement.resultName;
                tables.put(lastResultName, dataList);
                printStatus(lastResultName, dataList, logPrinter, startTime);
            }

            // 导出
            if (statement instanceof ExportStatement) {
                doExport(lastResultName, tables.get(lastResultName), (ExportStatement) statement, logPrinter);
            }

            // 提前结束
            if (statement instanceof EndStatement) {
                logPrinter.accept("结束");
                break;
            }
        }
    }

    static void doExport(String resultName, DataList dataList, ExportStatement statement, Consumer<String> logPrinter) {
        // 导出
        logPrinter.accept("导出结果集: " + resultName);
        String exportPath = ExportExecutor.export(resultName, dataList, statement);
        exportPath = Paths.get(exportPath).toFile().getAbsolutePath();
        logPrinter.accept("导出文件路径: " + exportPath);

        // 自动打开
        FileHelper.openFile(exportPath);
    }

    static void printStatus(String resultName, DataList dataList, Consumer<String> logPrinter, long startTime) {
        String msg = String.format("结果集: %s, 行数: %s, 耗时: %s毫秒", resultName, dataList.rows.size(), System.currentTimeMillis() - startTime);
        logPrinter.accept(msg);
    }
}
