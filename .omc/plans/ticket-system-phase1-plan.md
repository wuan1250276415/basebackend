# 企业工单系统 (Ticket System) — 一期实施计划

> 零决策可执行计划：所有架构决策已在 spec-research 阶段固化，实施阶段仅需机械执行。

## 元信息

| 项 | 值 |
|---|---|
| 分支 | `refactor/java25-springboot4` |
| 技术栈 | Java 25 / Spring Boot 4.0.3 / Spring Cloud 2025.1.1 |
| 前端 | React 18 / TypeScript / Ant Design 5 / Zustand / i18next |
| 多租户 | SHARED_DB 行级隔离 (`tenant_id` 列) |
| 审批流 | Camunda BPM 7.23.0 标准多级审批 |
| 数据库 | MySQL 8, Flyway 迁移 |
| 一期目标 | 7 个平台能力展示：多租户 + 幂等/锁 + 工作流 + 事件/消息 + 缓存 + 文件 + 限流 |

---

## 任务总览（依赖拓扑序）

```
T01 ─┐
T02 ─┤
T03 ─┴─→ T04 ─→ T05 ─→ T06 ─→ T07 ─→ T08 ─→ T09 ─→ T10 ─→ T11
                                              ↗
T12 (前端, 与 T06 并行启动) ─→ T13 ─→ T14 ─→ T15 ─→ T16
```

| ID | 任务 | 依赖 | 文件所属 |
|----|------|------|---------|
| T01 | 创建模块骨架 + pom.xml | 无 | basebackend-ticket-api/ |
| T02 | Flyway 数据库迁移脚本 | 无 | ticket-api/resources/db/ |
| T03 | 共享 DTO + Service Client | 无 | api-model/ + service-client/ |
| T04 | Entity + Mapper 层 | T01, T02 | ticket-api/entity/, mapper/ |
| T05 | Service 接口 + 实现（CRUD 核心） | T04 | ticket-api/service/ |
| T06 | Controller 层 + Swagger 文档 | T05 | ticket-api/controller/ |
| T07 | 平台能力集成（锁/幂等/缓存/限流/数据权限/审计） | T06 | ticket-api/ 各层注解 |
| T08 | 领域事件 + RocketMQ 消息集成 | T07 | ticket-api/event/, consumer/ |
| T09 | Camunda 工作流集成（BPMN + 审批 API） | T08 | scheduler-camunda/ + ticket-api/ |
| T10 | Gateway 路由 + Nacos 配置 | T06 | gateway/, config/nacos/ |
| T11 | 后端单元测试 | T09 | ticket-api/src/test/ |
| T12 | 前端页面骨架 + 路由 + API 层 | T03 | admin-web/ |
| T13 | 工单列表 + 创建页面 | T12 | admin-web/pages/ticket/ |
| T14 | 工单详情 + 评论 + 附件页面 | T13 | admin-web/pages/ticket/ |
| T15 | 工单审批页面 | T14 | admin-web/pages/ticket/ |
| T16 | 前端 i18n + 菜单 SQL | T15 | admin-web/i18n/, db/migration/ |

---

## T01: 创建模块骨架 + pom.xml

### 产出物

```
basebackend-ticket-api/
  pom.xml
  src/main/java/com/basebackend/ticket/
    TicketApiApplication.java
    config/
      SwaggerConfig.java
      TicketApiNativeHints.java
  src/main/resources/
    application.yml
    logback-structured.xml
  Dockerfile
```

### pom.xml 依赖清单（精确版本由 parent BOM 管理）

```xml
<parent>
  <groupId>com.basebackend</groupId>
  <artifactId>basebackend-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</parent>

<artifactId>basebackend-ticket-api</artifactId>
<name>BaseBackend Ticket API</name>
<description>工单微服务 - 工单管理、审批流、SLA 监控</description>

<dependencies>
  <!-- === 基础 === -->
  basebackend-common-starter
  database-core
  basebackend-security
  basebackend-api-model
  basebackend-service-client
  basebackend-nacos
  basebackend-logging-core
  observability-metrics
  <!-- === 平台能力 === -->
  basebackend-cache-core             <!-- 多级缓存 -->
  basebackend-messaging              <!-- RocketMQ -->
  basebackend-common-lock            <!-- 分布式锁 -->
  basebackend-common-idempotent      <!-- 幂等 -->
  basebackend-common-datascope       <!-- 数据权限 -->
  basebackend-common-ratelimit       <!-- 限流 -->
  basebackend-common-event           <!-- 领域事件 -->
  database-multitenant               <!-- 多租户 -->
  <!-- === Web === -->
  spring-boot-starter-web
  spring-boot-starter-validation
  spring-boot-starter-actuator
  knife4j-openapi3-jakarta-spring-boot-starter
  <!-- === Runtime === -->
  mysql-connector-j (runtime)
  <!-- === Test === -->
  spring-boot-starter-test (test)
</dependencies>
```

### TicketApiApplication.java

```java
@SpringBootApplication(scanBasePackages = {
    "com.basebackend.ticket",
    "com.basebackend.common",
    "com.basebackend.security"
})
@MapperScan({"com.basebackend.ticket.mapper"})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@ImportRuntimeHints(TicketApiNativeHints.class)
public class TicketApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketApiApplication.class, args);
    }
}
```

### application.yml 关键配置

