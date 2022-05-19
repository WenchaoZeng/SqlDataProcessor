package com.zwc.sqldataprocessor.entity;

import java.util.ArrayList;
import java.util.List;

public class DataList {

    public List<String> columns;
    public List<ColumnType> columnTypes;
    public static enum ColumnType {
        INT,
        DECIMAL,
        DATETIME,
        TEXT
    }
    public List<List<String>> rows = new ArrayList<>();
}
