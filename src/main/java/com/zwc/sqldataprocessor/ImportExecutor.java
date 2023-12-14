package com.zwc.sqldataprocessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.UserException;
import com.zwc.sqldataprocessor.entity.sql.ImportStatement;
import com.zwc.sqldataprocessor.importer.CsvImporter;
import com.zwc.sqldataprocessor.importer.Importer;
import com.zwc.sqldataprocessor.importer.XlsImporter;
import org.apache.commons.lang3.StringUtils;

public class ImportExecutor {
    public static DataList doImport(ImportStatement statement) {
        Importer importer = null;
        String filePath = statement.filePath.toLowerCase();
        if (filePath.endsWith(".csv")) {
            importer = new CsvImporter();
        } else if (filePath.endsWith(".xls")) {
            importer = new XlsImporter(false);
        } else if (filePath.endsWith(".xlsx")) {
            importer = new XlsImporter(true);
        }

        if (importer == null) {
            throw new UserException("导入文件格式无法识别");
        }

        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(Paths.get(statement.filePath));
        } catch (NoSuchFileException ex) {
            throw new UserException("导入文件不存在");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        DataList table = importer.doImport(fileContent, statement.sheetName);

        removeEmptyColumn(table);
        calculateColumnType(table);

        return table;
    }

    static Pattern intPattern = Pattern.compile("^-?\\d+$");
    static Pattern decimalPattern = Pattern.compile("^(-?\\d+)(\\.\\d+)?$");
    static void calculateColumnType(DataList table) {
        for (int columnIndex = 0; columnIndex < table.columns.size(); ++columnIndex) {

            // 空字符串
            boolean isEmpty = true;
            for (List<String> row : table.rows) {
                String value = row.get(columnIndex);
                if (!StringUtils.isBlank(value)) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                continue;
            }

            // 整数
            boolean isInt = true;
            for (List<String> row : table.rows) {
                String value = row.get(columnIndex);
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                Matcher matcher = intPattern.matcher(value);
                if (!matcher.find()) {
                    isInt = false;
                }
            }
            if (isInt) {
                table.columnTypes.set(columnIndex, ColumnType.INT);
                continue;
            }

            // 小数
            boolean isDecimal = true;
            for (List<String> row : table.rows) {
                String value = row.get(columnIndex);
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                Matcher matcher = decimalPattern.matcher(value);
                if (!matcher.find()) {
                    isDecimal = false;
                }
            }
            if (isDecimal) {
                table.columnTypes.set(columnIndex, ColumnType.DECIMAL);
                continue;
            }
        }
    }

    static void removeEmptyColumn(DataList table) {
        for (int columnIndex = table.columns.size() - 1; columnIndex >= 0; --columnIndex) {
            String columnName = table.columns.get(columnIndex);
            if (StringUtils.isBlank(columnName)) {
                table.columns.remove(columnIndex);
                table.columnTypes.remove(columnIndex);
                for (List<String> row : table.rows) {
                    row.remove(columnIndex);
                }
            }
        }
    }
}
