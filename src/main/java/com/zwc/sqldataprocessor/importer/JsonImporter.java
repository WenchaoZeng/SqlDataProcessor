package com.zwc.sqldataprocessor.importer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zwc.sqldataprocessor.entity.DataList;
import com.zwc.sqldataprocessor.entity.DataList.ColumnType;
import com.zwc.sqldataprocessor.entity.UserException;
import org.apache.commons.lang3.StringUtils;

/**
 * json文件导入
 */
public class JsonImporter implements Importer {

    @Override
    public DataList doImport(byte[] content, String sheetName, int headRowNo) {

        // 读取json
        String contentStr = new String(content);
        JSONConfig jsonConfig = JSONConfig.create().setIgnoreNullValue(false);
        Object json = JSONUtil.parse(contentStr, jsonConfig);

        // 指定字段路径
        if (StringUtils.isNotBlank(sheetName)) {
            String[] fields = sheetName.split("[.]");
            for (String field : fields) {
                if (!(json instanceof JSONObject)) {
                    throw new UserException("不支持在json数组里指定字段: " + field);
                }
                JSONObject jsonObject = (JSONObject) json;
                if (!jsonObject.containsKey(field)) {
                    throw new UserException("不存在json字段: " + field);
                }
                json = jsonObject.getJSONObject(field);
            }
        }

        // 规整化为json数组
        JSONArray jsonArray;
        if (json instanceof JSONObject) {
            jsonArray = new JSONArray(1);
            jsonArray.add(json);
        } else {
            jsonArray = (JSONArray) json;
        }

        // 扁平化json数据
        List<LinkedHashMap<String, String>> jsonRows = new ArrayList<>();
        for (Object row : jsonArray) {
            LinkedHashMap<String, String> jsonRow = new LinkedHashMap<>();
            jsonRows.add(jsonRow);
            flatJsonRow("", row, jsonRow);
        }

        // 计算表头
        DataList table = new DataList();
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        for (LinkedHashMap<String, String> jsonRow : jsonRows) {
            columns.addAll(jsonRow.keySet());
        }
        table.columns = new ArrayList<>(columns);
        table.columnTypes = table.columns.stream().map(x -> ColumnType.TEXT).collect(Collectors.toList());

        // 设置表数据
        for (LinkedHashMap<String, String> jsonRow : jsonRows) {
            List<String> row = new ArrayList<>();
            for (String column : table.columns) {
                row.add(jsonRow.get(column));
            }
            table.rows.add(row);
        }

        return table;
    }

    /**
     * 扁平化字段名
     */
    void flatJsonRow(String prefix, Object obj, LinkedHashMap<String, String> jsonRow) {
        if (obj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) obj;
            for (String key : jsonObject.keySet()) {
                Object value = jsonObject.get(key);
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                flatJsonRow(newPrefix, value, jsonRow);
            }
        } else if (obj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) obj;
            for (int index = 0; index < jsonArray.size(); ++index) {
                Object value = jsonArray.get(index);
                String newPrefix = prefix.isEmpty() ? "[" + index + "]" : prefix + ".[" + index + "]";
                flatJsonRow(newPrefix, value, jsonRow);
            }
        } else {
            prefix = prefix.isEmpty() ? "value" : prefix;
            String value = obj == null || obj instanceof JSONNull ? null : String.valueOf(obj);
            if (obj instanceof Number) { // 还原原始的数值
                if (value.contains("E")) {
                    value = new BigDecimal(value).toPlainString();
                } else if (value.endsWith(".0")) {
                    value = value.substring(0, value.lastIndexOf("."));
                }
            }
            jsonRow.put(prefix, value);
        }
    }
}
