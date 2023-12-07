package com.zwc.sqldataprocessor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

import com.zwc.sqldataprocessor.entity.UserException;
import org.apache.commons.lang3.StringUtils;

public class SqlDataProcessor {
    public static void main(String[] args) throws Exception {

        // 控制台和日志文件同时输出
        String logFileName = "run.log";
        FileHelper.deleteOutFile(logFileName);
        Consumer<String> logPrinter = msg -> {
            System.out.println(msg);
            FileHelper.appendOutFile(logFileName, msg);
            FileHelper.appendOutFile(logFileName, "\n");
        };

        try {
            run(args, logPrinter);
        } catch (Exception ex) {

            // SQL语法错误不用打印调用栈
            if (ex instanceof UserException) {
                logPrinter.accept("错误: " + ex.getMessage());
                return;
            }

            // 输出异常明细
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            ex.printStackTrace(printWriter);
            logPrinter.accept(stringWriter.toString());
        }
    }

    static void run(String[] args, Consumer<String> logPrinter) {
        // 初始化DB配置
        DatabaseConfigLoader.initializeDefaultConfig();

        // 命令行模式
        if (args.length <= 0 || StringUtils.isBlank(args[0])) {
            logPrinter.accept("缺少sql文件路径");
            return;
        }

        String filePath = args[0];
        SqlFileExecutor.exec(filePath, logPrinter);
    }
}