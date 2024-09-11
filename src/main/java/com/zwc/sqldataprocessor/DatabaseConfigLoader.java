package com.zwc.sqldataprocessor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import com.zwc.sqldataprocessor.dbexecutor.DbExecutor;
import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import org.apache.commons.lang3.StringUtils;

public class DatabaseConfigLoader {

    public static String path = "./databases.json";
    static Map<String, DatabaseConfig> databaseConfigs = null;
    static Map<String, Connection> conns = null;

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

            // 执行初始化的sql
            String sql = dbConfig.dbExecutor.getSqlAfterConnect();
            if (StringUtils.isNotBlank(sql)) {
                SqlExecutor.execRawSql(sql, databaseName);
            }

            return conn;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    static DatabaseConfig getDbConfig(String databaseName) {
        loadDatabaseConfigs();
        return databaseConfigs.get(databaseName);
    }

    static void loadDatabaseConfigs() {
        if (databaseConfigs != null) {
            return;
        }

        initializeDefaultConfig();

        String fileContent = FileHelper.readFile(path);
        List<DatabaseConfig> databaseConfigList = JSON.parseArray(fileContent, DatabaseConfig.class);
        for (DatabaseConfig dbConfig : databaseConfigList) {
            dbConfig.name = StringUtils.trimToEmpty(dbConfig.name);
            dbConfig.url = StringUtils.trimToEmpty(dbConfig.url);
            dbConfig.dbExecutor = DbExecutor.getDbExecutor(dbConfig.url);
            dbConfig.url = appendUrlSuffix(dbConfig.url, dbConfig.dbExecutor.getUrlSuffix());
        }

        databaseConfigs = databaseConfigList.stream().collect(Collectors.toMap(x -> x.name, x -> x, (a, b) -> a));
    }

    static String appendUrlSuffix(String url, String urlSuffix) {
        if (StringUtils.isBlank(urlSuffix)) {
            return url;
        }

        if (url.endsWith("?") || url.endsWith("&")) {
            return url + urlSuffix;
        }
        if (url.contains("?")) {
            return url + "&" + urlSuffix;
        }

        return url + "?" + urlSuffix;
    }

    static void initializeDefaultConfig() {
        if (Files.exists(Paths.get(path))) {
            return;
        }

        List<DatabaseConfig> list = DbExecutor.dbExecutors.stream()
            .map(x -> x.getDefaultConfig())
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        String fileContent = JSON.toJSONString(list, SerializerFeature.PrettyFormat);
        FileHelper.writeFile(path, fileContent);
    }

    public static void closeConnections() {
        if (conns == null) {
            return;
        }

        for (Connection conn : conns.values()) {
            try {
                conn.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }

        conns = null;
        databaseConfigs = null;
    }
}
