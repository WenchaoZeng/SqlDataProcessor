# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

SqlDataProcessor 是一个基于SQL的数据处理工具，支持通过SQL方式对 Excel、CSV、JSON 文件以及跨库 MySQL 表进行数据处理和加工。支持 H2、MySQL、PostgreSQL、SQLite 数据库。

## 开发命令

### 构建项目
```bash
mvn clean package
```

### 打包发布版本
```bash
./package.sh
```
这会在 `./target/release/` 目录创建发布包，并自动生成 `java_SqlDataProcessor.zip`。

### 运行测试
```bash
./start.sh /path/to/test.sql
```

## 项目架构

### 核心流程

1. **SQL 文件解析** (`SqlLoader`)
   - 解析自定义SQL语法，将文件内容解析为 `Statement` 对象列表
   - 支持的语句类型：`ImportStatement`、`SqlStatement`、`ExportStatement`、`CallStatement`

2. **语句执行** (`SqlFileExecutor`)
   - 按顺序执行各个语句
   - 维护结果集表 (`Map<String, DataList> tables`)
   - 自动导出最后一个结果集

3. **数据库抽象** (`DbExecutor` 及子类)
   - 为不同数据库提供统一的 SQL 渲染接口
   - 实现：`MySqlDbExecutor`、`H2DbExecutor`、`PostgreSqlDbExecutor`、`SqLiteDbExecutor`

### 关键组件

- **DatabaseConfigLoader**: 管理数据库配置 (`databases.json`) 和连接池
- **SqlExecutor**: 执行SQL并处理结果集引用 (`$xxx` 语法)
- **ImportExecutor**: 导入各种文件格式 (CSV、XLS、XLSX、JSON)
- **ExportExecutor**: 导出结果集到 CSV 或 XLSX

### 结果集引用机制

SQL中可以通过 `$xxx` 引用之前的结果集。有两种实现方式：

1. **子查询模式** (默认): 将结果集数据作为子查询嵌入SQL
2. **临时表模式** (`useTempTables: true`): 将结果集导入临时表，适合大数据量

临时表模式相关配置：
- `useTempTables`: 启用临时表模式
- `useRealTables`: 使用持久化表代替临时表
- `uploadBatchSize`: 分批导入的批次大小 (默认1000)

## 代码规范

- Java 版本: 1.8
- 使用 Lombok 简化实体类
- 使用 Hutool 工具库
- 异常处理: 用户错误抛出 `UserException`，系统错误抛出 `RuntimeException`
- 所有输出使用 `Consumer<String> logPrinter`，支持同时输出到控制台和日志文件

## 导入器实现

新增导入格式需实现 `Importer` 接口，并在 `ImportExecutor.importers` 中注册：

```java
importers.put(".ext", new NewImporter());
```

## 导出器实现

新增导出格式需实现 `Exporter` 接口，并在 `ExportExecutor` 中添加相应逻辑。

## 数据库支持

新增数据库支持需：

1. 创建 `XXXDbExecutor` 继承 `DbExecutor`
2. 实现抽象方法：SQL渲染、异常转换等
3. 在 `DbExecutor.dbExecutors` 静态列表中注册
4. 在 `pom.xml` 添加相应 JDBC 驱动依赖

## 测试文件位置

- `test.sql`: 示例SQL文件
- `output/`: 默认输出目录
- `run.log`: 运行日志
- `current.sql`: 最近执行的SQL (调试用)
