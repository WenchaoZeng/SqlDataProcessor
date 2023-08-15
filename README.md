# SqlDataProcessor

一个基于sql的数据处理工具, 可以通过写sql的方式对excel, csv, 跨库mysql表等进行数据处理和加工.

[演示视频](https://www.bilibili.com/video/BV1ou4y1R7tj/)

欢迎反馈问题, 提出建议, 甚至是直接贡献代码 (参与进来, 这是最好的方式. :D).

# 下载和运行

从 [releases](https://github.com/WenchaoZeng/SqlDataProcessor/releases) 里面下载最新的zip包, 解压到一个特定目录.

运行需要java 1.8版本以上运行时环境, 请使用下面命令来检查你是否已经安装java运行时环境并且版本是1.8以上:

```shell
java --version
```

准备一个测试的sql文件, 内容为:

```sql
# h2
select 1 as a;
```

从命令行进入目录, 执行下面命令:

```shell
./start.sh /你的sql文件路径/文件名称.sql
```

正常执行的话, 显示结果类似这样:

```text
~/xxxx$ ./start.sh /Users/xxx/test.sql
执行:  /Users/xxx/test.sql
==============================
SQL: h2
select 1 as a;

结果集: table, 行数: 1, 耗时: 149毫秒
==============================
导出结果集table
导出文件路径为: /Users/xxx/SqlDataProcessor/./output/./table.csv
```

正常情况下结果文件会自动打开的. 如果没有自动打开的话, 可以手动进入output目录, 然后双击里面的csv文件看是不是没有设置默认打开方式. 设置好默认打开方式后, 后续再执行就会自动打开了.

# 使用场景

通过在一个sql文件中写多个导入语句和多个sql语句, 以实现数据的流式处理:

* 表格文件处理/计算/合并
* mysql跨实例数据查询/导出/迁移
* 表格数据导入并迁移到mysql数据库

# 数据库配置文件

在当前目录下, 文件名称为: `databases.json`. 首次运行时会自动生成这个文件的默认内容, 默认自带一个 [h2](http://www.h2database.com/html/commands.html) 内存数据库, 名称为 `h2`, 可以做简单的测试和使用.

# SQL文件结构和语法定义

文件后缀建议为sql, 以让文本编辑器支持sql语法的高亮显示.

## `# import` 导入一个xls, xlsx或csv文件

可以在文件名后面添加 ` as $xxx` 的方式来指定结果集的名称, 否则默认为$table.

```sql
# import /Users/xxx/Downloads/xxx.xlsx
# import /Users/xxx/Downloads/xxx.xlsx as $table2
```

如果想指定导入具体的一个sheet, 可以这样:

```sql
# import /Users/xxx/Downloads/xxx.xlsx[sheet名称1]
# import /Users/xxx/Downloads/xxx.xlsx[sheet名称2] as $table2
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

默认情况下, 最后一个执行的结果集会被自动导出和打开的. `export` 这个命令主要用途是用来在一个sql文件的运行阶段导出各个不同的结果集, 或者想自己执行结果文件的名称和路径.

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

## 关于空值null的导出显示控制

默认情况下, 值为null会在导出时候显示成空字符串. 可以使用 `# export nulls` 来指定在导出文件的时候, null被导出为`<null>`, 这样可以看出来具体哪个是null值, 区分与空字符串. 然后, 后续可以使用 `# no export nulls` 来恢复默认行为, 即null被导出为空白, 和空字符串显示一样.