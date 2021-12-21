package com.zwc.sqldataprocessor.core;

import java.util.ArrayList;

import java.util.List;
import com.zwc.sqldataprocessor.Global;
import com.zwc.sqldataprocessor.core.entity.Sql;
import com.zwc.sqldataprocessor.core.entity.Sql.SqlType;

public class SqlLoader {
    public static List<Sql> loadSql(String filePath) {
        String fileContent = Global.readFile(filePath);
        List<Sql> sqlList = new ArrayList<>();
        for (String line : fileContent.split("\n")) {

            if (line.startsWith("# ")) {

                // 导入
                if (line.startsWith("# import ")) {
                    Sql importSql = new Sql();
                    importSql.type = SqlType.IMPORT;
                    importSql.fileName = line.replace("# import ", "");
                    importSql.fileName = removeResultNameClause(importSql.fileName);
                    importSql.resultName = getResultName(line);
                    sqlList.add(importSql);
                    continue;
                }

                // 导出
                if (line.startsWith("# export")) {
                    Sql exportSql = new Sql();
                    exportSql.type = SqlType.EXPORT;
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
                Sql sql = new Sql();
                sql.type = SqlType.SQL;
                sql.databaseName = databaseName;
                sql.sql = "";
                sql.resultName = getResultName(line);
                sqlList.add(sql);
                continue;
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
}
