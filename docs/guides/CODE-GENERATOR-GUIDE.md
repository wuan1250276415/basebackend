# 代码生成器平台使用指南

## 概述

代码生成器平台是一个功能强大的代码自动生成工具，支持从数据库表反向生成完整的 CRUD 代码，包括后端实体类、Mapper、Service、Controller 以及前端 React 页面代码。

## 功能特性

### 核心功能
- ✅ **多数据库支持**: MySQL、PostgreSQL、Oracle
- ✅ **多模板引擎**: FreeMarker、Velocity、Thymeleaf
- ✅ **后端代码生成**: Entity、Mapper、Service、Controller
- ✅ **前端代码生成**: React + Ant Design 页面、TypeScript 类型定义、API 接口
- ✅ **单元测试生成**: JUnit 测试模板
- ✅ **批量生成**: 支持一次生成多个表
- ✅ **增量更新**: 检测表结构变化，只更新修改的表
- ✅ **代码预览**: 生成前可预览代码
- ✅ **模板管理**: 自定义模板、模板分组、导入导出

### 技术架构
```
后端: Spring Boot 3 + MyBatis Plus + FreeMarker/Velocity/Thymeleaf
前端: React 18 + Ant Design 5 + Monaco Editor
数据库: MySQL 8+
```

## 快速开始

### 1. 启动服务

```bash
cd basebackend-code-generator
mvn spring-boot:run
```

服务将在端口 `8090` 启动。

访问 Swagger UI: http://localhost:8090/swagger-ui.html

### 2. 配置数据源

#### API 方式
```bash
POST /api/generator/datasource
Content-Type: application/json

{
  "name": "开发环境数据源",
  "dbType": "MYSQL",
  "host": "localhost",
  "port": 3306,
  "databaseName": "basebackend",
  "username": "root",
  "password": "root",
  "status": 1
}
```

#### 测试连接
```bash
POST /api/generator/datasource/test
Content-Type: application/json

{
  "dbType": "MYSQL",
  "host": "localhost",
  "port": 3306,
  "databaseName": "basebackend",
  "username": "root",
  "password": "root"
}
```

### 3. 生成代码

#### 预览模式
```bash
POST /api/generator/preview
Content-Type: application/json

{
  "datasourceId": 1,
  "tableNames": ["sys_user", "sys_role"],
  "templateGroupId": 1,
  "generateType": "PREVIEW",
  "packageName": "com.basebackend.demo",
  "moduleName": "demo",
  "author": "张三",
  "tablePrefix": "sys_"
}
```

#### 下载模式
```bash
POST /api/generator/generate
Content-Type: application/json

{
  "datasourceId": 1,
  "tableNames": ["sys_user", "sys_role"],
  "templateGroupId": 1,
  "generateType": "DOWNLOAD",
  "packageName": "com.basebackend.demo",
  "moduleName": "demo",
  "author": "张三",
  "tablePrefix": "sys_"
}
```

返回 ZIP 文件，包含所有生成的代码。

## 内置模板

### 后端模板

#### 1. Entity 模板 (entity.ftl)
生成 MyBatis Plus 实体类，继承 `BaseEntity`。

**输出路径**: `src/main/java/{packagePath}/entity/{ClassName}.java`

**特性**:
- 自动继承基础字段（id、createTime、updateTime等）
- 支持 Lombok 注解
- 自动导入需要的包（LocalDateTime、BigDecimal等）

#### 2. Mapper 模板 (mapper.ftl)
生成 MyBatis Plus Mapper 接口。

**输出路径**: `src/main/java/{packagePath}/mapper/{ClassName}Mapper.java`

#### 3. Service 模板 (service.ftl + serviceImpl.ftl)
生成 Service 接口和实现类。

**输出路径**: 
- `src/main/java/{packagePath}/service/{ClassName}Service.java`
- `src/main/java/{packagePath}/service/impl/{ClassName}ServiceImpl.java`

**包含方法**:
- page: 分页查询
- getById: 根据ID查询
- create: 创建
- update: 更新
- delete: 删除
- deleteBatch: 批量删除

