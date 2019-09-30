package com.zwc.sqldataprocessor;

import java.io.IOException;
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
    public static String version = "0.1.0";

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

        // 自动置顶
        if (fileList.contains(path)) {
            fileList.remove(path);
        }
        fileList.add(0, path);

        // 保存
        String fileContent = String.join("\n", fileList);
        writeFile(fileListPath, fileContent);
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
            return new String(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void alert(String msg) {
        JOptionPane.showMessageDialog(null, msg);
    }
}
