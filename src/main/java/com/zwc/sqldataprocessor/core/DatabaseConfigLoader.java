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
        DatabaseConfig config = new DatabaseConfig();
        config.name = "local";
        config.dbHost = "127.0.0.1";
        config.dbName = "test_db";
        config.dbUserName = "root";
        config.dbPassword = "123456";
        list.add(config);
        config = new DatabaseConfig();
        config.name = "local2";
        config.dbHost = "127.0.0.1";
        config.dbName = "test_db2";
        config.dbUserName = "root";
        config.dbPassword = "123456";
        list.add(config);
        String fileContent = JSON.toJSONString(list, SerializerFeature.PrettyFormat);
        Global.writeFile(path, fileContent);
    }
}
