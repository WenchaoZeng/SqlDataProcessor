package com.zwc.sqldataprocessor.core.entity;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataList {

    public List<String> columns;
    public List<ColumnType> columnTypes;
    public static enum ColumnType {
        INT,
        DECIMAL,
        DATETIME,
        TEXT
    }
    public List<List<String>> rows = new ArrayList<>();

    public String getValue(int rowIndex, String columnName) {
        int columnIndex = columns.indexOf(columnName);
        if (columnIndex < 0) {
            throw new RuntimeException(String.format("列%s不存在", columnName));
        }
        return rows.get(rowIndex).get(columnIndex);
    }

    public ColumnType getColumnType(String columnName) {
        int columnIndex = columns.indexOf(columnName);
        return columnTypes.get(columnIndex);
    }

    public String renderSelectSql() {
        if (rows.size() <= 0) {
            return renderEmptySelectSql();
        }

        StringBuilder builder = new StringBuilder();
        for (int rowIndex = 0; rowIndex < rows.size(); ++rowIndex) {
            String selectSql = renderSelectSql(rowIndex);
            builder.append(selectSql);
            builder.append("\n");
            if (rowIndex < rows.size() - 1) {
                builder.append(" union all \n");
            }
        }

        return builder.toString();
    }

    String renderSelectSql(int rowIndex) {
        return renderSelectSql(rows.get(rowIndex));
    }

    String renderEmptySelectSql() {
        List<String> rowValues = new ArrayList<>();
        for (ColumnType type : columnTypes) {
            if (type == ColumnType.INT || type == ColumnType.DECIMAL) {
                rowValues.add("1");
            } else if (type == ColumnType.DATETIME) {
                rowValues.add("2019-02-15");
            } else {
                rowValues.add("");
            }
        }

        return renderSelectSql(rowValues) + " where false";
    }

    String renderSelectSql(List<String> rowValues) {
        List<String> selectColumns = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < columns.size(); ++columnIndex) {
            ColumnType type = columnTypes.get(columnIndex);
            String name = columns.get(columnIndex);
            String value = rowValues.get(columnIndex);
            String selectColumnFormat = null;
            if (type == ColumnType.INT || type == ColumnType.DECIMAL) {
                selectColumnFormat = "%s as `%s`";
            } else if (type == ColumnType.DATETIME) {
                selectColumnFormat = "cast('%s' as datetime) as `%s`";
            } else {
                if (value == null) {
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
