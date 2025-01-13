package com.zwc.sqldataprocessor.exporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import com.google.protobuf.ServiceException;
import com.zwc.sqldataprocessor.FileHelper;
import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.UserException;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XlsxExporter implements Exporter {

    /**
     * 工作簿缓存, 用于多次写入同一个xlsx文件时, 避免重复加载文件和创建工作簿
     */
    static Map<String, SXSSFWorkbook> workbooks = new LinkedHashMap<>();

    @SneakyThrows
    @Override
    public void export(String filePath, DataList table, String sheetName, boolean exportNulls) {
        if (CollectionUtils.isEmpty(table.columns)) {
            throw new UserException("导出失败, 数据集里列数为0.");
        }

        if (StringUtils.isBlank(sheetName)) {
            sheetName = "sheet1";
        }

        // 创建或获取工作簿
        SXSSFWorkbook workbook;
        if (workbooks.containsKey(filePath)) {
            workbook = workbooks.get(filePath);
        } else {
            workbook = new SXSSFWorkbook(1000);
            workbooks.put(filePath, workbook);
        }

        if (workbook.getSheet(sheetName) != null) {
            throw new UserException("请勿往同一个xlsx文件重复导出相同的sheet名称: " + sheetName);
        }

        SXSSFSheet sheet = workbook.createSheet(sheetName);
        sheet.createFreezePane(0, 1); // 设置表头行固定

        // 写入表头
        XSSFCellStyle headStyle = (XSSFCellStyle) workbook.createCellStyle();
        headStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont headFont = (XSSFFont )workbook.createFont();
        headFont.setBold(true);
        headStyle.setFont(headFont);
        int index = 0;
        writeRow(sheet, index, table.columns, headStyle, false);

        // 写入行数据
        for (List<String> row : table.rows) {
            index++;
            writeRow(sheet, index, row, null, exportNulls);
        }

        // 检测列最大文字数
        int[] charCounts = new int[table.columns.size()];
        Consumer<List<String>> detectCharCount = row -> {
            for (int columnIndex = 0; columnIndex < row.size(); ++columnIndex) {
                if (row.get(columnIndex) == null) {
                    continue;
                }
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

    @Override
    public String getExtension() {
        return "xlsx";
    }

    @SneakyThrows
    @Override
    public void flushAllFiles() {
        for (Entry<String, SXSSFWorkbook> stringSXSSFWorkbookEntry : workbooks.entrySet()) {
            String filePath = stringSXSSFWorkbookEntry.getKey();
            SXSSFWorkbook workbook = stringSXSSFWorkbookEntry.getValue();
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
            workbook.close();
        }
        workbooks = new LinkedHashMap<>();
    }

    /**
     * 写入行数据
     */
    void writeRow(SXSSFSheet sheet, int rowIndex, List<String> values, CellStyle style, boolean exportNulls) {
        SXSSFRow row = sheet.createRow(rowIndex);
        row.setHeight((short)400);
        for (int columnIndex = 0; columnIndex < values.size(); ++columnIndex) {
            SXSSFCell cell = row.createCell(columnIndex);

            // 写入数据
            String value = values.get(columnIndex);
            cell.setCellValue(value == null && exportNulls ? "<null>" : value);

            // 设置样式
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }
}
