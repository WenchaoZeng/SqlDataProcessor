package com.zwc.sqldataprocessor;

import org.apache.commons.lang3.StringUtils;

public class SqlDataProcessor {
    public static void main(String[] args) throws Exception {

        // 初始化DB配置
        DatabaseConfigLoader.initializeDefaultConfig();

        // 命令行模式
        if (args.length > 0 && StringUtils.isNotBlank(args[0]) && args[0].contains(".")) {
            String filePath = args[0];

            SqlFileExecutor.exec(filePath, msg -> {
                System.out.println(msg);
            });
            return;
        }

        System.out.println("缺少sql文件路径");
    }
}
