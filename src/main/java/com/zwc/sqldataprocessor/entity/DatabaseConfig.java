package com.zwc.sqldataprocessor.entity;

import java.io.Serializable;

import com.zwc.sqldataprocessor.dbexecutor.DbExecutor;

public class DatabaseConfig implements Serializable {
    public String name;
    public String url;
    public String userName;
    public String password;

    public DbExecutor dbExecutor;
}
