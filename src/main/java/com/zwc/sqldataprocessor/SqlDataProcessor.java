package com.zwc.sqldataprocessor;

public class SqlDataProcessor {
    public static void main(String[] args) throws Exception {

        // 初始化DB配置
        DatabaseConfigLoader.initializeDefaultConfig();

        // 命令行模式
        if (args.length > 0) {
            String filePath = args[0];

            SqlFileExecutor.exec(filePath, msg -> {
                System.out.println(msg);
            });
            return;
        }

        System.out.println("sql文件路径缺失");
    }
}
