package com.zwc.sqldataprocessor.importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.UserException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XlsImporter implements Importer {

    boolean isXlsx;

    public XlsImporter(boolean isXlsx) {
        this.isXlsx = isXlsx;
    }

    @Override
    public DataList doImport(byte[] content, String sheetName, int headRowNo) {
        DataList table = new DataList();
        Workbook book = null;
        try {
            if (isXlsx) {
                book = new XSSFWorkbook(new ByteArrayInputStream(content));
            } else {
                book = WorkbookFactory.create(new POIFSFileSystem(new ByteArrayInputStream(content)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Sheet sheet;
        if (StringUtils.isBlank(sheetName)) {
            sheet = book.getSheetAt(0);
        } else {
            sheet = book.getSheet(sheetName);
            if (sheet == null) {
                throw new UserException("excel文件里不存在该sheet: " + sheetName);
            }
        }

        if (headRowNo > sheet.getLastRowNum() + 1) {
            throw new UserException("表头行号超出了sheet里的最大行数");
        }

        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); ++rowIndex) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                break;
            }

            // 跳过给定表头行号之前的行
            if (rowIndex + 1 < headRowNo) {
                continue;
            }

            // 行或列数据初始化
            List<String> values = null;
            if (table.columns == null) {
                values = new ArrayList<>(Arrays.asList(new String[row.getLastCellNum()]));
                table.columns = values;
                table.columnTypes = table.columns.stream().map(x -> ColumnType.TEXT).collect(Collectors.toList());
            } else {
                values = new ArrayList<>(Arrays.asList(new String[table.columns.size()]));
                table.rows.add(values);
            }

            // 读取单元格数据
            for (int columnIndex = 0; columnIndex < values.size(); ++columnIndex) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    values.set(columnIndex, "");
                    continue;
                }

                String value = cell.toString();
                if (cell.getCellType() == CellType.NUMERIC) {
                    if (value.contains("E")) {
                        double doubleValue = Double.parseDouble(value);
                        NumberFormat numberFormat = NumberFormat.getInstance();
                        numberFormat.setGroupingUsed(false);
                        value = numberFormat.format(doubleValue);
                    } else if (value.endsWith(".0")) {
                        value = value.substring(0, value.lastIndexOf("."));
                    } else if (DateUtil.isCellDateFormatted(cell)) {
                        value = cn.hutool.core.date.DateUtil.formatDateTime(cell.getDateCellValue());
                    }
                } else if (cell.getCellType() == CellType.FORMULA) {
                    DecimalFormat decimalFormat = new DecimalFormat("0");
                    try {
                        value = decimalFormat.format(cell.getNumericCellValue());
                    } catch (IllegalStateException e) {
                        value = String.valueOf(cell.getRichStringCellValue());
                    }
                }

                values.set(columnIndex, value);
            }
        }

        // 剔除空列头
        for (int columnIndex = table.columns.size() - 1; columnIndex >= 0; --columnIndex) {
            if (!StringUtils.isBlank(table.columns.get(columnIndex))) {
                continue;
            }

            table.columns.remove(columnIndex);
            int columnIndex2 = columnIndex;
            table.rows.forEach(row -> {
                row.remove(columnIndex2);
            });
        }

        // 清理后面的空行
        for (int rowIndex = table.rows.size() - 1; rowIndex >= 0; --rowIndex) {
            List<String> row = table.rows.get(rowIndex);

            // 检查该行是否为空行
            boolean isEmptyRow = true;
            for (int columnIndex = 0; columnIndex < row.size(); ++columnIndex) {
                if (!StringUtils.isBlank(row.get(columnIndex))) {
                    isEmptyRow = false;
                    break;
                }
            }

            // 不再继续检查处理中间的空行了, 不然行号不对.
            if (!isEmptyRow) {
                break;
            }

            table.rows.remove(rowIndex);
        }

        return table;
    }

}
