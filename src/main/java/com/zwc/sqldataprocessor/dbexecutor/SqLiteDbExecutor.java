package com.zwc.sqldataprocessor.dbexecutor;

import java.util.ArrayList;
import java.util.List;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.entity.UserException;
import org.apache.commons.lang3.StringUtils;

public class SqLiteDbExecutor extends DbExecutor {
    @Override
    public DatabaseConfig getDefaultConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.name = "sqlite";
        config.url = "jdbc:sqlite::memory:";
        config.useTempTables = true;
        return config;
    }

    @Override
    public String getJdbcDriverName() {
        return "sqlite";
    }

    @Override
    public String getUrlSuffix() {
        return null;
    }

    @Override
    public String getSqlAfterConnect() {
        return null;
    }

    @Override
    public void translateSqlException(Exception ex) {
        if (ex instanceof org.sqlite.SQLiteException) {
            throw new UserException(ex.getMessage());
        }
    }

    @Override
    public String renderDropTableSql(String tableName, boolean isTemporary) {
        return String.format("drop table if exists %s;", tableName);
    }

    @Override
    public String renderCreateTableSql(DataList table, String tableName, boolean isTemporary) {
        StringBuilder builder = new StringBuilder();
        builder.append("create");
        if (isTemporary) {
            builder.append(" temporary");
        }
        builder.append(" table " + tableName + "(");
        for (int index = 0; index < table.columns.size(); ++index) {
            builder.append("\"" + table.columns.get(index) + "\" ");

            ColumnType columnType = table.columnTypes.get(index);
            if (columnType == ColumnType.INT) {
                    builder.append("bigint");
            } else if (columnType == ColumnType.DECIMAL) {
                builder.append("decimal");
            } else {
                builder.append("text");
            }

            if (index != table.columns.size() - 1) {
                builder.append(",");
            }
        }
        builder.append(") ");
        return builder.toString();
    }

    @Override
    public void renderSelectSql(StringBuilder builder, DataList table) {
        for (int rowIndex = 0; rowIndex < table.rows.size(); ++rowIndex) {
            boolean includeColumnName = rowIndex == 0;
            String selectSql = renderSelectClause(table.rows.get(rowIndex), table, includeColumnName);
            builder.append(selectSql);
            builder.append("\n");
            if (rowIndex < table.rows.size() - 1) {
                builder.append("union all\n");
            }
        }
    }

    @Override
    public void renderInsertSql(StringBuilder builder, DataList table, String targetTableName) {
        builder.append("insert into " + targetTableName + " ");
        renderSelectSql(builder, table);
    }

    String renderSelectClause(List<String> rowValues, DataList table, boolean includeColumnName) {
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
            } else {
                if (value == null) {
                    sqlValue = "null";
                } else {
                    value = value.replace("'", "''");
                    sqlValue = "'" + value + "'";
                }
            }

            //  包含列名
            if (includeColumnName) {
                sqlValue += " as \"" + name + "\"";
            }

            selectColumns.add(sqlValue);
        }
        return "select " + String.join(",", selectColumns);
    }
}