```yaml
server:
  port: 8085

spring:
  application:
    name: basebackend-ticket-api
  threads:
    virtual:
      enabled: true
  config:
    import:
      - optional:nacos:common-config.yml
      - optional:nacos:mysql-config.yml
      - optional:nacos:redis-config.yml
      - optional:nacos:rocketmq-config.yml
      - optional:nacos:security-config.yml
      - optional:nacos:${spring.application.name}.yml
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml

# 多租户
database:
  enhanced:
    multi-tenancy:
      enabled: true
      isolation-mode: SHARED_DB
      tenant-column: tenant_id
      excluded-tables:
        - domain_event

# 领域事件
basebackend:
  common:
    event:
      enabled: true
      store:
        type: memory          # 一期用 InMemory，二期切 JDBC
      retry:
        enabled: true
        max-retries: 3

# 缓存
  cache:
    enabled: true
    multi-level:
      enabled: true

# 限流
    ratelimit:
      enabled: true
      type: redis
      algorithm: SLIDING_WINDOW

# 幂等
  idempotent:
    enabled: true

# 消息
messaging:
  enabled: true
  rocketmq:
    default-topic: ticket-topic
  idempotency:
    enabled: true
  dead-letter:
    enabled: true

# Knife4j
knife4j:
  enable: true
  setting:
    language: zh_cn
```

### root pom.xml 修改

在 `<modules>` 中添加：
```xml
<module>basebackend-ticket-api</module>
```

### Dockerfile

复制 `basebackend-user-api/Dockerfile`，替换 `-pl basebackend-ticket-api`，端口 8085。

---

## T02: Flyway 数据库迁移脚本

### 文件: `src/main/resources/db/migration/V1.0__ticket_system_init.sql`

```sql
-- =============================================
-- 工单系统初始化脚本
-- =============================================

-- 1. 工单分类表
CREATE TABLE ticket_category (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    name            VARCHAR(100)    NOT NULL COMMENT '分类名称',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父分类ID(0=顶级)',
    icon            VARCHAR(100)    DEFAULT '' COMMENT '图标',
    sort_order      INT             DEFAULT 0 COMMENT '排序号',
    description     VARCHAR(500)    DEFAULT '' COMMENT '描述',
    sla_hours       INT             DEFAULT 24 COMMENT '默认SLA时限(小时)',
    status          TINYINT         DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单分类';

-- 2. 工单主表
CREATE TABLE ticket (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_no       VARCHAR(32)     NOT NULL COMMENT '工单编号(唯一)',
    title           VARCHAR(200)    NOT NULL COMMENT '工单标题',
    description     TEXT            COMMENT '工单描述(富文本)',
    category_id     BIGINT          NOT NULL COMMENT '分类ID',
    priority        TINYINT         DEFAULT 2 COMMENT '优先级: 1=紧急 2=高 3=中 4=低',
    status          VARCHAR(20)     DEFAULT 'OPEN' COMMENT '状态: OPEN/IN_PROGRESS/PENDING_APPROVAL/APPROVED/REJECTED/RESOLVED/CLOSED',
    source          VARCHAR(20)     DEFAULT 'WEB' COMMENT '来源: WEB/API/EMAIL/WECHAT',
    reporter_id     BIGINT          NOT NULL COMMENT '报告人ID',
    reporter_name   VARCHAR(50)     NOT NULL COMMENT '报告人姓名',
    assignee_id     BIGINT          COMMENT '当前处理人ID',
    assignee_name   VARCHAR(50)     COMMENT '当前处理人姓名',
    dept_id         BIGINT          COMMENT '所属部门ID',
    -- SLA 相关
    sla_deadline    DATETIME        COMMENT 'SLA截止时间',
    sla_breached    TINYINT         DEFAULT 0 COMMENT 'SLA是否超时: 0=否 1=是',
    resolved_at     DATETIME        COMMENT '解决时间',
    closed_at       DATETIME        COMMENT '关闭时间',
    -- 工作流相关
    process_instance_id VARCHAR(64) COMMENT 'Camunda流程实例ID',
    process_definition_key VARCHAR(64) COMMENT '流程定义Key',
    -- 统计
    comment_count   INT             DEFAULT 0 COMMENT '评论数',
    attachment_count INT            DEFAULT 0 COMMENT '附件数',
    -- 扩展
    tags            VARCHAR(500)    DEFAULT '' COMMENT '标签(逗号分隔)',
    extra_data      JSON            COMMENT '扩展数据(JSON)',
    -- 基础字段
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_ticket_no (ticket_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_assignee (assignee_id),
    INDEX idx_reporter (reporter_id),
    INDEX idx_dept (dept_id),
    INDEX idx_sla_deadline (sla_deadline),
    INDEX idx_process_instance (process_instance_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单';

-- 3. 工单评论表
CREATE TABLE ticket_comment (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    content         TEXT            NOT NULL COMMENT '评论内容(富文本)',
    type            VARCHAR(20)     DEFAULT 'COMMENT' COMMENT '类型: COMMENT=评论 SYSTEM=系统消息 APPROVAL=审批意见',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父评论ID(0=顶级)',
    is_internal     TINYINT         DEFAULT 0 COMMENT '是否内部备注: 0=否 1=是',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    creator_name    VARCHAR(50)     DEFAULT '' COMMENT '创建人姓名',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单评论';

-- 4. 工单附件关联表
CREATE TABLE ticket_attachment (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    file_id         BIGINT          NOT NULL COMMENT '文件服务中的文件ID',
    file_name       VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    file_size       BIGINT          DEFAULT 0 COMMENT '文件大小(bytes)',
    file_type       VARCHAR(50)     DEFAULT '' COMMENT '文件MIME类型',
    file_url        VARCHAR(500)    DEFAULT '' COMMENT '文件访问URL',
    upload_by       BIGINT          DEFAULT 0 COMMENT '上传人ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单附件';

-- 5. 工单状态变更日志
CREATE TABLE ticket_status_log (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    from_status     VARCHAR(20)     NOT NULL COMMENT '原状态',
    to_status       VARCHAR(20)     NOT NULL COMMENT '新状态',
    operator_id     BIGINT          NOT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(50)     NOT NULL COMMENT '操作人姓名',
    remark          VARCHAR(500)    DEFAULT '' COMMENT '变更说明',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单状态变更日志';

-- 6. 工单审批记录表
CREATE TABLE ticket_approval (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    task_id         VARCHAR(64)     NOT NULL COMMENT 'Camunda任务ID',
    task_name       VARCHAR(100)    NOT NULL COMMENT '审批节点名称',
    approver_id     BIGINT          NOT NULL COMMENT '审批人ID',
    approver_name   VARCHAR(50)     NOT NULL COMMENT '审批人姓名',
    action          VARCHAR(20)     NOT NULL COMMENT '动作: APPROVE/REJECT/RETURN/DELEGATE/COUNTERSIGN',
    opinion         VARCHAR(1000)   DEFAULT '' COMMENT '审批意见',
    delegate_to_id  BIGINT          COMMENT '转办目标人ID',
    delegate_to_name VARCHAR(50)    COMMENT '转办目标人姓名',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单审批记录';

-- 7. 工单抄送表
CREATE TABLE ticket_cc (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    user_id         BIGINT          NOT NULL COMMENT '被抄送人ID',
    user_name       VARCHAR(50)     NOT NULL COMMENT '被抄送人姓名',
    is_read         TINYINT         DEFAULT 0 COMMENT '是否已读: 0=未读 1=已读',
    read_time       DATETIME        COMMENT '阅读时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_user (user_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单抄送';
```