#### 4. Controller 模板 (controller.ftl)
生成 RESTful Controller。

**输出路径**: `src/main/java/{packagePath}/controller/{ClassName}Controller.java`

**特性**:
- Swagger 注解
- 统一返回格式 Result
- RESTful 风格 API

### 前端模板

#### 1. React 页面模板 (react-page.ftl)
生成 Ant Design CRUD 页面。

**输出路径**: `src/pages/{ModuleName}/{ClassName}/index.tsx`

**特性**:
- 完整的 CRUD 操作
- 表格展示
- 新增/编辑模态框
- 分页
- 搜索过滤

#### 2. API 接口模板 (api.ftl)
生成 TypeScript API 接口。

**输出路径**: `src/api/{variableName}Api.ts`

#### 3. 类型定义模板 (types.ftl)
生成 TypeScript 类型定义。

**输出路径**: `src/types/{variableName}.ts`

## 自定义模板

### 创建自定义模板

1. **创建模板分组**
```bash
POST /api/generator/template/group
{
  "name": "我的模板组",
  "code": "my_templates",
  "engineType": "FREEMARKER",
  "description": "自定义模板组"
}
```

2. **添加模板**
```bash
POST /api/generator/template
{
  "groupId": 2,
  "name": "自定义实体类",
  "code": "entity",
  "templateContent": "...",
  "outputPath": "src/main/java/${packagePath}/entity/${className}.java",
  "fileSuffix": ".java",
  "enabled": 1
}
```

### 模板变量

模板中可用的变量：

| 变量名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| packageName | String | 包名 | com.basebackend.demo |
| moduleName | String | 模块名 | demo |
| author | String | 作者 | 张三 |
| date | String | 日期 | 2025-10-24 |
| tableName | String | 表名 | sys_user |
| tableComment | String | 表注释 | 系统用户 |
| className | String | 类名 | SysUser |
| variableName | String | 变量名 | sysUser |
| urlPath | String | URL路径 | sys-user |
| columns | List | 字段列表 | - |
| primaryKey | Object | 主键字段 | - |
| hasDateTime | Boolean | 是否有日期时间类型 | true |
| hasBigDecimal | Boolean | 是否有BigDecimal类型 | false |

### 字段对象 (Column)

| 属性名 | 类型 | 说明 |
|--------|------|------|
| columnName | String | 数据库字段名 |
| columnType | String | 数据库字段类型 |
| columnComment | String | 字段注释 |
| javaField | String | Java字段名 |
| javaType | String | Java类型 |
| tsType | String | TypeScript类型 |
| isPrimaryKey | Boolean | 是否主键 |
| isSystemField | Boolean | 是否系统字段 |
| nullable | Boolean | 是否可为空 |
| maxLength | Integer | 最大长度 |

### FreeMarker 模板示例

```freemarker
package ${packageName}.entity;

import lombok.Data;
<#if hasDateTime>
import java.time.LocalDateTime;
</#if>

/**
 * ${tableComment}
 * 
 * @author ${author}
 * @date ${date}
 */
@Data
public class ${className} {

<#list columns as column>
    /** ${column.columnComment} */
    private ${column.javaType} ${column.javaField};

</#list>
}
```

## 类型映射

系统内置了常用的数据库类型到 Java/TypeScript 类型的映射。

### MySQL 类型映射

| 数据库类型 | Java类型 | TypeScript类型 | 导入包 |
|-----------|---------|---------------|--------|
| bigint | Long | number | - |
| int | Integer | number | - |
| varchar | String | string | - |
| text | String | string | - |
| datetime | LocalDateTime | string | java.time.LocalDateTime |
| date | LocalDate | string | java.time.LocalDate |
| decimal | BigDecimal | number | java.math.BigDecimal |
| bit | Boolean | boolean | - |

可以通过配置表 `gen_type_mapping` 自定义类型映射。

## 命名规则

### 表名到类名
- 表名: `sys_user` → 类名: `SysUser`
- 表名: `order_info` → 类名: `OrderInfo`

