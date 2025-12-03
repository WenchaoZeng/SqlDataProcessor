package com.zwc.sqldataprocessor.executor;

import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.function.Consumer;

import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.sql.CallStatement;

/**
 * 运行本地工具
 */
public class LocalToolExectuor {
    public static void exec(CallStatement statement, DataList dataList, Consumer<String> logPrinter) {
        String columnName = "call_result";
        int increment = 0;
        while (dataList.columns.contains(columnName)) {
            increment++;
            columnName = columnName + "" + increment;
        }

        // 增加调用结果列
        dataList.columns.add(columnName);
        dataList.columnTypes.add(DataList.ColumnType.TEXT);

        // 调用工具
        int processed = 0;
        for (List<String> row : dataList.rows) {
            // 引用行数据
            String command = statement.command;
            for (int index = 0; index < dataList.columns.size() - 1; index++) {
                String placeHolderName = "{" + dataList.columns.get(index) + "}";
                command = command.replace(placeHolderName, row.get(index));
            }

            row.add(exec(command));

            // 输出已处理百分比
            processed++;
            if (processed % (Math.max(1, dataList.rows.size() / 10)) == 0 || processed == dataList.rows.size()) {
                int percentage = (int) (((double) processed / dataList.rows.size()) * 100);
                logPrinter.accept("已处理: " + percentage + "% (" + processed + "/" + dataList.rows.size() + " 行)");
            }
        }

        logPrinter.accept("已处理完毕, 列名: " + columnName);
    }

    private static String exec(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            // 对于 Windows 系统，使用 cmd /c
            // 对于 Unix/Linux 系统，使用 bash -c
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                processBuilder.command("cmd", "/c", command);
            } else {
                processBuilder.command("bash", "-c", command);
            }

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            process.getOutputStream().close(); // 关闭输出流，防止阻塞进程

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append(System.lineSeparator());
                    }
                    output.append(line);
                }
            }

            // 等待进程执行完成
            int exitCode = process.waitFor();

            // 如果进程执行失败，可以在输出中包含退出码信息
            if (exitCode != 0) {
                output.append(System.lineSeparator())
                      .append("[命令执行失败，退出码: ")
                      .append(exitCode)
                      .append("]")
                    .append(System.lineSeparator())
                    .append("命令: ")
                    .append(command);
            }

            return output.toString();

        } catch (IOException | InterruptedException e) {
            return "命令执行异常: " + e.getMessage();
        }
    }
}
