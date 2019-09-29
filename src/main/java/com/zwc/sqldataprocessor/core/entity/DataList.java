package com.zwc.sqldataprocessor.core.entity;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
