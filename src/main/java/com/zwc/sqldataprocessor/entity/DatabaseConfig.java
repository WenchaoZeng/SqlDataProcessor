package com.zwc.sqldataprocessor.entity;

import java.io.Serializable;

import com.zwc.sqldataprocessor.dbexecutor.DbExecutor;

public class DatabaseConfig implements Serializable {
    public String name;
    public String url;
    public String userName;
    public String password;

    /**
     * 使用临时表的方式来引用数据集
     * 如果配置为否(默认), 则使用子查询的方式来引用数据集
     */
    public boolean useTempTables;

    /**
     * 使用真实表的方式来引用数据集
     * 如果配置为否(默认), 则使用子查询的方式来引用数据集
     */
    public boolean useRealTables;

    /**
     * 插入临时表时的批量大小
     */
    public int uploadBatchSize = 1000;

    public DbExecutor dbExecutor;
}
