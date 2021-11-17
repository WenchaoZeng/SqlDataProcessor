# SqlDataProcessor

一个基于sql的数据处理工具, 可以通过写sql的方式对excel, csv, 跨库mysql表等进行数据处理和加工.

[点击下载](https://github.com/WenchaoZeng/SqlDataProcessor/releases)

# 使用场景

通过在一个sql文件中写多个导入语句和多个sql语句, 以实现数据的流式处理:

* 表格文件处理/计算/合并
* mysql跨实例数据查询/导出/迁移
* 表格数据导入并迁移到mysql数据库

# 文件结构和语法定义

文件后缀建议为sql, 以让文本编辑器支持sql语法的高亮显示.

## `# import` 导入一个xls, xlsx或csv文件

```sql
# import /Users/xxx/Downloads/xxx.xlsx
```

## 使用 `# xxx` 来指定一个数据库连接名称和紧跟着一个或多个SQL语句, 以最后一个SQL语句执行结果作为本次的结果

```sql
# local_db
set @input = 'hello';
update test_table set a = 2 where a = 1;
select
    temp.name,
    temp2.text
from test_table temp
left join test_table2 temp2 on temp2.id = temp.temp2_id
where
    text = @input
order by temp.id desc
limit 10
;
```

## 使用 `as $xxx` 来指定结果集的名称, 若不指定, 则默认结果集名称为$table

```sql
# import /Users/xxx/Downloads/xxx.xlsx as $table1

# local_db as $table2
select
    temp.name
from test_table temp;
```

## 在SQL中可以使用 `$xxx` 的方式来引用之前的结果集

```sql
# import /Users/xxx/Downloads/xxx.xlsx

# local_db as $table2
select
    id
    text
from test_table2
;

# local_db
select
    temp.name,
    temp2.text,
    temp3.label
from $table temp
left join $table2 temp2 on temp2.id = temp.id2
left join test_table3 temp3 on temp3.id = temp.id2
;

# local_db
select * from $table temp;
```

## 使用 `# export`  来导出上一个执行的结果集

```sql
-- 使用默认路径和文件名
# export

-- 指定导出文件的名称
# export 这是一个文件名

-- 使用指定的完整路径
# export /Users/wenchaozeng/Downloads/aa.csv
```

## 使用 `##` 和 `--`  来添加注释

```sql

-- 这是注释
## 这是注释
# local_db
select
    temp.name
    ## 这是注释
from test_table temp
;
```
