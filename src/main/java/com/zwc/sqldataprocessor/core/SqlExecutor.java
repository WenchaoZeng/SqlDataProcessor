package com.zwc.sqldataprocessor.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.core.entity.DatabaseConfig;
import org.apache.commons.lang3.StringUtils;

public class SqlExecutor {

    public static DataList exec(String sql, String databaseName, Map<String, DataList> tables) {
        String rawSql = renderSql(sql, tables);
        return execRawSql(rawSql, databaseName);
    }

    static DataList execRawSql(String sql, String databaseName) {
        try {

            Connection conn = DatabaseConfigLoader.getConn(databaseName);

            // 解除group_concat的长度限制
            if (DatabaseConfigLoader.isMySql(databaseName)) {
                try (Statement cmd = conn.createStatement()) {
                    cmd.execute("SET SESSION group_concat_max_len = 1000000;");
                }
            }

            try (PreparedStatement cmd = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                DataList table = new DataList();
                table.columns = new ArrayList<>();
                table.columnTypes = new ArrayList<>();

                if (!sql.startsWith("select") && !sql.startsWith("SELECT")) {
                    cmd.execute();
                    return table;
                }

                cmd.setFetchSize(Integer.MAX_VALUE);
                try (ResultSet resultSet = cmd.executeQuery()) {

                    // 列头
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int count = metaData.getColumnCount();
                    for (int i = 1; i <= count; ++i) {
                        String name = metaData.getColumnLabel(i);
                        String type = metaData.getColumnTypeName(i);
                        table.columns.add(name);
                        if (type.equals("INT")) {
                            table.columnTypes.add(ColumnType.INT);
                        } else if (type.equals("DECIMAL")) {
                            table.columnTypes.add(ColumnType.DECIMAL);
                        } else if (type.equals("DATETIME")) {
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
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static String renderSql(String sql, Map<String, DataList> tables) {

        for (String tableName : tables.keySet()) {
            if (sql.contains("$" + tableName)) {
                DataList table = tables.get(tableName);
                if (table == null) {
                    throw new RuntimeException("结果集$" + tableName + "不存在.");
                }
                StringBuilder builder = new StringBuilder();
                builder.append("(\n");
                String tableSelectSql = renderSelectSql(table);
                builder.append(tableSelectSql);
                builder.append(")");
                sql = sql.replace("$" + tableName, builder.toString());
            }
        }

        return sql;
    }

    static String renderSelectSql(DataList table) {
        if (table.rows.size() <= 0) {
            return renderEmptySelectSql(table);
        }

        StringBuilder builder = new StringBuilder();
        for (int rowIndex = 0; rowIndex < table.rows.size(); ++rowIndex) {
            String selectSql = renderSelectSql(rowIndex, table);
            builder.append(selectSql);
            builder.append("\n");
            if (rowIndex < table.rows.size() - 1) {
                builder.append(" union all \n");
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

        return renderSelectSql(rowValues, table) + " where false";
    }

    static String renderSelectSql(int rowIndex, DataList table) {
        return renderSelectSql(table.rows.get(rowIndex), table);
    }

    static String renderSelectSql(List<String> rowValues, DataList table) {
        List<String> selectColumns = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < table.columns.size(); ++columnIndex) {
            ColumnType type = table.columnTypes.get(columnIndex);
            String name = table.columns.get(columnIndex);
            String value = rowValues.get(columnIndex);
            String selectColumnFormat = null;
            if (type == ColumnType.INT || type == ColumnType.DECIMAL) {
                selectColumnFormat = "%s as `%s`";
                if (StringUtils.isBlank(value)) {
                    value = null;
                }
            } else if (type == ColumnType.DATETIME) {
                if (StringUtils.isBlank(value)) {
                    value = null;
                    selectColumnFormat = "cast(%s as datetime) as `%s`";
                } else {
                    selectColumnFormat = "cast('%s' as datetime) as `%s`";
                }
            } else {
                if (StringUtils.isBlank(value)) {
                    value = "";
                }
                value = value.replace("'", "\\'");
                selectColumnFormat = "'%s' as `%s`";
            }
            selectColumns.add(String.format(selectColumnFormat, value, name));
        }
        return "select " + String.join(", ", selectColumns);
    }

}