---

## T03: 共享 DTO + Service Client

### basebackend-api-model 新增文件

```
src/main/java/com/basebackend/api/model/ticket/
  TicketBasicDTO.java          // id, ticketNo, title, status, priority, assigneeName, createTime
  TicketStatusChangeRequest.java  // ticketId, toStatus, remark
  TicketApprovalRequest.java   // ticketId, taskId, action, opinion, delegateToId
```

### basebackend-service-client 新增文件

```
src/main/java/com/basebackend/service/client/
  TicketServiceClient.java     // @HttpExchange("/api/ticket/tickets")
  fallback/TicketServiceClientFallback.java
```

### TicketServiceClient.java 接口

```java
@HttpExchange("/api/ticket/tickets")
public interface TicketServiceClient {
    @GetExchange("/{id}")
    @Operation(summary = "根据ID获取工单")
    Result<TicketBasicDTO> getById(@PathVariable("id") Long id);

    @GetExchange("/no/{ticketNo}")
    @Operation(summary = "根据工单号获取工单")
    Result<TicketBasicDTO> getByTicketNo(@PathVariable("ticketNo") String ticketNo);
}
```

在 `ServiceClientAutoConfiguration` 中注册 `TicketServiceClient` bean，baseUrl = `http://basebackend-ticket-api`。

---

## T04: Entity + Mapper 层

### Entity 清单（全部 extends BaseEntity）

| Entity | 表名 | 特殊注解 |
|--------|------|---------|
| `Ticket` | `ticket` | `@TableName("ticket")` |
| `TicketCategory` | `ticket_category` | `@TableName("ticket_category")` |
| `TicketComment` | `ticket_comment` | `@TableName("ticket_comment")` |
| `TicketAttachment` | `ticket_attachment` | `@TableName("ticket_attachment")` |
| `TicketStatusLog` | `ticket_status_log` | `@TableName("ticket_status_log")` |
| `TicketApproval` | `ticket_approval` | `@TableName("ticket_approval")` |
| `TicketCc` | `ticket_cc` | `@TableName("ticket_cc")` |

### Entity 模式（以 Ticket 为例）

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket")
public class Ticket extends BaseEntity {
    @TableField("tenant_id")
    private Long tenantId;

    @TableField("ticket_no")
    private String ticketNo;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("category_id")
    private Long categoryId;

    @TableField("priority")
    private Integer priority;

    @TableField("status")
    private String status;

    @TableField("source")
    private String source;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("reporter_name")
    private String reporterName;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("assignee_name")
    private String assigneeName;

    @TableField("dept_id")
    private Long deptId;

    @TableField("sla_deadline")
    private LocalDateTime slaDeadline;

    @TableField("sla_breached")
    private Integer slaBreached;

    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("process_instance_id")
    private String processInstanceId;

    @TableField("process_definition_key")
    private String processDefinitionKey;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("attachment_count")
    private Integer attachmentCount;

    @TableField("tags")
    private String tags;

