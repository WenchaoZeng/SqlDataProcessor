package com.zwc.sqldataprocessor.exporter;

import com.zwc.sqldataprocessor.entity.DataList;

public interface Exporter {
    /**
     * 执行导出
     */
    void export(String filePath, DataList table, String sheetName, boolean exportNulls);

    /**
     * 获取文件名的后缀
     */
    String getExtension();

    /**
     * 将结果写入文件, 并关闭文件流
     */
    void flushAllFiles();
}