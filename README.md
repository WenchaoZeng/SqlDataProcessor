# SqlDataProcessor

一个基于sql的数据处理工具, 可以通过写sql的方式对excel, csv, 夸库mysql表等进行数据处理和加工.

# 使用场景

通过在一个sql文件中写多个导入语句和多个sql语句, 以实现数据的流式处理:

* 表格文件处理/计算/合并
* mysql跨实例数据查询/导出/迁移
* 表格数据导入并迁移到mysql数据库

# 文件结构和语法定义

推荐文件后缀为sql, 以让文本编辑器支持sql语法的高亮显示.

## `# import` 导入一个xls, xlsx或csv文件

```sql
# import /Users/xxx/Downloads/xxx.xlsx
```

## `# xxx` 指定一个数据库连接名称和一个SQL, 以`;`分号结束

```sql
# local_db
select
    temp.name,
    temp2.text
from test_table temp
left join test_table2 temp2 on temp2.id = temp.temp2_id
where
    text = 'hello'
order by temp.id desc
limit 10
;
```

SQL中可以引用之前的结果集

```sql
# local_db
select
    temp.name
from $table2 temp
;

# local_db
select
    temp.name,
    temp2.text
from test_table temp
left join test_table2 temp2 on temp2.id = temp.temp2_id
;
```

## `##` 注释

```sql

## 这是注释
# local_db
select
    temp.name
    ## 这是注释
from test_table temp
;
```

## `as $xxx` 指定结果集的名称, 若不指定, 则默认结果集名称为$table

```sql
# import /Users/xxx/Downloads/xxx.xlsx as $table1

# local_db as $table2
select
    temp.name
from $table1 temp;
```
