package com.zwc.sqldataprocessor.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.zwc.sqldataprocessor.Global;
import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.importer.CsvImporter;
import com.zwc.sqldataprocessor.core.importer.Importer;
import com.zwc.sqldataprocessor.core.importer.XlsImporter;

public class ImportExecutor {
    public static DataList doImport(String filePath) {

        Importer importer = null;
        if (filePath.endsWith(".csv") || filePath.endsWith(".CSV")) {
            importer = new CsvImporter();
        } else if (filePath.endsWith(".xls") || filePath.endsWith(".XLS")) {
            importer = new XlsImporter(false);
        } else if (filePath.endsWith(".xlsx") || filePath.endsWith(".XLSX")) {
            importer = new XlsImporter(true);
        }

        if (importer == null) {
            throw new RuntimeException("导入文件格式无法识别");
        }

        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return importer.doImport(fileContent);
    }
}