    @TableField("extra_data")
    private String extraData;
}
```

### Mapper 清单

每个 Entity 对应一个 `extends BaseMapper<T>` 的接口 + 可选 XML。

```java
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
    // 自定义查询在 XML 中定义
}
```

需要 XML 的 Mapper：`TicketMapper.xml`（分页查询含 JOIN category 名称）。

### 枚举

```java
public enum TicketStatus {
    OPEN, IN_PROGRESS, PENDING_APPROVAL, APPROVED, REJECTED, RESOLVED, CLOSED
}

public enum TicketPriority {
    URGENT(1), HIGH(2), MEDIUM(3), LOW(4)
}

public enum ApprovalAction {
    APPROVE, REJECT, RETURN, DELEGATE, COUNTERSIGN
}
```

---

## T05: Service 接口 + 实现（CRUD 核心）

### Service 接口清单

| 接口 | 职责 |
|------|------|
| `TicketService` | 工单 CRUD、分页查询、状态变更 |
| `TicketCategoryService` | 分类 CRUD + 树结构 |
| `TicketCommentService` | 评论 CRUD |
| `TicketAttachmentService` | 附件关联管理（调用 FileServiceClient） |
| `TicketApprovalService` | 审批记录查询 |
| `TicketStatisticsService` | 统计数据（一期基础版） |

### TicketService 接口

```java
public interface TicketService {
    Ticket create(TicketCreateDTO dto);
    Ticket getById(Long id);
    Ticket getByTicketNo(String ticketNo);
    Page<TicketListVO> page(TicketQueryDTO query, Page<Ticket> page);
    void update(Long id, TicketUpdateDTO dto);
    void changeStatus(Long id, TicketStatus toStatus, String remark);
    void assign(Long id, Long assigneeId, String assigneeName);
    void close(Long id, String remark);
    String generateTicketNo(); // 格式: TK-yyyyMMdd-NNNN
}
```

### DTO 清单

```
dto/
  TicketCreateDTO.java     // title, description, categoryId, priority, assigneeId, tags, attachmentIds[]
  TicketUpdateDTO.java     // title, description, categoryId, priority, tags
  TicketQueryDTO.java      // status, priority, categoryId, assigneeId, keyword, dateRange
  TicketListVO.java        // 列表展示字段（含 categoryName）
  TicketDetailVO.java      // 详情（含 comments, attachments, statusLogs, approvals）
  TicketCommentDTO.java    // content, type, isInternal, parentId
  TicketCategoryDTO.java   // name, parentId, icon, sortOrder, slaHours
  TicketCategoryTreeVO.java // 含 children 的树形结构
```

### 工单编号生成逻辑

```java
@DistributedLock(key = "'ticket:no:' + T(java.time.LocalDate).now()",
                 waitTime = 5, leaseTime = 10)
public String generateTicketNo() {
    String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    String key = "ticket:no:seq:" + dateStr;
    Long seq = redisTemplate.opsForValue().increment(key);
    if (seq == 1L) {
        redisTemplate.expire(key, 2, TimeUnit.DAYS);
    }
    return String.format("TK-%s-%04d", dateStr, seq);
}
```

---

## T06: Controller 层

### Controller 清单

| Controller | 路径前缀 | 职责 |
|------------|---------|------|
| `TicketController` | `/api/ticket/tickets` | 工单 CRUD + 状态变更 |
| `TicketCategoryController` | `/api/ticket/categories` | 分类管理 |
| `TicketCommentController` | `/api/ticket/tickets/{ticketId}/comments` | 评论 CRUD |
| `TicketAttachmentController` | `/api/ticket/tickets/{ticketId}/attachments` | 附件管理 |
| `TicketApprovalController` | `/api/ticket/tickets/{ticketId}/approvals` | 审批操作 |
| `TicketStatisticsController` | `/api/ticket/statistics` | 统计数据 |

### API 端点清单

```
POST   /api/ticket/tickets              创建工单
GET    /api/ticket/tickets               分页查询工单
GET    /api/ticket/tickets/{id}          工单详情
PUT    /api/ticket/tickets/{id}          更新工单
PUT    /api/ticket/tickets/{id}/status   变更状态
PUT    /api/ticket/tickets/{id}/assign   分配处理人
PUT    /api/ticket/tickets/{id}/close    关闭工单
DELETE /api/ticket/tickets/{id}          删除工单(逻辑)

GET    /api/ticket/categories            分类列表(树)
POST   /api/ticket/categories            创建分类
PUT    /api/ticket/categories/{id}       更新分类
DELETE /api/ticket/categories/{id}       删除分类

GET    /api/ticket/tickets/{id}/comments       评论列表
POST   /api/ticket/tickets/{id}/comments       添加评论
DELETE /api/ticket/tickets/{id}/comments/{cid}  删除评论

POST   /api/ticket/tickets/{id}/attachments    关联附件
DELETE /api/ticket/tickets/{id}/attachments/{aid} 移除附件

POST   /api/ticket/tickets/{id}/approvals/submit    提交审批
POST   /api/ticket/tickets/{id}/approvals/approve   审批通过
POST   /api/ticket/tickets/{id}/approvals/reject    审批拒绝
POST   /api/ticket/tickets/{id}/approvals/return    退回
POST   /api/ticket/tickets/{id}/approvals/delegate  转办
POST   /api/ticket/tickets/{id}/approvals/cc        抄送

