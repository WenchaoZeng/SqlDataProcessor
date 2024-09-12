package com.zwc.sqldataprocessor.entity.sql;

/**
 * 导入语句
 */
public class ImportStatement implements Statement {

    /**
     * 导入文件的路径
     */
    public String filePath;

    /**
     * 一个excel表格里的一个sheet名称
     */
    public String sheetName;

    /**
     * 表头行的行号
     */
    public int headRowNo = 1;

    /**
     * 结果集名称
     */
    public String resultName;
}
