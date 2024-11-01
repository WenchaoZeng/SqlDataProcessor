package com.zwc.sqldataprocessor.entity.sql;

/**
 * 导出语句
 */
public class ExportStatement implements Statement {

    /**
     * 把null值导出显示成: <null>
     */
    public boolean exportNulls;

    /**
     * 导出文件格式为: xlsx
     */
    public boolean exportXlsx;

    /**
     * 导出xlsx时的sheet的名称
     */
    public String sheetName;

    /**
     * 文件名称或文件绝对路径
     */
    public String filePath;
}
