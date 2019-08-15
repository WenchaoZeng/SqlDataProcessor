package com.zwc.sqldataprocessor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日志
 */
public class Log {

    public static void error(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        error(sw.toString());
    }

    public static void info(String msg, Object... params) {
        msg = "[INFO] " + formatMsg(msg, params);
        System.out.println(msg);
        appendToFile(msg);
    }

    public static void error(String msg, Object... params) {
        msg = "[ERROR] " + formatMsg(msg, params);
        System.err.println(msg);
        appendToFile(msg);
    }

    static String formatMsg(String msg, Object... params) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        msg = dateFormat.format(new Date()) + ": " + String.format(msg, params);
        return msg;
    }

    static void appendToFile(String log) {
        Path path = Paths.get("./log.txt");
        synchronized (Global.class) {
            try {
                if (Files.notExists(path)) {
                    Files.createFile(path);
                }
                Files.write(path, log.getBytes(), StandardOpenOption.APPEND);
                Files.write(path, "\n".getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
