package com.zwc.sqldataprocessor.dbexecutor;

import java.util.ArrayList;
import java.util.List;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import org.apache.commons.lang3.StringUtils;

public class MySqlDbExecutor extends DbExecutor {
    @Override
    public DatabaseConfig getDefaultConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.name = "mysql";
        config.url = "jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterset=utf-8";
        config.userName = "root";
        config.password = "123456";
        return config;
    }

    @Override
    public String getJdbcDriverName() {
        return "mysql";
    }

    @Override
    public String getUrlSuffix() {
        return "allowMultiQueries=true";
    }

    @Override
    public String getSqlAfterConnect() {
        return "SET SESSION group_concat_max_len = 4294967295;";
    }

    @Override
    public String renderDropTableSql(String tableName, boolean isTemporary) {
        return String.format("drop %s table if exists %s;", isTemporary ? "temporary" : "", tableName);
    }

    @Override
    public String renderCreateTableSql(DataList table, String tableName, boolean isTemporary) {
        StringBuilder builder = commonCreateTableSql(table, tableName, isTemporary);
        builder.append(" collate utf8mb4_general_ci CHARACTER SET utf8mb4");
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

    @Override
    public boolean supportReopenTempTables() {
        return false;
    }

    @Override
    public String renderCloneTempTables(String sourceTableName, String targetTableName) {
        return String.format("create temporary table %s as select * from %s;", targetTableName, sourceTableName);
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
}
