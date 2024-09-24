package com.zwc.sqldataprocessor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.zwc.sqldataprocessor.dbexecutor.DbExecutor;
import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.entity.UserException;
import com.zwc.sqldataprocessor.entity.sql.SqlStatement;

public class SqlExecutor {

    public static DataList exec(SqlStatement statement, Map<String, DataList> tables) {
        DatabaseConfig dbConfig = DatabaseConfigLoader.getDbConfig(statement.databaseName);
        Set<String> tempTableNames = new HashSet<>();
        try {
            String rawSql = renderSql(dbConfig, statement, tables, tempTableNames);
            return execRawSql(rawSql, statement.databaseName);
        } finally {
            // 删除临时表
            for (String tempTableName : tempTableNames) {
                String dropTempTableSql = dbConfig.dbExecutor.renderDropTableSql(tempTableName, dbConfig.useTempTables);
                execRawSql(dropTempTableSql, statement.databaseName);
            }
        }
    }

    public static DataList execRawSql(String sql, String databaseName) {
        FileHelper.writeOutputFile("./current.sql", sql);
        DatabaseConfig dbConfig = DatabaseConfigLoader.getDbConfig(databaseName);
        try {

            Connection conn = DatabaseConfigLoader.getConn(databaseName);

            try (Statement cmd = conn.createStatement()) {
                boolean hasResult = cmd.execute(sql);

                // 读取结果
                cmd.setFetchSize(Integer.MAX_VALUE);
                DataList table = newEmptyList();
                for (;;hasResult = cmd.getMoreResults()) {

                    // 查询语句
                    if (hasResult) {
                        try (ResultSet resultSet = cmd.getResultSet()) {
                            table = readResult(resultSet);
                            continue;
                        }
                    }

                    // 非查询语句
                    int updateCount = cmd.getUpdateCount();
                    if (updateCount == -1) { // 代表已经没有结果了
                        break;
                    }
                };

                return table;
            }
        } catch (Exception ex) {
            dbConfig.dbExecutor.translateSqlException(ex);

            // SQL语法错误
            if (ex instanceof SQLSyntaxErrorException) {
                throw new UserException(ex.getMessage());
            }

            throw new RuntimeException(ex);
        }
    }

    static DataList newEmptyList() {
        DataList table = new DataList();
        table.columns = new ArrayList<>();
        table.columnTypes = new ArrayList<>();
        return table;
    }

    static DataList readResult(ResultSet resultSet) throws SQLException {
        DataList table = newEmptyList();

        // 列头
        ResultSetMetaData metaData = resultSet.getMetaData();
        int count = metaData.getColumnCount();
        for (int i = 1; i <= count; ++i) {
            String name = metaData.getColumnLabel(i);
            String type = metaData.getColumnTypeName(i);
            table.columns.add(name);
            if (type.equals("INT") || type.equals("BIGINT")|| type.equals("TINYINT")) {
                table.columnTypes.add(ColumnType.INT);
            } else if (type.equals("DECIMAL")) {
                table.columnTypes.add(ColumnType.DECIMAL);
            } else if (type.equals("DATETIME") || type.equals("TIMESTAMP") || type.equals("DATE")) {
                table.columnTypes.add(ColumnType.DATETIME);
            } else {
                table.columnTypes.add(ColumnType.TEXT);
            }
        }

        // 行数据
        while (resultSet.next()) {
            List<String> values = new ArrayList<>();
            for (int i = 1; i <= table.columns.size(); ++ i) {
                String value = resultSet.getString(i);
                values.add(value);
            }
            table.rows.add(values);
        }

        return table;
    }

    static String renderSql(DatabaseConfig dbConfig, SqlStatement statement, Map<String, DataList> tables, Set<String> tempTableNames) {

        StringBuilder sqlBuilder = new StringBuilder();
        for (String sqlLine : statement.sql.split("\n")) {

            // 对于注释行不需要解析数据集
            String sqlLineTrim = sqlLine.trim();
            if (sqlLineTrim.startsWith("-- ") || sqlLineTrim.startsWith("#")) {
                sqlBuilder.append(sqlLine);
                sqlBuilder.append("\n");
                continue;
            }

            // 查找数据集关键字
            List<String> tableNames = new ArrayList<>();
            for (String tableName : tables.keySet()) {
                if (sqlLine.contains("$" + tableName + " ")) {
                    tableNames.add(tableName);
                }
            }

            // 读取数据集
            for (String tableName : tableNames) {
                DataList table = tables.get(tableName);

                String tableReplacement = "";
                if (dbConfig.useTempTables || dbConfig.useRealTables) { // 构建临时表
                    String tempTableName = "_sql_" + System.currentTimeMillis() + "_" + tableName.replace("$", "");

                    if (!tempTableNames.contains(tempTableName)) {
                        tempTableNames.add(tempTableName);

                        // 创建临时表
                        String createTempTableSql = dbConfig.dbExecutor.renderCreateTableSql(table, tempTableName, dbConfig.useTempTables);
                        execRawSql(createTempTableSql, statement.databaseName);

                        // 分批导入数据
                        List<DataList> dataLists = table.split(dbConfig.uploadBatchSize);
                        for (DataList dataList : dataLists) {
                            StringBuilder builder = new StringBuilder();
                            builder.append("insert into " + tempTableName + " ");
                            renderSelectSql(builder, dataList, dbConfig.dbExecutor);
                            execRawSql(builder.toString(), statement.databaseName);
                        }
                    }

                    tableReplacement = tempTableName + " ";
                } else { // 构建数据集sql语句
                    StringBuilder builder = new StringBuilder();
                    builder.append("(\n");
                    renderSelectSql(builder, table, dbConfig.dbExecutor);
                    builder.append(") ");
                    tableReplacement = builder.toString();
                }

                // 替换到原sql里
                sqlLine = sqlLine.replace("$" + tableName + " ", tableReplacement);
            }

            sqlBuilder.append(sqlLine);
            sqlBuilder.append("\n");
        }

        return sqlBuilder.toString();
    }

    static void renderSelectSql(StringBuilder builder, DataList table, DbExecutor dbExecutor) {
        if (table.rows.isEmpty()) {
            renderEmptySelectSql(builder, table, dbExecutor);
            return;
        }

        dbExecutor.renderSelectSql(builder, table);
    }

    static void renderEmptySelectSql(StringBuilder builder, DataList table, DbExecutor dbExecutor) {
        List<String> rowValues = new ArrayList<>();
        for (ColumnType type : table.columnTypes) {
            if (type == ColumnType.INT || type == ColumnType.DECIMAL) {
                rowValues.add("1");
            } else if (type == ColumnType.DATETIME) {
                rowValues.add("2019-02-15");
            } else {
                rowValues.add("");
            }
        }

        builder.append("select * from ( ");

        table.rows.add(rowValues);
        dbExecutor.renderSelectSql(builder, table);
        table.rows.clear();

        builder.append(" ) _sqldataprocessor_empty_ where 1 = 0");
    }
}