GET    /api/ticket/statistics/overview     统计概览
GET    /api/ticket/statistics/by-category  按分类统计
GET    /api/ticket/statistics/by-status    按状态统计
```

### Controller 模式（以创建工单为例）

```java
@RestController
@RequestMapping("/api/ticket/tickets")
@RequiredArgsConstructor
@Tag(name = "工单管理", description = "工单 CRUD 及状态管理")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "创建工单")
    @OperationLog(operation = "创建工单", businessType = "TICKET")
    @Idempotent(strategy = IdempotentStrategy.TOKEN)
    @RateLimit(limit = 30, window = 60, message = "创建工单过于频繁")
    public Result<TicketDetailVO> create(@RequestBody @Valid TicketCreateDTO dto) {
        Ticket ticket = ticketService.create(dto);
        return Result.success(convertToDetailVO(ticket));
    }

    @GetMapping
    @Operation(summary = "分页查询工单")
    @DataScope(type = DataScopeType.DEPT_AND_BELOW, deptAlias = "t", deptField = "dept_id")
    @Cacheable(key = "'ticket:list:' + #query.hashCode() + ':' + #current + ':' + #size", ttl = 60)
    public Result<Page<TicketListVO>> page(
            TicketQueryDTO query,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        Page<Ticket> page = new Page<>(current, size);
        return Result.success(ticketService.page(query, page));
    }
}
```

---

## T07: 平台能力集成注解矩阵

| 能力 | 注解/方式 | 应用位置 | 具体配置 |
|------|----------|---------|---------|
| **多租户** | `TenantInterceptor` 自动 | 全部 SQL（自动注入 `WHERE tenant_id = ?`） | `database.enhanced.multi-tenancy.enabled=true` |
| **分布式锁** | `@DistributedLock` | `generateTicketNo()`, `assign()` | `key="#ticketId"`, `waitTime=5`, `leaseTime=30` |
| **幂等** | `@Idempotent` | `create()` (TOKEN), `changeStatus()` (SPEL) | `strategy=TOKEN`, 前端需获取 token |
| **缓存** | `@Cacheable` / `@CacheEvict` | `getById()`, `page()`, `getCategories()` | `ttl=300` (详情), `ttl=60` (列表) |
| **限流** | `@RateLimit` | `create()`, `addComment()` | `limit=30, window=60` |
| **数据权限** | `@DataScope` | `page()` 列表查询 | `type=DEPT_AND_BELOW, deptAlias="t"` |
| **审计日志** | `@OperationLog` | 全部写操作 | `operation="xxx", businessType="TICKET"` |
| **权限** | `@RequiresPermission` | 各 Controller 方法 | `ticket:create`, `ticket:update`, `ticket:delete`, `ticket:approve` |

---

## T08: 领域事件 + RocketMQ 消息集成

### 领域事件定义

```java
public class TicketCreatedEvent extends DomainEvent {
    private final Long ticketId;
    private final String ticketNo;
    private final Long reporterId;
    private final Long assigneeId;
    public TicketCreatedEvent(String source, Long ticketId, String ticketNo, Long reporterId, Long assigneeId) {
        super(source);
        this.ticketId = ticketId;
        this.ticketNo = ticketNo;
        this.reporterId = reporterId;
        this.assigneeId = assigneeId;
    }
}

public class TicketStatusChangedEvent extends DomainEvent { ... }
public class TicketAssignedEvent extends DomainEvent { ... }
public class TicketApprovedEvent extends DomainEvent { ... }
```

### 事件发布（在 Service 中）

```java
@Transactional
public Ticket create(TicketCreateDTO dto) {
    Ticket ticket = buildTicket(dto);
    ticketMapper.insert(ticket);
    // 发布领域事件
    eventPublisher.publish(new TicketCreatedEvent(
        "ticket-service", ticket.getId(), ticket.getTicketNo(),
        ticket.getReporterId(), ticket.getAssigneeId()));
    return ticket;
}
```

### RocketMQ 消息消费者

```java
@Component
@RocketMQMessageListener(
    topic = "ticket-notification-topic",
    consumerGroup = "ticket-notification-consumer-group"
)
public class TicketNotificationConsumer extends BaseRocketMQConsumer<TicketNotificationMessage> {
    @Override
    protected MessageHandler<TicketNotificationMessage> getMessageHandler() {
        return msg -> {
            // 调用通知服务发送通知
            notificationServiceClient.send(msg.getPayload());
        };
    }
    @Override
    protected Class<TicketNotificationMessage> getPayloadClass() {
        return TicketNotificationMessage.class;
    }
}
```

### 事件监听器（桥接到 RocketMQ）

```java
@Component
@RequiredArgsConstructor
public class TicketEventHandler {
    private final MessageProducer messageProducer;

    @DomainEventListener
    public void onTicketCreated(TicketCreatedEvent event) {
        Message<TicketNotificationMessage> msg = Message.<TicketNotificationMessage>builder()
            .topic("ticket-notification-topic")
            .tags("CREATED")
            .payload(new TicketNotificationMessage(event))
            .build();
        messageProducer.sendAsync(msg);
    }
}
```

---

## T09: Camunda 工作流集成

### BPMN 流程定义: `ticket-approval.bpmn`

```
开始事件 → 提交工单(ServiceTask: 设置变量)
  → 一级审批(UserTask: candidateUsers = ${approver1})
    → 网关: approved?
      → yes → 需要二级审批?(ExclusiveGateway: ${needLevel2})
        → yes → 二级审批(UserTask: candidateUsers = ${approver2})
          → 网关: approved?
            → yes → 审批通过(ServiceTask: 更新状态为 APPROVED)
            → no  → 审批拒绝(ServiceTask: 更新状态为 REJECTED)
        → no  → 审批通过(ServiceTask: 更新状态为 APPROVED)
      → no(reject) → 审批拒绝(ServiceTask: 更新状态为 REJECTED)
      → return → 退回(ServiceTask: 更新状态为 OPEN)
  → 结束事件
