# Mapper Bean 冲突修复报告

## 问题描述

启动应用时遇到 MyBatis Mapper Bean 定义冲突异常：

```
org.springframework.context.annotation.ConflictingBeanDefinitionException:
Annotation-specified bean name 'auditLogMapper' for bean class
[com.basebackend.database.audit.mapper.AuditLogMapper] conflicts with existing,
non-compatible bean definition of same name and class
[org.mybatis.spring.mapper.MapperFactoryBean]
```

## 根因分析

### 冲突的两个 Mapper

1. **通用数据库审计 Mapper** (`basebackend-database`)
   ```java
   package com.basebackend.database.audit.mapper;
   @Mapper
   public interface AuditLogMapper extends BaseMapper<AuditLog> {
       // 批量插入审计日志方法
   }
   ```

2. **Camunda 工作流审计 Mapper** (`scheduler-integration`)
   ```java
   package com.basebackend.scheduler.camunda.mapper;
   @Mapper
   public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
       // Camunda 专用审计日志
   }
   ```

### 冲突机制

- **MyBatis MapperScanner**：默认扫描所有 `@Mapper` 注解的接口
- **默认 Bean 命名**：使用接口名首字母小写，即 `auditLogMapper`
- **冲突结果**：两个不同的 Mapper 尝试注册同一个 Bean 名称

## 解决方案

### 1. 指定 Camunda Mapper 唯一 Bean 名称

为 Camunda 的 `AuditLogMapper` 添加 `@Repository` 注解指定唯一名称：

```java
@Mapper
@Repository("camundaAuditLogMapper")
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
}
```

### 2. 注解说明

- `@Mapper`：MyBatis 扫描标识
- `@Repository("camundaAuditLogMapper")`：Spring Bean 名称，避免与通用审计 Mapper 冲突

## 修复文件

| 文件路径 | 修改内容 |
|----------|----------|
| `scheduler-integration/src/main/java/com/basebackend/scheduler/camunda/mapper/AuditLogMapper.java` | 添加 `@Repository("camundaAuditLogMapper")` |

## 验证结果

✅ **编译成功**：所有模块编译无错误
✅ **构建成功**：JAR 包构建完成
✅ **Mapper 冲突解决**：MyBatis 可以正确区分两个不同的审计 Mapper

## 其他发现

通过扫描发现项目中存在多个潜在的 Mapper 重复名称：

| Mapper 名称 | 重复次数 | 状态 |
|-------------|----------|------|
| AuditLogMapper | 2 | ✅ 已修复 |
| FormTemplateMapper | 2-3 | ⚠️ 备份目录存在重复 |
| WorkflowInstanceMapper | 2-3 | ⚠️ 不同模块使用 |

### 建议

1. **命名规范**：建立 Mapper 命名规范，避免跨模块重复
2. **包结构优化**：考虑按领域拆分 Mapper，避免通用 Mapper 被重复实现
3. **备份清理**：清理 `scheduler-backup` 和 `scheduler-old` 目录中的重复代码

---

**修复时间**: 2025-12-16
**修复状态**: ✅ 完成