### 表名到变量名
- 表名: `sys_user` → 变量名: `sysUser`
- 表名: `order_info` → 变量名: `orderInfo`

### 表名到URL路径
- 表名: `sys_user` → URL: `sys-user`
- 表名: `order_info` → URL: `order-info`

### 列名到字段名
- 列名: `user_name` → 字段: `userName`
- 列名: `create_time` → 字段: `createTime`

### 表前缀处理
如果配置了表前缀（如 `sys_`），生成时会自动去除：
- 表名: `sys_user`，前缀: `sys_` → 类名: `User`
- 表名: `sys_role`，前缀: `sys_` → 类名: `Role`

## 高级功能

### 批量生成

一次性生成多个表的代码：

```json
{
  "tableNames": [
    "sys_user",
    "sys_role",
    "sys_permission",
    "sys_menu",
    "sys_dept"
  ]
}
```

### 增量更新

系统会记录每次生成的历史，可以检测表结构变化：

```bash
POST /api/generator/increment
{
  "projectId": 1
}
```

### 生成历史查询

```bash
GET /api/generator/history?current=1&size=10
```

## 配置说明

### application.yml

```yaml
generator:
  # 默认作者
  default-author: ${USER_NAME:System}
  
  # 默认模板引擎
  default-engine: FREEMARKER
  
  # 临时文件目录
  temp-dir: /tmp/generator
```

## 最佳实践

### 1. 数据库设计规范
- 表名使用小写字母和下划线
- 表名建议添加模块前缀，如 `sys_`、`order_`
- 每个表都应有主键 `id`
- 添加审计字段：`create_time`、`update_time`、`create_by`、`update_by`
- 添加逻辑删除字段：`deleted`
- 字段名使用小写字母和下划线
- 为表和字段添加清晰的注释

### 2. 命名规范
- 包名全小写：`com.basebackend.demo`
- 模块名小写：`demo`、`system`、`order`
- 表前缀统一：同一模块的表使用相同前缀

### 3. 模板使用
- 优先使用内置模板，保持代码风格统一
- 自定义模板前先预览效果
- 定期备份自定义模板

### 4. 代码审查
- 生成代码后先预览再下载
- 检查生成的代码是否符合项目规范
- 必要时手动调整生成的代码

## 故障排查

### 常见问题

#### 1. 数据源连接失败
- 检查数据库地址、端口、用户名、密码是否正确
- 确认数据库服务正在运行
- 检查防火墙设置

#### 2. 生成失败
- 检查表是否存在
- 确认模板语法正确
- 查看日志文件获取详细错误信息

#### 3. 类型映射错误
- 检查 `gen_type_mapping` 表是否有对应的类型映射
- 自定义类型映射

## 扩展开发

### 添加新的数据库支持

1. 实现 `DatabaseMetadataReader` 接口
2. 在 `DatabaseType` 枚举中添加新数据库
3. 添加类型映射到 `gen_type_mapping` 表
4. 配置数据库驱动依赖

### 添加新的模板引擎

1. 实现 `TemplateEngine` 接口
2. 在 `EngineType` 枚举中添加新引擎
3. 注册为 Spring Bean

## 联系支持

- 文档: 参考项目 README
- Issues: 提交 GitHub Issue
- 邮件: support@basebackend.com

## 更新日志

### v1.0.0 (2025-10-24)
- ✅ 初始版本发布
- ✅ 支持 MySQL 数据库
- ✅ 支持 FreeMarker 模板引擎
- ✅ 内置后端代码模板
- ✅ 内置前端 React 模板
- ✅ 支持批量生成
- ✅ 支持代码预览
- ✅ 支持下载 ZIP

### 计划中的功能
- [ ] 支持 PostgreSQL 和 Oracle
- [ ] 支持 Velocity 和 Thymeleaf 引擎
- [ ] 前端页面可视化配置
- [ ] 模板市场
- [ ] 在线模板编辑器
- [ ] 增量更新功能
- [ ] 代码差异对比