```

### 流程变量

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `ticketId` | Long | 工单ID |
| `ticketNo` | String | 工单编号 |
| `reporterId` | Long | 报告人ID |
| `approver1` | String | 一级审批人ID |
| `approver2` | String | 二级审批人ID（可选） |
| `needLevel2` | Boolean | 是否需要二级审批 |
| `approved` | Boolean | 当前节点审批结果 |
| `action` | String | 审批动作 (APPROVE/REJECT/RETURN) |

### 工作流服务（ticket-api 侧）

```java
@Service
@RequiredArgsConstructor
public class TicketWorkflowService {
    private final ProcessInstanceServiceClient processInstanceClient;
    private final TaskServiceClient taskServiceClient;

    public void startApproval(Long ticketId, String approver1, String approver2) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("ticketId", ticketId);
        variables.put("approver1", approver1);
        variables.put("approver2", approver2);
        variables.put("needLevel2", approver2 != null);

        ProcessDefinitionStartRequest request = new ProcessDefinitionStartRequest();
        request.setProcessKey("ticket-approval");
        request.setBusinessKey("TICKET-" + ticketId);
        request.setVariables(variables);

        Result<ProcessInstanceFeignDTO> result = processInstanceClient.startByKey(request);
        // 更新工单的 processInstanceId
        ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
            .eq(Ticket::getId, ticketId)
            .set(Ticket::getProcessInstanceId, result.getData().getId())
            .set(Ticket::getStatus, TicketStatus.PENDING_APPROVAL.name()));
    }

    public void completeTask(String taskId, ApprovalAction action, String opinion) {
        TaskActionRequest request = new TaskActionRequest();
        request.setTaskId(taskId);
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", action == ApprovalAction.APPROVE);
        variables.put("action", action.name());
        request.setVariables(variables);
        taskServiceClient.complete(request);
    }
}
```

### BPMN 部署

BPMN 文件 `ticket-approval.bpmn` 放在 `scheduler-camunda/src/main/resources/bpmn/` 目录下。
通过 `ProcessDefinitionController` 的 deploy 接口部署，或 Camunda 自动扫描加载。

---

## T10: Gateway 路由 + Nacos 配置

### 修改 `basebackend-gateway/src/main/resources/application-routes.yml`

```yaml
# ==================== 工单服务路由 ====================
- id: ticket-api
  uri: lb://basebackend-ticket-api
  predicates:
    - Path=/api/ticket/**
  filters:
    - StripPrefix=1
    - name: CircuitBreaker
      args:
        name: ticketServiceCircuitBreaker
        fallbackUri: forward:/fallback/ticket
```

### Nacos 配置（可选）

创建 `basebackend-ticket-api.yml` 配置文件在 Nacos 中，包含工单服务专属配置。

---

## T11: 后端单元测试

### 测试清单

| 测试类 | 覆盖范围 |
|--------|---------|
| `TicketServiceImplTest` | CRUD、状态变更、工单号生成、SLA 计算 |
| `TicketCategoryServiceImplTest` | 分类树、CRUD |
| `TicketCommentServiceImplTest` | 评论 CRUD |
| `TicketWorkflowServiceTest` | 流程启动、任务完成 |
| `TicketEventHandlerTest` | 事件发布、消息发送 |

### 测试模式

```java
@SpringBootTest(classes = TicketApiApplication.class)
@ActiveProfiles("test")
class TicketServiceImplTest {
    @MockitoBean private TicketMapper ticketMapper;
    @MockitoBean private DomainEventPublisher eventPublisher;
    @Autowired private TicketService ticketService;

    @Test
    void create_shouldGenerateTicketNoAndPublishEvent() {
        // given ...
        // when ...
        // then: verify ticketMapper.insert called, eventPublisher.publish called
    }
}
```

---

## T12: 前端页面骨架 + 路由 + API

### 目录结构

```
basebackend-admin-web/src/
  api/
    ticketApi.ts                // 工单 API 接口
  pages/
    ticket/
      index.tsx                 // 工单列表页
      detail/
        index.tsx               // 工单详情页
      create/
        index.tsx               // 创建工单页
      approval/
        index.tsx               // 审批页面
      category/
        index.tsx               // 分类管理页
  stores/
    ticketStore.ts              // 工单状态管理
  components/
    ticket/
      TicketStatusTag.tsx       // 状态标签
      PriorityBadge.tsx         // 优先级徽章
      CommentSection.tsx        // 评论区
      AttachmentList.tsx        // 附件列表
      ApprovalTimeline.tsx      // 审批时间线
      StatusLog.tsx             // 状态变更日志
  i18n/locales/
    zh-CN/ticket.json           // 中文
    en-US/ticket.json           // 英文
```

### 路由注册

前端使用**动态路由**机制（`dynamicRoutes.ts` 通过 `import.meta.glob('../pages/**/index.tsx')` 自动扫描）。只需：

