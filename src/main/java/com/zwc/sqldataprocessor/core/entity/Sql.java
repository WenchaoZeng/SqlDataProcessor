package com.zwc.sqldataprocessor.core.entity;

public class Sql {
    public SqlType type;
    public String databaseName;
    public String sql;
    public String fileName;
    public String resultName;
    public boolean exportNulls;
    public enum SqlType {
        SQL,
        IMPORT,
        EXPORT,
        END
    }
}
