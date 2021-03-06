package com.zwc.sqldataprocessor.core.importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.entity.DataList.ColumnType;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
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
    public DataList doImport(byte[] content) {
        DataList table = new DataList();
        Workbook book = null;
        try {
            if (isXlsx) {
                book = new XSSFWorkbook(new ByteArrayInputStream(content));
            } else {
                book = WorkbookFactory.create(new NPOIFSFileSystem(new ByteArrayInputStream(content)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Sheet sheet = book.getSheetAt(0);
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); ++rowIndex) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                break;
            }
            List<String> values = new ArrayList<>();
            row.cellIterator().forEachRemaining(cell -> {
                if (cell == null) {
                    values.add("");
                    return;
                }

                String value = cell.toString();
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    if (value.contains("E")) {
                        double doubleValue = Double.parseDouble(value);
                        NumberFormat numberFormat = NumberFormat.getInstance();
                        numberFormat.setGroupingUsed(false);
                        value = numberFormat.format(doubleValue);
                    } else if (value.endsWith(".0")) {
                        value = value.substring(0, value.lastIndexOf("."));
                    }
                } else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                    DecimalFormat decimalFormat = new DecimalFormat("0");
                    try {
                        value = decimalFormat.format(cell.getNumericCellValue());
                    } catch (IllegalStateException e) {
                        value = String.valueOf(cell.getRichStringCellValue());
                    }
                }

                values.add(value);
            });

            if (table.columns == null) {
                // 去掉后面空的列名
                for (int index = values.size() - 1; index >= 0; --index) {
                    if (StringUtils.isBlank(values.get(index))) {
                        values.remove(index);
                    }
                }

                table.columns = values;
                table.columnTypes = values.stream().map(x -> ColumnType.TEXT).collect(Collectors.toList());
                continue;
            }

            // 自动补充数据列以满足列的个数
            int lackCount = table.columns.size() - values.size();
            for (int index = 0; index < lackCount; ++index) {
                values.add("");
            }

            List<String> values2 = values.subList(0, table.columns.size());
            table.rows.add(values2);
        }


        return table;
    }

}
