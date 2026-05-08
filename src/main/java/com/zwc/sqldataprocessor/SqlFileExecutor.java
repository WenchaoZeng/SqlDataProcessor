package com.zwc.sqldataprocessor;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.UserException;
import com.zwc.sqldataprocessor.entity.sql.CallStatement;
import com.zwc.sqldataprocessor.entity.sql.ExportStatement;
import com.zwc.sqldataprocessor.entity.sql.GotoStatement;
import com.zwc.sqldataprocessor.entity.sql.ImportStatement;
import com.zwc.sqldataprocessor.entity.sql.LabelStatement;
import com.zwc.sqldataprocessor.entity.sql.SqlStatement;
import com.zwc.sqldataprocessor.entity.sql.Statement;
import com.zwc.sqldataprocessor.executor.ExportExecutor;
import com.zwc.sqldataprocessor.executor.ImportExecutor;
import com.zwc.sqldataprocessor.executor.LocalToolExectuor;
import com.zwc.sqldataprocessor.executor.SqlExecutor;
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
            throw new UserException("SQL文件里没有可执行的命令");
        }

        Map<String, DataList> tables = new HashMap<>();
        Map<String, Integer> labelPositions = buildLabelPositions(statements);

        // 执行
        String lastResultName = null;
        for (int statementIndex = 0; statementIndex < statements.size(); ++statementIndex) {
            Statement statement = statements.get(statementIndex);

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
                logPrinter.accept("执行SQL (" + sqlStatement.databaseName + "): ");
                logPrinter.accept(sqlStatement.sql);
                DataList dataList = SqlExecutor.exec(sqlStatement, tables);
                lastResultName = sqlStatement.resultName;
                tables.put(lastResultName, dataList);
                printStatus(lastResultName, dataList, logPrinter, startTime);
            }

            if (statement instanceof LabelStatement) {
                logPrinter.accept("标签: " + ((LabelStatement) statement).labelName);
            }

            if (statement instanceof GotoStatement) {
                GotoStatement gotoStatement = (GotoStatement) statement;
                DataList dataList = lastResultName == null ? null : tables.get(lastResultName);
                if (!hasRows(dataList)) {
                    logPrinter.accept("跳过goto: " + gotoStatement.labelName + "，上一个SQL结果集无数据");
                    continue;
                }

                Integer nextIndex = labelPositions.get(gotoStatement.labelName);
                if (nextIndex == null) {
                    throw new UserException("未找到label: " + gotoStatement.labelName);
                }

                logPrinter.accept("跳转到label: " + gotoStatement.labelName);
                statementIndex = nextIndex;
                continue;
            }

            // 执行本地工具
            if (statement instanceof CallStatement && lastResultName != null) {
                DataList dataList = tables.get(lastResultName);
                CallStatement callStatement = (CallStatement) statement;
                logPrinter.accept("执行本地工具: " + callStatement.command);
                LocalToolExectuor.exec(callStatement, dataList, logPrinter);
            }

            // 导出
            if (statement instanceof ExportStatement) {
                doExport(lastResultName, tables.get(lastResultName), (ExportStatement) statement, logPrinter);
            }
        }

        // 自动打开导出的文件
        ExportExecutor.flushAllFiles();
        for (String exportedPath : ExportExecutor.exportedPaths) {
            FileHelper.openFile(exportedPath);
        }

    }

    static Map<String, Integer> buildLabelPositions(List<Statement> statements) {
        Map<String, Integer> labelPositions = new LinkedHashMap<>();
        for (int i = 0; i < statements.size(); ++i) {
            Statement statement = statements.get(i);
            if (!(statement instanceof LabelStatement)) {
                continue;
            }

            String labelName = ((LabelStatement) statement).labelName;
            if (labelPositions.containsKey(labelName)) {
                throw new UserException("label重复定义: " + labelName);
            }
            labelPositions.put(labelName, i);
        }
        return labelPositions;
    }

    static boolean hasRows(DataList dataList) {
        return dataList != null && dataList.rows != null && dataList.rows.size() > 0;
    }

    static void doExport(String resultName, DataList dataList, ExportStatement statement, Consumer<String> logPrinter) {
        // 导出
        logPrinter.accept("导出结果集: " + resultName);
        String exportPath = ExportExecutor.export(resultName, dataList, statement);
        exportPath = Paths.get(exportPath).toFile().getAbsolutePath();
        logPrinter.accept("导出文件路径: " + exportPath);
        if (StringUtils.isNotBlank(statement.sheetName)) {
            logPrinter.accept("sheet名称: " + statement.sheetName);
        }
    }

    static void printStatus(String resultName, DataList dataList, Consumer<String> logPrinter, long startTime) {
        String msg = String.format("结果集: %s, 行数: %s, 耗时: %s毫秒", resultName, dataList.rows.size(), System.currentTimeMillis() - startTime);
        logPrinter.accept(msg);
    }
}
