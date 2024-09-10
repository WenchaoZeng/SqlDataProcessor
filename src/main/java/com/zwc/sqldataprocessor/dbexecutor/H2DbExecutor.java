package com.zwc.sqldataprocessor.dbexecutor;

import com.zwc.sqldataprocessor.entity.DatabaseConfig;

public class H2DbExecutor extends DbExecutor {
    @Override
    public DatabaseConfig getDefaultConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.name = "h2";
        config.url = "jdbc:h2:mem:;DATABASE_TO_UPPER=FALSE";
        return config;
    }

    @Override
    public String getJdbcDriverName() {
        return "h2";
    }

    @Override
    public String getUrlSuffix() {
        return null;
    }
}
