# SqlDataProcessor

一个基于sql的数据处理工具, 可以通过写sql的方式对excel, csv, 跨库mysql表等进行数据处理和加工.

[点击下载](https://github.com/WenchaoZeng/SqlDataProcessor/releases)

欢迎反馈问题, 提出建议, 甚至是直接贡献代码 (参与进来, 这是最好的方式. :D).

# 使用场景

通过在一个sql文件中写多个导入语句和多个sql语句, 以实现数据的流式处理:

* 表格文件处理/计算/合并
* mysql跨实例数据查询/导出/迁移
* 表格数据导入并迁移到mysql数据库

# 数据库配置文件

在当前目录下, 文件名称为: databases.txt

# 文件结构和语法定义

文件后缀建议为sql, 以让文本编辑器支持sql语法的高亮显示.

## `# import` 导入一个xls, xlsx或csv文件

可以在文件名后面添加 `as $xxx` 的方式来指定结果集的名称, 否则默认为$table.

```sql
# import /Users/xxx/Downloads/xxx.xlsx
# import /Users/xxx/Downloads/xxx.xlsx as $table2
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

注意: 一定要在结果集后面加一个自己的自定义名称, 结果集是以一个sub query的方式运行的, sql标准要求一定要指定一个别名的.

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

## 使用 `##` 或 `--` 或 `#` 来添加注释

当使用 `#` 来注释时, 后面的文字不能是这个工具的关键词, 比如 `export`, `import`, `end`, 一个存在的数据库名, 等等.

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

## 使用 `# end` 来提前结束文件, 后面的sql内容不会再处理.

```sql
# import /Users/xxx/Downloads/xxx.xlsx

# end

-- 从这里开始以下的sql都不会被运行了

# local_db as $table2
select
    id
    text
from test_table2
;
```

## 使用 `# no export nulls` 来指定在导出文件的时候, null被导出为空白, 和空白字符串一样.

## 使用 `# no export nulls` 来指定在导出文件的时候, null被导出为`<null>`, 这样可以看出来具体哪个是null值, 而不是空字符串.