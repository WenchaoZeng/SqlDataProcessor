package com.zwc.sqldataprocessor.entity;

public class Sql {
    public SqlType type;
    public String databaseName;
    public String sql;
    public String fileName;
    public String sheetName;
    public String resultName;
    public boolean exportNulls;
    public boolean useTempTables;
    public enum SqlType {
        SQL,
        IMPORT,
        EXPORT,
        END
    }
}
