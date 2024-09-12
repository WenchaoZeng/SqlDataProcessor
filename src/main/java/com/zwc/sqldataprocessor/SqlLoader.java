package com.zwc.sqldataprocessor;

import java.util.ArrayList;

import java.util.List;

import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.entity.UserException;
import com.zwc.sqldataprocessor.entity.sql.EndStatement;
import com.zwc.sqldataprocessor.entity.sql.ExportStatement;
import com.zwc.sqldataprocessor.entity.sql.ImportStatement;
import com.zwc.sqldataprocessor.entity.sql.SqlStatement;
import com.zwc.sqldataprocessor.entity.sql.Statement;
import org.apache.commons.lang3.StringUtils;

public class SqlLoader {
    static boolean exportNulls;
    static boolean exportXlsx;
    static boolean tempTables;

    public static List<Statement> loadSql(String filePath) {
        exportNulls = false;
        exportXlsx = false;
        tempTables = false;
        String fileContent = FileHelper.readFile(filePath);
        List<Statement> statements = new ArrayList<>();

        for (String line : fileContent.split("\n")) {

            if (line.startsWith("# ")) {
                line = line.trim();
                String lowerLine = line.toLowerCase();

                if (lowerLine.equals("# end")) {

                    // 确保最后有一个导出语句
                    tryAddExportToEnd(statements);

                    statements.add(new EndStatement());
                    return statements;
                }

                if (lowerLine.equals("# exportnulls")) {
                    exportNulls = true;
                    continue;
                }

                if (lowerLine.equals("# -exportnulls")) {
                    exportNulls = false;
                    continue;
                }

                if (lowerLine.equals("# exportxlsx")) {
                    exportXlsx = true;
                    continue;
                }

                if (lowerLine.equals("# -exportxlsx")) {
                    exportXlsx = false;
                    continue;
                }

                if (lowerLine.equals("# temptables")) {
                    tempTables = true;
                    continue;
                }

                if (lowerLine.equals("# -temptables")) {
                    tempTables = false;
                    continue;
                }

                // 导入
                if (line.startsWith("# import ")) {
                    ImportStatement statement = new ImportStatement();
                    statement.filePath = line.replace("# import ", "");
                    statement.filePath = removeResultNameClause(statement.filePath);
                    statement.sheetName = getSheetName(statement.filePath);
                    statement.headRowNo = getHeadRowNo(statement.filePath);
                    statement.filePath = removeSheetName(statement.filePath);
                    statement.resultName = getResultName(line);
                    statements.add(statement);
                    continue;
                }

                // 导出
                if (line.startsWith("# export")) {
                    ExportStatement statement = new ExportStatement();
                    statement.exportNulls = exportNulls;
                    statement.exportXlsx = exportXlsx;
                    String exportFilePath = line.replace("# export", "").trim();
                    if (!exportFilePath.equals("")) {
                        statement.filePath = exportFilePath;
                    }
                    statements.add(statement);
                    continue;
                }

                // db名称
                String databaseName = line.replaceFirst("# ", "");
                databaseName = removeResultNameClause(databaseName);
                DatabaseConfig dbConfig = DatabaseConfigLoader.getDbConfig(databaseName);
                if (dbConfig != null) {
                    SqlStatement statement = new SqlStatement();
                    statement.databaseName = databaseName;
                    statement.sql = "";
                    statement.resultName = getResultName(line);
                    statement.tempTables = tempTables;
                    statements.add(statement);
                    continue;
                }
            }

            // 读取sql语句
            if (statements.size() <= 0) {
                continue;
            }
            Statement lastStatement = statements.get(statements.size() - 1);
            if (!(lastStatement instanceof SqlStatement)) {
                continue;
            }
            SqlStatement sqlStatement = ((SqlStatement)lastStatement);
            if (sqlStatement.sql == "") {
                sqlStatement.sql = line;
                continue;
            }
            sqlStatement.sql += "\n" + line;
        }

        // 确保最后有一个导出语句
        tryAddExportToEnd(statements);

        return statements;
    }

    /**
     * 在语句后面自动加上一个导出语句
     */
    static void tryAddExportToEnd(List<Statement> statements) {
        if (statements.size() <= 0) {
            return;
        }

        if (!(statements.get(statements.size() - 1) instanceof ExportStatement)) {
            ExportStatement statement = new ExportStatement();
            statement.exportNulls = exportNulls;
            statement.exportXlsx = exportXlsx;
            statements.add(statement);
        }
    }

    static String getResultName(String line) {
        int index = line.lastIndexOf(" as $");
        return index > 0 ? line.substring(index + 5) : "table";
    }

    static String removeResultNameClause(String line) {
        int index = line.lastIndexOf(" as $");
        return index >= 0 ? line.substring(0, index) : line;
    }

    /**
     * 在文件路径后使用方括号包含着的就是sheet名称
     * 示例: /root/test.xlsx[sheet名称]
     */
    static String getSheetName(String filePath) {
        int extIndex = getExtIndex(filePath);
        int leftBracketIndex = filePath.indexOf("[", extIndex);
        if (leftBracketIndex < 0) {
            return null;
        }

        int rightBracketIndex = filePath.indexOf("]", leftBracketIndex + 1);
        if (rightBracketIndex < 0) {
            rightBracketIndex = filePath.length();
        }

        return filePath.substring(leftBracketIndex + 1, rightBracketIndex).trim();
    }

    /**
     * 在文件路径后第二个方括号包含着的就是表头行号
     * 示例: /root/test.xlsx[sheet名称][2]
     */
    static int getHeadRowNo(String filePath) {
        int extIndex = getExtIndex(filePath);
        int leftBracketIndex = filePath.indexOf("[", extIndex);
        if (leftBracketIndex < 0) {
            return 1;
        }

        int rightBracketIndex = filePath.indexOf("]", leftBracketIndex + 1);
        if (rightBracketIndex < 0) {
            return 1;
        }

        int leftBracketIndex2 = filePath.indexOf("[", rightBracketIndex + 1);
        if (leftBracketIndex2 < 0) {
            return 1;
        }

        int rightBracketIndex2 = filePath.indexOf("]", leftBracketIndex2 + 1);
        if (rightBracketIndex2 < 0) {
            return 1;
        }

        String rowNoStr = filePath.substring(leftBracketIndex2 + 1, rightBracketIndex2).trim();
        if (StringUtils.isBlank(rowNoStr)) {
            return 1;
        }

        int rowNo;
        try {
            rowNo = Integer.parseInt(rowNoStr);
        } catch (NumberFormatException e) {
            throw new UserException("表头行号必须是一个整数: " + rowNoStr);
        }

        if (rowNo < 1) {
            throw new UserException("表头行号必须是一个正整数: " + rowNoStr);
        }

        return rowNo;
    }

    static String removeSheetName(String filePath) {
        int extIndex = getExtIndex(filePath);
        int leftBracketIndex = filePath.indexOf("[", extIndex);
        if (leftBracketIndex < 0) {
            return filePath.trim();
        }

        return filePath.substring(0, leftBracketIndex).trim();
    }

    /**
     * 获取文件后缀的下标
     */
    static int getExtIndex(String filePath) {
        filePath = filePath.toLowerCase();
        for (String ext : ImportExecutor.importers.keySet()) {
            int index = filePath.lastIndexOf(ext);
            if (index > 0) {
                return index;
            }
        }

        throw new UserException("不支持导入格式, 请检查文件名后缀是否正确: " + filePath);
    }
}
