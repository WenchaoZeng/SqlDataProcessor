package com.zwc.sqldataprocessor.entity.sql;

/**
 * SQL语句
 */
public class SqlStatement implements Statement {

    /**
     * 临时表模式: 把数据集里的数据准备到临时表里去, 然后sql里再引用这个临时表
     */
    public boolean tempTables;

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