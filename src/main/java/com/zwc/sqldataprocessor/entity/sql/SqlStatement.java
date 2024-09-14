package com.zwc.sqldataprocessor.entity.sql;

/**
 * SQL语句
 */
public class SqlStatement implements Statement {
    /**
     * SQL执行的目标数据库名称
     */
    public String databaseName;

    /**
     * 执行的SQL
     */
    public String sql;

    /**
     * 数据集名称
     */
    public String resultName;
}