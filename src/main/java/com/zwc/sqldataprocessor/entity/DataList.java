package com.zwc.sqldataprocessor.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.formula.functions.T;

public class DataList {

    public List<String> columns;
    public List<ColumnType> columnTypes;
    public enum ColumnType {
        INT,
        DECIMAL,
        DATETIME,
        TEXT
    }
    public List<List<String>> rows = new ArrayList<>();

    public List<DataList> split(int maxRowCount) {
        if (rows.size() <= maxRowCount) {
            return Collections.singletonList(this);
        }

        List<List<List<String>>> partitions = ListUtils.partition(rows, maxRowCount);
        List<DataList> resultList = new ArrayList<>();
        for (List<List<String>> partition : partitions) {
            DataList dataList = new DataList();
            dataList.columns = columns;
            dataList.columnTypes = columnTypes;
            dataList.rows = partition;
            resultList.add(dataList);
        }
        return resultList;
    }
}
