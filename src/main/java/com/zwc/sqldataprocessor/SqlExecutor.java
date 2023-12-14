package com.zwc.sqldataprocessor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.UserException;
import com.zwc.sqldataprocessor.entity.sql.SqlStatement;
import org.apache.commons.lang3.StringUtils;

public class SqlExecutor {

    public static DataList exec(SqlStatement statement, Map<String, DataList> tables) {
        String rawSql = renderSql(statement, tables);

        // 解除group_concat的长度限制
        if (DatabaseConfigLoader.isMySql(statement.databaseName)) {
            execRawSql("SET SESSION group_concat_max_len = 1000000;", statement.databaseName);
        }

        return execRawSql(rawSql, statement.databaseName);
    }

    public static DataList execRawSql(String sql, String databaseName) {

        FileHelper.writeOutputFile("./current.sql", sql);

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
            // h2数据库会把完整的sql打印出来, 会导致错误里输出的内容太多了, 所以只需要保留错误的描述就行.
            if (ex instanceof org.h2.jdbc.JdbcSQLSyntaxErrorException) {
                String msg = ex.getMessage();
                int sqlStatementIndex = msg.indexOf("SQL statement:");
                if (sqlStatementIndex > 0) {
                    msg = msg.substring(0, sqlStatementIndex);
                }
                throw new UserException(msg);
            }

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

    static String renderSql(SqlStatement statement, Map<String, DataList> tables) {
        Set<String> createdTempTables = new HashSet<>();
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
                if (statement.tempTables) { // 构建临时表
                    String tempTableName = "_temp_" + tableName.replace("$", "");

                    if (!createdTempTables.contains(tempTableName)) {
                        String sqlFormat = "drop temporary table if exists %s;";
                        if (DatabaseConfigLoader.isH2(statement.databaseName)) {
                            sqlFormat = "drop table if exists %s;";
                        }
                        execRawSql(String.format(sqlFormat, tempTableName), statement.databaseName);
                        String createTempTableSql = renderCreateTempTableSql(table, tempTableName, statement.databaseName);
                        execRawSql(createTempTableSql, statement.databaseName);

                        // 分批导入数据
                        List<DataList> dataLists = table.split(1000);
                        for (DataList dataList : dataLists) {
                            String dataInsertSql = "insert into " + tempTableName + " ";
                            dataInsertSql += renderSelectSql(dataList, statement.databaseName);
                            execRawSql(dataInsertSql, statement.databaseName);
                        }

                        createdTempTables.add(tempTableName);
                    }

                    tableReplacement = tempTableName + " ";
                } else { // 构建数据集sql语句
                    StringBuilder builder = new StringBuilder();
                    builder.append("(\n");
                    String tableSelectSql = renderSelectSql(table, statement.databaseName);
                    builder.append(tableSelectSql);
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

    static String renderCreateTempTableSql(DataList table, String tableName, String databaseName) {
        StringBuilder builder = new StringBuilder();
        builder.append("create temporary table " + tableName + "(");
        for (int index = 0; index < table.columns.size(); ++index) {
            builder.append("`" + table.columns.get(index) + "` ");

            ColumnType columnType = table.columnTypes.get(index);
            if (columnType == ColumnType.INT) {
                    builder.append("bigint");
            } else if (columnType == ColumnType.DECIMAL) {
                builder.append("decimal");
            } else if (columnType == ColumnType.DATETIME) {
                builder.append("datetime");
            } else {
                builder.append("LONGTEXT");
            }

            if (index != table.columns.size() - 1) {
                builder.append(",");
            }
        }
        builder.append(") ");
        if (DatabaseConfigLoader.isMySql(databaseName)) {
            builder.append("collate utf8mb4_general_ci CHARACTER SET utf8mb4");
        }
        return builder.toString();
    }

    static String renderSelectSql(DataList table, String databaseName) {
        if (DatabaseConfigLoader.isH2(databaseName)) {
            return renderSelectSqlForH2(table);
        } else {
            return renderSelectSql(table);
        }
    }

    static String renderSelectSql(DataList table) {
        if (table.rows.size() <= 0) {
            return renderEmptySelectSql(table);
        }

        StringBuilder builder = new StringBuilder();
        for (int rowIndex = 0; rowIndex < table.rows.size(); ++rowIndex) {
            boolean includeColumnName = rowIndex == 0;
            String selectSql = renderSelectSql(table.rows.get(rowIndex), table, includeColumnName);
            builder.append(selectSql);
            builder.append("\n");
            if (rowIndex < table.rows.size() - 1) {
                builder.append("union all\n");
            }
        }

        return builder.toString();
    }

    static String renderEmptySelectSql(DataList table) {
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

        return renderSelectSql(rowValues, table, true) + " from (select 1) _sqldataprocessor_ where false";
    }

    static String renderSelectSql(List<String> rowValues, DataList table, boolean includeColumnName) {
        List<String> selectColumns = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < table.columns.size(); ++columnIndex) {
            ColumnType type = table.columnTypes.get(columnIndex);
            String name = table.columns.get(columnIndex);
            String value = rowValues.get(columnIndex);
            String sqlValue = null;
            if (type == ColumnType.INT || type == ColumnType.DECIMAL) {
                if (StringUtils.isBlank(value)) {
                    sqlValue = "null";
                } else {
                    sqlValue = value;
                }
            } else if (type == ColumnType.DATETIME) {
                if (StringUtils.isBlank(value)) {
                    sqlValue = "cast(null as datetime)";
                } else {
                    sqlValue = "cast('" + value + "' as datetime)";
                }
            } else {
                if (value == null) {
                    sqlValue = "null";
                } else {
                    value = value.replace("\\", "\\\\");
                    value = value.replace("'", "''");
                    sqlValue = "'" + value + "'";
                }
            }

            //  包含列名
            if (includeColumnName) {
                sqlValue += " as `" + name + "`";
            }

            selectColumns.add(sqlValue);
        }
        return "select " + String.join(",", selectColumns);
    }

    static String renderSelectSqlForH2(DataList table) {
        if (table.rows.size() <= 0) {
            return renderEmptySelectSql(table);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("select\n");
        List<String> selectColumns = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < table.columns.size(); ++columnIndex) {
            String name = table.columns.get(columnIndex);
            ColumnType type = table.columnTypes.get(columnIndex);
            String selectColumnFormat = null;
            if (type == ColumnType.DATETIME) {
                selectColumnFormat = "cast(C" + (columnIndex +1) + " as datetime) as `%s`";
            } else {
                selectColumnFormat = "C" + (columnIndex +1) + " as `%s`";
            }
            selectColumns.add(String.format(selectColumnFormat, name));
        }
        builder.append(String.join(", ", selectColumns));
        builder.append("\n");
        builder.append("from VALUES\n");

        for (int rowIndex = 0; rowIndex < table.rows.size(); ++rowIndex) {
            String valueClause = renderValueClause(table.rows.get(rowIndex), table);
            builder.append(valueClause);
            if (rowIndex < table.rows.size() - 1) {
                builder.append(", \n");
            }
        }

        return builder.toString();
    }

    static String renderValueClause(List<String> rowValues, DataList table) {
        List<String> values = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < table.columns.size(); ++columnIndex) {
            ColumnType type = table.columnTypes.get(columnIndex);
            String value = rowValues.get(columnIndex);
            if (type == ColumnType.INT || type == ColumnType.DECIMAL) {
                if (StringUtils.isBlank(value)) {
                    values.add(null);
                } else {
                    values.add(value);
                }
            } else if (type == ColumnType.DATETIME) {
                if (StringUtils.isBlank(value)) {
                    values.add(null);
                } else {
                    values.add("'" + value + "'");
                }
            } else {
                if (value == null) {
                    values.add(null);
                } else {
                    value = value.replace("'", "''");
                    values.add("'" + value + "'");
                }
            }
        }
        return "(" + String.join(", ", values) + ")";
    }
}