# 代码生成器模块

## 模块简介

代码生成器是一个功能强大的代码自动生成工具，支持从数据库表反向生成完整的 CRUD 代码，包括后端和前端代码。

## 技术栈

- **Spring Boot 3.1.5**
- **MyBatis Plus 3.5.5**
- **模板引擎**: FreeMarker 2.3.32、Velocity 2.3、Thymeleaf 3.1.2
- **数据库**: MySQL 8.0、PostgreSQL 42.7、Oracle 21
- **工具**: Hutool、Google Java Format、Commons Compress

## 目录结构

```
basebackend-code-generator/
├── src/main/java/com/basebackend/generator/
│   ├── config/                           # 配置类
│   ├── core/                             # 核心功能
│   │   ├── engine/                       # 模板引擎
│   │   │   ├── TemplateEngine.java       # 模板引擎接口
│   │   │   ├── FreeMarkerTemplateEngine.java
│   │   │   ├── VelocityTemplateEngine.java
│   │   │   ├── ThymeleafTemplateEngine.java
│   │   │   └── TemplateEngineFactory.java
│   │   ├── metadata/                     # 元数据
│   │   │   ├── DatabaseMetadataReader.java
│   │   │   ├── MySQLMetadataReader.java
│   │   │   ├── TableMetadata.java
│   │   │   └── ColumnMetadata.java
│   │   └── strategy/                     # 策略
│   │       └── NamingStrategy.java       # 命名策略
│   ├── entity/                           # 实体类
│   │   ├── DatabaseType.java             # 数据库类型枚举
│   │   ├── EngineType.java               # 引擎类型枚举
│   │   ├── GenerateType.java             # 生成类型枚举
│   │   ├── GenerateStatus.java           # 生成状态枚举
│   │   ├── GenDataSource.java            # 数据源实体
│   │   ├── GenTemplateGroup.java         # 模板分组实体
│   │   ├── GenTemplate.java              # 模板实体
│   │   ├── GenProject.java               # 项目配置实体
│   │   ├── GenHistory.java               # 生成历史实体
│   │   └── GenTypeMapping.java           # 类型映射实体
│   ├── mapper/                           # Mapper接口
│   │   ├── GenDataSourceMapper.java
│   │   ├── GenTemplateGroupMapper.java
│   │   ├── GenTemplateMapper.java
│   │   ├── GenHistoryMapper.java
│   │   └── GenTypeMappingMapper.java
│   ├── service/                          # 服务层
│   │   └── GeneratorService.java         # 代码生成服务
│   ├── controller/                       # 控制层
│   │   ├── GeneratorController.java      # 代码生成控制器
│   │   ├── DataSourceController.java     # 数据源管理控制器
│   │   └── TemplateController.java       # 模板管理控制器
│   ├── dto/                              # DTO
│   │   ├── GenerateRequest.java
│   │   ├── GenerateResult.java
│   │   └── PreviewFile.java
│   ├── util/                             # 工具类
│   │   ├── DataSourceUtils.java
│   │   └── ZipUtils.java
│   └── GeneratorApplication.java         # 启动类
│
└── src/main/resources/
    ├── templates/freemarker/             # 内置模板
    │   ├── entity.ftl                    # 实体类模板
    │   ├── mapper.ftl                    # Mapper模板
    │   ├── service.ftl                   # Service接口模板
    │   ├── serviceImpl.ftl               # Service实现模板
    │   └── controller.ftl                # Controller模板
    ├── db/migration/                     # 数据库迁移
    │   └── V2.0__create_generator_tables.sql
    ├── application.yml                   # 配置文件
    └── application-dev.yml               # 开发环境配置
```

## 核心功能

### 1. 多数据库支持
- MySQL
- PostgreSQL
- Oracle

### 2. 多模板引擎
- FreeMarker
- Velocity
- Thymeleaf

### 3. 代码生成能力
- **后端**: Entity、Mapper、Service、Controller
- **前端**: React 页面、TypeScript 类型、API 接口
- **测试**: JUnit 单元测试
- **批量生成**和**增量更新**

