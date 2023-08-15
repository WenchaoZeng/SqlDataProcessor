package com.zwc.sqldataprocessor;

import java.util.ArrayList;

import java.util.List;

import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.entity.Sql;
import com.zwc.sqldataprocessor.entity.Sql.SqlType;

public class SqlLoader {
    public static List<Sql> loadSql(String filePath) {
        String fileContent = FileHelper.readFile(filePath);
        List<Sql> sqlList = new ArrayList<>();
        boolean exportNulls = false;
        for (String line : fileContent.split("\n")) {

            if (line.startsWith("# ")) {
                line = line.trim();

                if (line.startsWith("# end")) {
                    Sql endSql = new Sql();
                    endSql.type = SqlType.END;
                    sqlList.add(endSql);
                    continue;
                }

                if (line.startsWith("# no export nulls")) {
                    exportNulls = false;
                    continue;
                }

                if (line.startsWith("# export nulls")) {
                    exportNulls = true;
                    continue;
                }

                // 导入
                if (line.startsWith("# import ")) {
                    Sql importSql = new Sql();
                    importSql.type = SqlType.IMPORT;
                    importSql.fileName = line.replace("# import ", "");
                    importSql.fileName = removeResultNameClause(importSql.fileName);
                    importSql.sheetName = getSheetName(importSql.fileName);
                    importSql.fileName = removeSheetName(importSql.fileName);
                    importSql.resultName = getResultName(line);
                    sqlList.add(importSql);
                    continue;
                }

                // 导出
                if (line.startsWith("# export")) {
                    Sql exportSql = new Sql();
                    exportSql.type = SqlType.EXPORT;
                    exportSql.exportNulls = exportNulls;
                    String exportFilePath = line.replace("# export", "").trim();
                    if (!exportFilePath.equals("")) {
                        exportSql.fileName = exportFilePath;
                    }
                    sqlList.add(exportSql);
                    continue;
                }

                // db名称
                String databaseName = line.replaceFirst("# ", "");
                databaseName = removeResultNameClause(databaseName);
                DatabaseConfig dbConfig = DatabaseConfigLoader.getDbConfig(databaseName);
                if (dbConfig != null) {
                    Sql sql = new Sql();
                    sql.type = SqlType.SQL;
                    sql.databaseName = databaseName;
                    sql.sql = "";
                    sql.resultName = getResultName(line);
                    sqlList.add(sql);
                    continue;
                }
            }

            // 读取sql语句
            if (sqlList.size() <= 0) {
                continue;
            }
            Sql sql = sqlList.get(sqlList.size() - 1);
            if (sql.type != SqlType.SQL) {
                continue;
            }
            sql.sql += line + "\n";
        }

        // 确保最后有一个导出语句
        if (sqlList.size() > 0 && sqlList.get(sqlList.size() - 1).type != SqlType.EXPORT) {
            Sql exportSql = new Sql();
            exportSql.type = SqlType.EXPORT;
            exportSql.exportNulls = exportNulls;
            sqlList.add(exportSql);
        }

        return sqlList;
    }

    static String getResultName(String line) {
        int index = line.lastIndexOf(" as $");
        return index > 0 ? line.substring(index + 5) : null;
    }

    static String removeResultNameClause(String line) {
        int index = line.lastIndexOf(" as $");
        return index >= 0 ? line.substring(0, index) : line;
    }

    static String getSheetName(String filePath) {
        int extIndex = filePath.lastIndexOf(".");
        int leftBracketIndex = filePath.indexOf("[", extIndex);
        if (leftBracketIndex < 0) {
            return null;
        }

        return filePath.substring(leftBracketIndex + 1)
            .replace("]", "")
            .trim();
    }

    static String removeSheetName(String filePath) {
        int extIndex = filePath.lastIndexOf(".");
        int leftBracketIndex = filePath.indexOf("[", extIndex);
        if (leftBracketIndex < 0) {
            return filePath.trim();
        }

        return filePath.substring(0, leftBracketIndex).trim();
    }
}
