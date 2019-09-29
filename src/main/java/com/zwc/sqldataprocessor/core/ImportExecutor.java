package com.zwc.sqldataprocessor.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.zwc.sqldataprocessor.Global;
import com.zwc.sqldataprocessor.core.entity.DataList;
import com.zwc.sqldataprocessor.core.importer.CsvImporter;
import com.zwc.sqldataprocessor.core.importer.Importer;

public class ImportExecutor {
    public static DataList doImport(String filePath) {

        Importer importer = null;
        if (filePath.endsWith(".csv") || filePath.endsWith(".CSV")) {
            importer = new CsvImporter();
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
