package com.zwc.sqldataprocessor.core;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import com.zwc.sqldataprocessor.Global;
import com.zwc.sqldataprocessor.Log;
import com.zwc.sqldataprocessor.core.entity.DatabaseConfig;

public class DatabaseConfigLoader {

    public static String path = "./databases.txt";
    static List<DatabaseConfig> databaseConfigs = null;
    static Map<String, Connection> conns = null;

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

    public static Connection getConn(String databaseName) {
        if (conns == null) {
            conns = new HashMap<>();
        }

        // 尝试从连接池里获取连接
        Connection conn = conns.get(databaseName);
        if (conn != null) {
            return conn;
        }

        // 读取数据库配置
        DatabaseConfig dbConfig = getDbConfig(databaseName);

        // 新建数据库连接
        try {
            conn = DriverManager.getConnection(dbConfig.url, dbConfig.userName, dbConfig.password);
            conns.put(databaseName, conn);
            return conn;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean isMySql(String databaseName) {
        DatabaseConfig dbConfig = getDbConfig(databaseName);
        return dbConfig.url.contains("mysql");
    }

    static DatabaseConfig getDbConfig(String databaseName) {
        loadDatabaseConfigs();
        DatabaseConfig dbConfig = databaseConfigs.stream().filter(x -> x.name.equals(databaseName)).findAny().orElse(null);
        if (dbConfig == null) {
            throw new RuntimeException(databaseName +"数据库的配置不存在");
        }
        return dbConfig;
    }

    static void loadDatabaseConfigs() {
        if (databaseConfigs != null) {
            return;
        }

        String fileContent = Global.readFile(path);
        databaseConfigs = JSON.parseArray(fileContent, DatabaseConfig.class);
        for (DatabaseConfig dbConfig : databaseConfigs) {
            dbConfig.url += "&allowMultiQueries=true";
        }
    }

    public static void closeConnections() {
        if (conns == null) {
            return;
        }

        for (Connection conn : conns.values()) {
            try {
                conn.close();
            } catch (Exception ex) {
                Log.error(ex);
            }
        }

        conns = null;
        databaseConfigs = null;
    }
}