### 4. 模板管理
- 模板 CRUD
- 模板分组
- 自定义模板
- 模板导入导出

## 快速开始

### 1. 启动服务

```bash
cd basebackend-code-generator
mvn spring-boot:run
```

服务默认端口: `8090`

Swagger UI: http://localhost:8090/swagger-ui.html

### 2. 配置数据源

```bash
POST /api/generator/datasource
{
  "name": "本地MySQL",
  "dbType": "MYSQL",
  "host": "localhost",
  "port": 3306,
  "databaseName": "basebackend",
  "username": "root",
  "password": "root"
}
```

### 3. 生成代码

```bash
POST /api/generator/generate
{
  "datasourceId": 1,
  "tableNames": ["sys_user"],
  "templateGroupId": 1,
  "generateType": "DOWNLOAD",
  "packageName": "com.basebackend.demo",
  "moduleName": "demo",
  "author": "System"
}
```

## API 文档

### 数据源管理

- `GET /api/generator/datasource` - 分页查询数据源
- `GET /api/generator/datasource/{id}` - 查询数据源详情
- `POST /api/generator/datasource` - 创建数据源
- `PUT /api/generator/datasource/{id}` - 更新数据源
- `DELETE /api/generator/datasource/{id}` - 删除数据源
- `POST /api/generator/datasource/test` - 测试连接
- `GET /api/generator/datasource/{id}/tables` - 获取表列表

### 模板管理

- `GET /api/generator/template/group` - 查询模板分组
- `GET /api/generator/template/group/{groupId}/templates` - 查询分组模板
- `GET /api/generator/template/{id}` - 查询模板详情
- `POST /api/generator/template` - 创建模板
- `PUT /api/generator/template/{id}` - 更新模板
- `DELETE /api/generator/template/{id}` - 删除模板

### 代码生成

- `POST /api/generator/generate` - 生成并下载代码
- `POST /api/generator/preview` - 预览代码

## 数据库表

- `gen_datasource` - 数据源配置表
- `gen_template_group` - 模板分组表
- `gen_template` - 代码模板表
- `gen_project` - 项目配置表
- `gen_history` - 生成历史表
- `gen_history_detail` - 生成文件明细表
- `gen_type_mapping` - 字段类型映射表

## 配置说明

```yaml
server:
  port: 8090

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/basebackend
    username: root
    password: root

generator:
  default-author: System
  default-engine: FREEMARKER
  temp-dir: /tmp/generator
```

## 模板开发

### 可用变量

- `packageName` - 包名
- `moduleName` - 模块名
- `author` - 作者
- `date` - 日期
- `tableName` - 表名
- `tableComment` - 表注释
- `className` - 类名
- `variableName` - 变量名
- `urlPath` - URL路径
- `columns` - 字段列表
- `primaryKey` - 主键
- `hasDateTime` - 是否有日期时间类型
- `hasBigDecimal` - 是否有BigDecimal类型

### FreeMarker 示例

```freemarker
package ${packageName}.entity;

/**
 * ${tableComment}
 * @author ${author}
 */
public class ${className} {
<#list columns as column>
    private ${column.javaType} ${column.javaField};
</#list>
}
```

## 扩展开发

### 添加新数据库

1. 在 `DatabaseType` 枚举中添加
2. 实现 `DatabaseMetadataReader` 接口
3. 添加类型映射数据

### 添加新模板引擎

1. 在 `EngineType` 枚举中添加
2. 实现 `TemplateEngine` 接口
3. 注册为 Spring Bean

## 注意事项

1. 数据库表必须有主键 `id`
2. 建议添加审计字段：`create_time`、`update_time`、`create_by`、`update_by`
3. 建议添加逻辑删除字段：`deleted`
4. 表和字段必须添加注释
5. 生成代码后需要检查和调整

## 更多文档

详细使用指南请参考: [CODE-GENERATOR-GUIDE.md](../CODE-GENERATOR-GUIDE.md)

## 许可证

Copyright © 2025 BaseBackend
