package com.zwc.sqldataprocessor.dbexecutor;

import com.zwc.sqldataprocessor.entity.DatabaseConfig;

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
}
