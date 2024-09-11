package com.zwc.sqldataprocessor.dbexecutor;

import java.util.ArrayList;
import java.util.List;

import com.zwc.sqldataprocessor.DatabaseConfigLoader;
import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.entity.UserException;

public abstract class DbExecutor {
    public final static List<DbExecutor> dbExecutors = new ArrayList<>();
    static {
        dbExecutors.add(new MySqlDbExecutor());
        dbExecutors.add(new H2DbExecutor());
        dbExecutors.add(new PostgreSqlDbExecutor());
    }

    public static DbExecutor getDbExecutor(String url) {
        DbExecutor dbExecutor = dbExecutors.stream().filter(x -> url.toLowerCase().startsWith("jdbc:" + x.getJdbcDriverName())).findFirst().orElse(null);
        if (dbExecutor == null) {
            throw new UserException("不支持的数据库url, 请检查数据库的url地址是否填写有误");
        }

        return dbExecutor;
    }

    public abstract DatabaseConfig getDefaultConfig();
    public abstract String getJdbcDriverName();
    public abstract String getUrlSuffix();
    public abstract String getSqlAfterConnect();
    public abstract void translateSqlException(Exception ex);
    public abstract String renderDropTempTableSql(String tableName);
    public abstract String renderCreateTempTableSql(DataList table, String tableName);
    public abstract void renderSelectSql(StringBuilder builder, DataList table);

    public static StringBuilder commonCreateTempTableSql(DataList table, String tableName) {
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
        return builder;
    }
}