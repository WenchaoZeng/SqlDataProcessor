package com.zwc.sqldataprocessor.core;

import java.util.ArrayList;

import java.util.List;
import com.zwc.sqldataprocessor.Global;
import com.zwc.sqldataprocessor.core.entity.Sql;

public class SqlLoader {
    public static List<Sql> loadSql(String filePath) {
        String fileContent = Global.readFile(filePath);
        List<Sql> sqlList = new ArrayList<>();
        Sql sql = null;
        for (String line : fileContent.split("\n")) {
            line = line.trim();

            // 读取sql语句
            if (sql != null) {
                if (line.endsWith(";")) {
                    sql.sql += line;
                    sqlList.add(sql);
                    sql = null;
                } else {
                    sql.sql += line + "\n";
                }
                continue;
            }

            if (line.startsWith("# ")) {

                // 导入
                if (line.startsWith("# import ")) {
                    Sql importSql = new Sql();
                    importSql.fileName = line.replace("# import ", "");
                    importSql.fileName = removeResultNameClause(importSql.fileName);
                    importSql.resultName = getResultName(line);
                    sqlList.add(importSql);
                    continue;
                }

                // db名称
                String databaseName = line.replace("# ", "");
                databaseName = removeResultNameClause(databaseName);
                sql = new Sql();
                sql.databaseName = databaseName;
                sql.sql = "";
                sql.resultName = getResultName(line);
            }
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
