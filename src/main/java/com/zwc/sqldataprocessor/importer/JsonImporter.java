package com.zwc.sqldataprocessor.importer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

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
        Object jsonObject = JSON.parse(contentStr);

        // 指定一个字段路径
        if (StringUtils.isNotBlank(sheetName)) {
            String[] fields = sheetName.split("[.]");
            for (String field : fields) {
                if (!(jsonObject instanceof JSONObject)) {
                    throw new UserException("不支持在json数组里指定字段: " + field);
                }
                if (!((JSONObject) jsonObject).containsKey(field)) {
                    throw new UserException("不存在json字段: " + field);
                }
                jsonObject = ((JSONObject) jsonObject).get(field);
            }
        }

        // 规整化为json数组
        JSONArray jsonArray;
        if (jsonObject instanceof JSONObject) {
            jsonArray = new JSONArray();
            jsonArray.add(jsonObject);
        } else {
            jsonArray = (JSONArray) jsonObject;
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
    void flatJsonRow(String prefix, Object row, LinkedHashMap<String, String> jsonRow) {
        if (row instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) row;
            for (String key : jsonObject.keySet()) {
                Object value = jsonObject.get(key);
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                flatJsonRow(newPrefix, value, jsonRow);
            }
        } else if (row instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) row;
            for (int index = 0; index < jsonArray.size(); ++index) {
                Object value = jsonArray.get(index);
                String newPrefix = prefix.isEmpty() ? "[" + index + "]" : prefix + ".[" + index + "]";
                flatJsonRow(newPrefix, value, jsonRow);
            }
        } else {
            prefix = prefix.isEmpty() ? "value" : prefix;
            jsonRow.put(prefix, Objects.toString(row));
        }
    }
}
