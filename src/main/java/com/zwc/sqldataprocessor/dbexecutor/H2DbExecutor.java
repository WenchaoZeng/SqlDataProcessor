package com.zwc.sqldataprocessor.dbexecutor;

import java.util.ArrayList;
import java.util.List;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.entity.UserException;

public class H2DbExecutor extends DbExecutor {
    @Override
    public DatabaseConfig getDefaultConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.name = "h2";
        config.url = "jdbc:h2:mem:;DATABASE_TO_UPPER=FALSE";
        config.useTempTables = true;
        return config;
    }

    @Override
    public String getJdbcDriverName() {
        return "h2";
    }

    @Override
    public void translateSqlException(Exception ex) {
        // h2数据库会把完整的sql打印出来, 会导致错误里输出的内容太多了, 所以只需要保留错误的描述就行.
        if (ex instanceof org.h2.jdbc.JdbcSQLSyntaxErrorException) {
            String msg = ex.getMessage();
            int sqlStatementIndex = msg.indexOf("SQL statement:");
            if (sqlStatementIndex > 0) {
                msg = msg.substring(0, sqlStatementIndex);
            }
            throw new UserException(msg);
        }
    }

    @Override
    public String renderDropTableSql(String tableName, boolean isTemporary) {
        return String.format("drop table if exists %s;", tableName);
    }

    @Override
    public String renderCreateTableSql(DataList table, String tableName, boolean isTemporary) {
        return commonCreateTableSql(table, tableName, isTemporary).toString();
    }

    @Override
    public void renderSelectSql(StringBuilder builder, DataList table) {
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
        builder.append("from");
        renderCommonValuesClause(builder, table);
    }

    @Override
    public void renderInsertSql(StringBuilder builder, DataList table, String targetTableName) {
        builder.append("insert into " + targetTableName + " ");
        renderCommonValuesClause(builder, table);
    }
}
