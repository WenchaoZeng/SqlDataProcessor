package com.zwc.sqldataprocessor.exporter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import com.zwc.sqldataprocessor.entity.DataList;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;

public class XlsxExporter implements Exporter {

    @Override
    public byte[] export(DataList table, boolean exportNulls) {
        LinkedHashMap<String, DataList> tables = new LinkedHashMap<>();
        tables.put("sheet1", table);
        return export(tables);
    }

    @Override
    public String getExtension() {
        return "xlsx";
    }

    public byte[] export(LinkedHashMap<String, DataList> tables) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            for (String sheetName : tables.keySet()) {
                DataList table = tables.get(sheetName);
                if (table.columns == null) {
                    continue;
                }
                SXSSFSheet sheet = workbook.createSheet(sheetName);

                // 写入表头
                XSSFCellStyle headStyle = (XSSFCellStyle) workbook.createCellStyle();
                headStyle.setAlignment(HorizontalAlignment.CENTER);
                XSSFFont headFont = (XSSFFont )workbook.createFont();
                headFont.setBold(true);
                headStyle.setFont(headFont);
                int index = 0;
                writeRow(sheet, index, table.columns, headStyle);

                // 写入行数据
                for (List<String> row : table.rows) {
                    index++;
                    writeRow(sheet, index, row, null);
                }

                // 检测列最大文字数
                int[] charCounts = new int[table.columns.size()];
                Consumer<List<String>> detectCharCount = row -> {
                    for (int columnIndex = 0; columnIndex < row.size(); ++columnIndex) {
                        int charCount = row.get(columnIndex).getBytes(Charset.forName("GBK")).length;
                        charCounts[columnIndex] = Math.max(charCounts[columnIndex], charCount);
                    }
                };
                detectCharCount.accept(table.columns);
                for (List<String> row : table.rows) {
                    detectCharCount.accept(row);
                }

                // 设置列宽度
                for (int columnIndex = 0; columnIndex < table.columns.size(); ++columnIndex) {
                    // 限定宽度范围
                    int charCount = Math.max(charCounts[columnIndex], 4);
                    charCount = Math.min(charCount, 80);
                    int width = (charCount + 4) * 220;
                    width = Math.min(width, 255*256);
                    sheet.setColumnWidth(columnIndex, width);
                }
            }

            // 生成二进制
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 写入行数据
     */
    void writeRow(SXSSFSheet sheet, int rowIndex, List<String> values, CellStyle style) {
        SXSSFRow row = sheet.createRow(rowIndex);
        row.setHeight((short)400);
        for (int columnIndex = 0; columnIndex < values.size(); ++columnIndex) {
            SXSSFCell cell = row.createCell(columnIndex);

            // 写入数据
            cell.setCellValue(values.get(columnIndex));

            // 设置样式
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }
}