1. 在 `src/pages/ticket/index.tsx` 创建页面组件
2. 在数据库 `sys_application_resource` 表中插入菜单数据（见 T16）
3. 前端自动匹配 path → 页面组件

### API 层: `src/api/ticketApi.ts`

```typescript
import request from '@/utils/request';

export interface TicketCreateDTO {
  title: string;
  description: string;
  categoryId: number;
  priority: number;
  assigneeId?: number;
  tags?: string;
  attachmentIds?: number[];
}

export interface TicketQueryDTO {
  status?: string;
  priority?: number;
  categoryId?: number;
  assigneeId?: number;
  keyword?: string;
  startDate?: string;
  endDate?: string;
}

export const ticketApi = {
  create: (data: TicketCreateDTO) =>
    request.post('/api/ticket/tickets', data, {
      headers: { 'X-Idempotent-Token': '' }, // 从 idempotent token 接口获取
    }),
  page: (params: TicketQueryDTO & { current: number; size: number }) =>
    request.get('/api/ticket/tickets', { params }),
  getById: (id: number) =>
    request.get(`/api/ticket/tickets/${id}`),
  update: (id: number, data: Partial<TicketCreateDTO>) =>
    request.put(`/api/ticket/tickets/${id}`, data),
  changeStatus: (id: number, data: { status: string; remark?: string }) =>
    request.put(`/api/ticket/tickets/${id}/status`, data),
  assign: (id: number, data: { assigneeId: number }) =>
    request.put(`/api/ticket/tickets/${id}/assign`, data),
  close: (id: number, remark?: string) =>
    request.put(`/api/ticket/tickets/${id}/close`, { remark }),
  delete: (id: number) =>
    request.delete(`/api/ticket/tickets/${id}`),

  // 评论
  getComments: (ticketId: number) =>
    request.get(`/api/ticket/tickets/${ticketId}/comments`),
  addComment: (ticketId: number, data: { content: string; isInternal?: boolean }) =>
    request.post(`/api/ticket/tickets/${ticketId}/comments`, data),

  // 附件
  addAttachment: (ticketId: number, data: { fileId: number; fileName: string; fileSize: number }) =>
    request.post(`/api/ticket/tickets/${ticketId}/attachments`, data),

  // 审批
  submitApproval: (ticketId: number, data: { approver1: string; approver2?: string }) =>
    request.post(`/api/ticket/tickets/${ticketId}/approvals/submit`, data),
  approve: (ticketId: number, data: { taskId: string; opinion?: string }) =>
    request.post(`/api/ticket/tickets/${ticketId}/approvals/approve`, data),
  reject: (ticketId: number, data: { taskId: string; opinion: string }) =>
    request.post(`/api/ticket/tickets/${ticketId}/approvals/reject`, data),

  // 分类
  getCategories: () =>
    request.get('/api/ticket/categories'),
  createCategory: (data: { name: string; parentId?: number; slaHours?: number }) =>
    request.post('/api/ticket/categories', data),

  // 统计
  getOverview: () =>
    request.get('/api/ticket/statistics/overview'),

  // 幂等 Token
  getIdempotentToken: () =>
    request.get('/api/idempotent/token'),
};
```

### Zustand Store: `src/stores/ticketStore.ts`

```typescript
import { create } from 'zustand';

interface TicketState {
  currentTicket: TicketDetail | null;
  listParams: TicketQueryDTO;
  setCurrentTicket: (ticket: TicketDetail | null) => void;
  setListParams: (params: Partial<TicketQueryDTO>) => void;
  resetListParams: () => void;
}

export const useTicketStore = create<TicketState>((set) => ({
  currentTicket: null,
  listParams: {},
  setCurrentTicket: (ticket) => set({ currentTicket: ticket }),
  setListParams: (params) => set((s) => ({ listParams: { ...s.listParams, ...params } })),
  resetListParams: () => set({ listParams: {} }),
}));
```

---

## T13-T15: 前端页面实现

### T13: 工单列表 + 创建页面

- **列表页** (`pages/ticket/index.tsx`): ProTable + 筛选栏（状态/优先级/分类/关键词/日期）+ 批量操作
- **创建页** (`pages/ticket/create/index.tsx`): Steps 表单（基本信息 → 描述附件 → 确认提交），提交前获取 idempotent token

### T14: 工单详情 + 评论 + 附件

- **详情页** (`pages/ticket/detail/index.tsx`): Descriptions 展示工单信息 + Tabs（评论/附件/状态日志/审批记录）
- **CommentSection 组件**: 评论列表 + 富文本输入 + 内部备注标记
- **AttachmentList 组件**: 文件列表 + Upload 组件（调用 file-service）

### T15: 审批页面

- **审批页** (`pages/ticket/approval/index.tsx`): 待审批列表 + 审批操作（通过/拒绝/退回/转办）
- **ApprovalTimeline 组件**: 展示 BPMN 流程进度 + 审批意见时间线

---

## T16: i18n + 菜单 SQL

### 菜单 SQL（追加到 Flyway）

文件: `V1.1__ticket_menu_init.sql`

