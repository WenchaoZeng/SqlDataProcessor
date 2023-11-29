package com.zwc.sqldataprocessor.exporter;

import com.zwc.sqldataprocessor.entity.DataList;

public interface Exporter {
    byte[] export(DataList table, boolean exportNulls);
    String getExtension();
}
