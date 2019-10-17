package com.zwc.sqldataprocessor.core;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import com.zwc.sqldataprocessor.Global;
import com.zwc.sqldataprocessor.core.entity.DatabaseConfig;

public class DatabaseConfigLoader {

    public static String path = "./databases.txt";

    public static void initializeDefaultConfig() {
        if (Files.exists(Paths.get(path))) {
            return;
        }

        List<DatabaseConfig> list = new ArrayList<>();
        DatabaseConfig config = null;

        config = new DatabaseConfig();
        config.name = "h2";
        config.url = "jdbc:h2:mem:";
        list.add(config);

        config = new DatabaseConfig();
        config.name = "local_mysql";
        config.url = "jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterset=utf-8";
        config.userName = "root";
        config.password = "123456";
        list.add(config);

        String fileContent = JSON.toJSONString(list, SerializerFeature.PrettyFormat);
        Global.writeFile(path, fileContent);
    }

    public static List<DatabaseConfig> loadDatabaseConfigs() {
        String fileContent = Global.readFile(path);
        return JSON.parseArray(fileContent, DatabaseConfig.class);
    }
}
