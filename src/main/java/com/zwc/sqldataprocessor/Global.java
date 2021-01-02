package com.zwc.sqldataprocessor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

/**
 * 全局存储区
 */
public class Global {

    /**
     * 软件版本号
     */
    public static String version = "2019-09-30";

    public static List<String> fileList;
    static String fileListPath = "./files.txt";
    static {
        fileList = new ArrayList<>();
        if (Files.exists(Paths.get(fileListPath))) {
            String fileContent = readFile(fileListPath);
            fileList.addAll(Arrays.asList(fileContent.split("\n")));
        }
    }
    public static void addFile(String path) {
        if (fileList.indexOf(path) == 0) {
            return;
        }

        if (!ensureFileExists(path)) {
            return;
        }

        // 自动置顶
        fileList.remove(path);
        fileList.add(0, path);

        saveFileList();
    }

    public static void removeFile(int index) {
        fileList.remove(index);
        saveFileList();
    }

    static void saveFileList() {
        String fileContent = String.join("\n", fileList);
        writeFile(fileListPath, fileContent);
    }

    public static boolean ensureFileExists(String path) {
        if (!Files.exists(Paths.get(path))) {
            alert("文件不存在: " + path);
            return false;
        }
        return true;
    }

    public static void writeFile(String path, String content) {
        try {
            Files.write(Paths.get(path), content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        if (!ensureFileExists(path)) {
            return;
        }

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

    static void alert(String msg) {
        JOptionPane.showMessageDialog(null, msg);
    }
}
