package com.zwc.sqldataprocessor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

/**
 * 文件工具类
 */
public class FileHelper {

    public static void writeFile(String path, String content) {
        writeFile(path, content.getBytes());
    }

    public static void writeFile(String path, byte[] bytes) {
        try {
            Files.write(Paths.get(path), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeOutputFile(String path, String content) {
        return writeOutputFile(path, content.getBytes());
    }

    public static String writeOutputFile(String path, byte[] bytes) {

        // 创建结果目录
        try {
            Path outputDirectoryPath = Paths.get("./output");
            if (!Files.exists(outputDirectoryPath)) {
                Files.createDirectory(outputDirectoryPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        path = "./output/" + path;
        writeFile(path, bytes);
        return path;
    }

    public static String readFile(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void openFile(String path) {
        String os = System.getProperty("os.name");
        String cmd = "open " + path;
        if (os.contains("Windows")) {
            path  = path.replace("/", "\\");
            cmd = "explorer.exe " + path;
        }
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