```sql
-- 工单管理主菜单 (type=0 目录)
INSERT INTO sys_application_resource (id, app_id, parent_id, permission_name, permission_key, path, type, order_num, status, icon)
VALUES (3000, 1, 0, '工单管理', 'ticket', '/ticket', 0, 6, 1, 'solution');

-- 工单列表 (type=1 菜单)
INSERT INTO sys_application_resource (id, app_id, parent_id, permission_name, permission_key, path, type, order_num, status, icon)
VALUES (3001, 1, 3000, '工单列表', 'ticket:list', '/ticket', 1, 1, 1, 'unordered-list');

-- 创建工单 (type=2 按钮权限)
INSERT INTO sys_application_resource (id, app_id, parent_id, permission_name, permission_key, path, type, order_num, status)
VALUES (3002, 1, 3001, '创建工单', 'ticket:create', '', 2, 1, 1);

-- 工单详情 (type=1 菜单, 隐藏)
INSERT INTO sys_application_resource (id, app_id, parent_id, permission_name, permission_key, path, type, order_num, status, visible)
VALUES (3003, 1, 3000, '工单详情', 'ticket:detail', '/ticket/detail', 1, 2, 1, 0);

-- 待审批 (type=1 菜单)
INSERT INTO sys_application_resource (id, app_id, parent_id, permission_name, permission_key, path, type, order_num, status, icon)
VALUES (3004, 1, 3000, '待审批', 'ticket:approval', '/ticket/approval', 1, 3, 1, 'audit');

-- 分类管理 (type=1 菜单)
INSERT INTO sys_application_resource (id, app_id, parent_id, permission_name, permission_key, path, type, order_num, status, icon)
VALUES (3005, 1, 3000, '分类管理', 'ticket:category', '/ticket/category', 1, 4, 1, 'appstore');

-- 审批权限 (type=2 按钮)
INSERT INTO sys_application_resource (id, app_id, parent_id, permission_name, permission_key, path, type, order_num, status)
VALUES (3006, 1, 3004, '审批操作', 'ticket:approve', '', 2, 1, 1);

-- 删除工单 (type=2 按钮)
INSERT INTO sys_application_resource (id, app_id, parent_id, permission_name, permission_key, path, type, order_num, status)
VALUES (3007, 1, 3001, '删除工单', 'ticket:delete', '', 2, 2, 1);
```

### i18n Keys: `zh-CN/ticket.json`

```json
{
  "ticket": {
    "title": "工单管理",
    "list": "工单列表",
    "create": "创建工单",
    "detail": "工单详情",
    "approval": "待审批",
    "category": "分类管理",
    "field": {
      "ticketNo": "工单编号",
      "title": "标题",
      "description": "描述",
      "status": "状态",
      "priority": "优先级",
      "category": "分类",
      "reporter": "报告人",
      "assignee": "处理人",
      "slaDeadline": "SLA截止",
      "createTime": "创建时间"
    },
    "status": {
      "OPEN": "待处理",
      "IN_PROGRESS": "处理中",
      "PENDING_APPROVAL": "审批中",
      "APPROVED": "已通过",
      "REJECTED": "已拒绝",
      "RESOLVED": "已解决",
      "CLOSED": "已关闭"
    },
    "priority": {
      "1": "紧急",
      "2": "高",
      "3": "中",
      "4": "低"
    },
    "action": {
      "approve": "通过",
      "reject": "拒绝",
      "return": "退回",
      "delegate": "转办",
      "countersign": "加签",
      "cc": "抄送",
      "close": "关闭",
      "assign": "分配"
    }
  }
}
```

---

## PBT 属性（Property-Based Testing）

| # | 属性 | 不变量 | 伪造策略 |
|---|------|--------|---------|
| P1 | **工单号唯一性** | `∀ t1, t2 ∈ Ticket: t1.id ≠ t2.id → t1.ticketNo ≠ t2.ticketNo` | 并发创建 100 个工单，断言无重复 ticketNo |
| P2 | **租户隔离** | `∀ query(tenantId=A): result ∩ {t ∈ Ticket: t.tenantId ≠ A} = ∅` | 创建两个租户的数据，查询时断言无交叉 |
| P3 | **幂等性** | `create(dto, token) × N → Ticket.count += 1` | 相同 token 重复提交 5 次，断言只创建 1 张工单 |
| P4 | **状态机合法性** | `transition(OPEN → CLOSED) = ✗`, `transition(OPEN → IN_PROGRESS) = ✓` | 枚举所有非法状态转换，断言全部拒绝 |
| P5 | **审批完整性** | `process_complete → approval_records.count ≥ 1` | 流程结束后断言至少存在 1 条审批记录 |
| P6 | **缓存一致性** | `update(ticket) → cache.get(ticket.id).version = db.get(ticket.id).version` | 更新后立即读取，断言缓存与 DB 一致 |
| P7 | **数据权限** | `user(deptId=X) query → ∀ t ∈ result: t.deptId ∈ subtree(X)` | 跨部门查询，断言结果仅含本部门及下属 |

---

## 验收标准（一期完成定义）

- [ ] `mvn clean install -DskipTests` 全仓构建通过（含 ticket-api 模块）
- [ ] Knife4j `/doc.html` 可访问全部工单 API
- [ ] 切换 `X-Tenant-Id` 验证租户隔离
- [ ] 重复提交返回 1022 错误码
- [ ] 工单创建后 RocketMQ 消费日志可见
- [ ] BPMN 审批流跑通（提交→一级→二级→完成）
- [ ] 工单列表缓存命中 < 50ms
- [ ] 前端工单页面可正常 CRUD 操作
- [ ] Service 层单元测试覆盖率 > 60%
