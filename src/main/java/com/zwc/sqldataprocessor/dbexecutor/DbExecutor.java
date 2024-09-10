package com.zwc.sqldataprocessor.dbexecutor;

import java.util.ArrayList;
import java.util.List;

import com.zwc.sqldataprocessor.entity.DatabaseConfig;
import com.zwc.sqldataprocessor.entity.UserException;
import org.apache.commons.lang3.StringUtils;

public abstract class DbExecutor {
    public final static List<DbExecutor> dbExecutors = new ArrayList<>();

    public static String appendUrlSuffix(String url) {
        DbExecutor dbExecutor = dbExecutors.stream().filter(x -> belongs(url, x.getJdbcDriverName())).findFirst().orElse(null);
        if (dbExecutor == null) {
            throw new UserException("不支持的数据库url, 请检查数据库的url地址是否填写有误");
        }

        String urlSuffix = dbExecutor.getUrlSuffix();
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

    static boolean belongs(String url, String driverName) {
        return url.toLowerCase().startsWith("jdbc:" + driverName);
    }

    public abstract DatabaseConfig getDefaultConfig();
    public abstract String getJdbcDriverName();
    public abstract String getUrlSuffix();
}
