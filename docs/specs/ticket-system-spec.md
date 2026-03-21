# 企业工单系统 (Ticket System) - 技术规格文档
> BearTeam 项目 | 基于 BaseBackend 平台 | 全能力展示

## 1. 项目概述

### 1.1 产品定位
企业级智能工单系统，覆盖工单全生命周期管理：创建→分配→处理→审批→关闭。
**核心目标：一个项目展示 basebackend 平台 10+ 个基础能力。**

### 1.2 平台能力覆盖

| 平台模块 | 展示场景 |
|----------|---------|
| 多租户 (database-multitenant) | 多企业/部门隔离 |
| 数据权限 (common-datascope) | 工单只看本部门/本人 |
| 数据脱敏 (database-security) | 客户手机号/身份证脱敏 |
| 分布式锁 (common-lock) | 工单抢单防并发 |
| 幂等 (common-idempotent) | 防止重复提交工单 |
| 消息/事件 (messaging) | 工单状态变更通知 |
| WebSocket (websocket) | 实时推送工单动态 |
| 搜索 (search) | 工单全文搜索 |
| AI (ai) | 智能分类/自动回复建议 |
| 文件服务 (file-service) | 工单附件上传 |
| 缓存 (cache) | 热门工单/统计面板缓存 |
| 定时任务 (scheduler) | SLA 超时检查/自动升级 |
| 导出 (common-export) | 工单数据 Excel 导出 |
| 审计日志 (logging-audit) | 操作记录追踪 |
| 限流 (common-ratelimit) | API 限流防刷 |

### 1.3 核心功能
- 📝 工单 CRUD（创建、编辑、关闭、重开）
- 🎯 智能分配（轮询/负载均衡/手动/抢单）
- 📊 SLA 管理（响应时间/解决时间/超时升级）
- 💬 工单评论与内部备注
- 📎 附件上传
- 🔔 实时通知（WebSocket + 站内信）
- 🔍 全文搜索（标题/描述/评论）
- 🤖 AI 辅助（自动分类、回复建议、相似工单推荐）
- 📈 统计看板（今日工单、响应率、满意度）
- 🏷️ 标签/分类管理
- ⭐ 客户满意度评价
- 📥 数据导出

### 1.4 技术栈
- **后端**: Java 25 + Spring Boot 4.0.3 + MyBatis-Plus 3.5.16
- **前端**: React 18 + TypeScript + Ant Design Pro 5 + Zustand
- **存储**: MySQL + Redis + Elasticsearch（可选）

---

## 2. 系统架构

### 2.1 模块结构
```
basebackend-ticket-api/              # 后端微服务
├── src/main/java/com/basebackend/ticket/
│   ├── TicketApiApplication.java
│   ├── config/
│   │   ├── TicketAutoConfiguration.java
│   │   └── AiConfig.java              # AI 模块配置
│   ├── controller/
│   │   ├── TicketController.java       # 工单 CRUD + 分配 + 抢单
│   │   ├── CommentController.java      # 评论/内部备注
│   │   ├── CategoryController.java     # 分类管理
│   │   ├── SlaController.java          # SLA 策略管理
│   │   ├── DashboardController.java    # 统计看板
│   │   ├── AiController.java           # AI 辅助接口
│   │   └── CustomerController.java     # 客户管理
│   ├── entity/
│   │   ├── Ticket.java                 # 工单
│   │   ├── TicketComment.java          # 工单评论
│   │   ├── TicketAttachment.java       # 工单附件
│   │   ├── TicketCategory.java         # 工单分类
│   │   ├── TicketTag.java              # 工单标签
│   │   ├── TicketTagRelation.java      # 工单-标签关联
│   │   ├── SlaPolicy.java             # SLA 策略
│   │   ├── TicketLog.java             # 工单操作日志
│   │   ├── Customer.java              # 客户信息
│   │   └── Satisfaction.java          # 满意度评价
│   ├── mapper/
│   ├── service/
│   │   ├── TicketService.java
│   │   ├── AssignmentService.java      # 分配策略
│   │   ├── SlaService.java            # SLA 检查
│   │   ├── AiService.java             # AI 辅助
│   │   ├── NotifyService.java         # 通知服务
│   │   ├── DashboardService.java      # 统计
│   │   └── impl/
│   ├── dto/
│   ├── vo/
│   ├── enums/
│   │   ├── TicketStatus.java          # 待处理/处理中/待审核/已解决/已关闭/已重开
│   │   ├── TicketPriority.java        # 低/中/高/紧急
│   │   ├── AssignMode.java            # 手动/轮询/负载均衡/抢单
│   │   └── CommentType.java           # 公开回复/内部备注
│   ├── event/                         # 事件驱动
│   │   ├── TicketCreatedEvent.java
│   │   ├── TicketAssignedEvent.java
│   │   ├── TicketStatusChangedEvent.java
│   │   └── TicketEventListener.java
│   ├── scheduler/                     # 定时任务
│   │   └── SlaCheckScheduler.java     # SLA 超时检查
│   └── ai/                            # AI 集成
│       ├── TicketClassifier.java      # 工单分类
│       └── ReplySuggester.java        # 回复建议
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__init_ticket_tables.sql
└── pom.xml

basebackend-ticket-ui/               # 前端应用
├── src/
│   ├── api/                          # API 接口
│   ├── components/
│   │   ├── TicketDetail/             # 工单详情面板
│   │   ├── TicketForm/               # 创建/编辑工单表单
│   │   ├── CommentList/              # 评论列表
│   │   ├── SlaIndicator/             # SLA 倒计时指示器
│   │   └── AiSuggestion/            # AI 建议面板
│   ├── pages/
│   │   ├── Dashboard/                # 统计看板
│   │   ├── TicketList/               # 工单列表
│   │   ├── TicketDetail/             # 工单详情页
│   │   ├── MyTickets/                # 我的工单
│   │   ├── Category/                 # 分类管理
│   │   ├── SlaPolicy/               # SLA 策略
│   │   ├── Customer/                 # 客户管理
│   │   └── Login/
│   ├── stores/
│   └── types/
├── vite.config.ts
└── package.json
```

---

## 3. 数据模型

### 3.1 数据库表设计

```sql
-- =============================================
-- 企业工单系统 - 建表脚本
-- 表前缀: ticket_
-- =============================================

-- 工单分类
CREATE TABLE ticket_category (
    id BIGINT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    sla_policy_id BIGINT COMMENT '关联SLA策略ID',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_parent(parent_id),
    INDEX idx_tenant(tenant_id)
) COMMENT '工单分类';

-- 客户信息（展示数据脱敏）
CREATE TABLE ticket_customer (
    id BIGINT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '客户姓名',
    phone VARCHAR(20) COMMENT '手机号（脱敏展示）',
    email VARCHAR(200) COMMENT '邮箱（脱敏展示）',
    id_card VARCHAR(30) COMMENT '身份证号（脱敏展示）',
    company VARCHAR(200) COMMENT '公司名称',
    level TINYINT DEFAULT 0 COMMENT '客户等级: 0=普通 1=VIP 2=SVIP',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_phone(phone),
    INDEX idx_tenant(tenant_id)
) COMMENT '客户信息';

-- 工单主表
CREATE TABLE ticket_ticket (
    id BIGINT PRIMARY KEY COMMENT '主键',
    ticket_no VARCHAR(32) UNIQUE NOT NULL COMMENT '工单编号（TK202602280001）',
    title VARCHAR(500) NOT NULL COMMENT '工单标题',
    description TEXT COMMENT '工单描述',
    category_id BIGINT COMMENT '分类ID',
    customer_id BIGINT COMMENT '客户ID',
    status TINYINT DEFAULT 0 COMMENT '状态: 0=待处理 1=处理中 2=待审核 3=已解决 4=已关闭 5=已重开',
    priority TINYINT DEFAULT 1 COMMENT '优先级: 0=低 1=中 2=高 3=紧急',
    source TINYINT DEFAULT 0 COMMENT '来源: 0=Web 1=邮件 2=电话 3=API',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    assignee_id BIGINT COMMENT '处理人ID',
    dept_id BIGINT COMMENT '所属部门ID（数据权限）',
    ai_category VARCHAR(100) COMMENT 'AI自动分类结果',
    ai_confidence DECIMAL(5,2) COMMENT 'AI分类置信度',
    first_response_at DATETIME COMMENT '首次响应时间',
    resolved_at DATETIME COMMENT '解决时间',
    closed_at DATETIME COMMENT '关闭时间',
    sla_response_deadline DATETIME COMMENT 'SLA响应截止时间',
    sla_resolve_deadline DATETIME COMMENT 'SLA解决截止时间',
    sla_breached TINYINT DEFAULT 0 COMMENT 'SLA是否已超时',
    satisfaction_score TINYINT COMMENT '满意度评分(1-5)',
    satisfaction_comment VARCHAR(500) COMMENT '满意度评语',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_no(ticket_no),
    INDEX idx_status(status),
    INDEX idx_priority(priority),
    INDEX idx_assignee(assignee_id),
    INDEX idx_creator(creator_id),
    INDEX idx_dept(dept_id),
    INDEX idx_category(category_id),
    INDEX idx_customer(customer_id),
    INDEX idx_tenant(tenant_id),
    INDEX idx_sla_response(sla_response_deadline),
    INDEX idx_sla_resolve(sla_resolve_deadline),
    FULLTEXT idx_search(title, description)
) COMMENT '工单';

-- 工单评论
CREATE TABLE ticket_comment (
    id BIGINT PRIMARY KEY COMMENT '主键',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    user_id BIGINT NOT NULL COMMENT '评论者ID',
    content TEXT NOT NULL COMMENT '评论内容',
    type TINYINT DEFAULT 0 COMMENT '类型: 0=公开回复 1=内部备注',
    is_ai_generated TINYINT DEFAULT 0 COMMENT '是否AI生成',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_ticket(ticket_id)
) COMMENT '工单评论';

-- 工单附件
CREATE TABLE ticket_attachment (
    id BIGINT PRIMARY KEY COMMENT '主键',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    comment_id BIGINT COMMENT '关联评论ID',
    file_name VARCHAR(500) NOT NULL COMMENT '文件名',
    file_path VARCHAR(1000) NOT NULL COMMENT '存储路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    uploader_id BIGINT NOT NULL COMMENT '上传者ID',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_ticket(ticket_id)
) COMMENT '工单附件';

-- 工单标签
CREATE TABLE ticket_tag (
    id BIGINT PRIMARY KEY COMMENT '主键',
    name VARCHAR(50) NOT NULL COMMENT '标签名',
    color VARCHAR(20) DEFAULT '#1677ff' COMMENT '标签颜色',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name_tenant(name, tenant_id)
) COMMENT '工单标签';

-- 工单-标签关联
CREATE TABLE ticket_tag_relation (
    id BIGINT PRIMARY KEY COMMENT '主键',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    UNIQUE KEY uk_ticket_tag(ticket_id, tag_id)
) COMMENT '工单标签关联';

-- SLA 策略
CREATE TABLE ticket_sla_policy (
    id BIGINT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '策略名称',
    description VARCHAR(500) COMMENT '描述',
    priority TINYINT NOT NULL COMMENT '适用优先级',
    response_hours INT NOT NULL COMMENT '响应时间(小时)',
    resolve_hours INT NOT NULL COMMENT '解决时间(小时)',
    escalation_enabled TINYINT DEFAULT 0 COMMENT '是否启用升级',
    escalation_user_id BIGINT COMMENT '超时升级通知人',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_priority(priority),
    INDEX idx_tenant(tenant_id)
) COMMENT 'SLA策略';

-- 工单操作日志（展示审计日志）
CREATE TABLE ticket_log (
    id BIGINT PRIMARY KEY COMMENT '主键',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    user_id BIGINT COMMENT '操作者ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型(CREATE/ASSIGN/STATUS_CHANGE/COMMENT/CLOSE等)',
    old_value VARCHAR(500) COMMENT '变更前值',
    new_value VARCHAR(500) COMMENT '变更后值',
    remark VARCHAR(500) COMMENT '备注',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ticket(ticket_id)
) COMMENT '工单操作日志';

-- 满意度评价
CREATE TABLE ticket_satisfaction (
    id BIGINT PRIMARY KEY COMMENT '主键',
    ticket_id BIGINT UNIQUE NOT NULL COMMENT '工单ID',
    score TINYINT NOT NULL COMMENT '评分(1-5)',
    comment VARCHAR(500) COMMENT '评语',
    user_id BIGINT NOT NULL COMMENT '评价者ID',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ticket(ticket_id)
) COMMENT '满意度评价';
```

---

## 4. REST API 设计

### 4.1 工单 `/api/ticket/tickets`
| 方法 | 路径 | 功能 | 展示能力 |
|------|------|------|---------|
| POST | `/api/ticket/tickets` | 创建工单 | 幂等、事件驱动、AI分类 |
| GET | `/api/ticket/tickets` | 工单列表 | 多租户、数据权限、分页 |
| GET | `/api/ticket/tickets/{id}` | 工单详情 | 缓存 |
| PUT | `/api/ticket/tickets/{id}` | 编辑工单 | 审计日志 |
| PUT | `/api/ticket/tickets/{id}/status` | 变更状态 | 事件驱动、WebSocket |
| PUT | `/api/ticket/tickets/{id}/assign` | 分配工单 | 分布式锁 |
| POST | `/api/ticket/tickets/{id}/grab` | 抢单 | 分布式锁、幂等 |
| PUT | `/api/ticket/tickets/{id}/priority` | 修改优先级 | SLA重算 |
| GET | `/api/ticket/tickets/search` | 全文搜索 | 搜索引擎 |
| GET | `/api/ticket/tickets/my` | 我的工单 | 数据权限 |
| GET | `/api/ticket/tickets/export` | 导出Excel | 导出模块 |
| POST | `/api/ticket/tickets/{id}/satisfaction` | 满意度评价 | - |

### 4.2 评论 `/api/ticket/comments`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/ticket/comments` | 发表评论/内部备注 |
| GET | `/api/ticket/comments/ticket/{ticketId}` | 评论列表 |
| DELETE | `/api/ticket/comments/{id}` | 删除评论 |

### 4.3 分类 `/api/ticket/categories`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/ticket/categories` | 创建分类 |
| GET | `/api/ticket/categories/tree` | 分类树 |
| PUT | `/api/ticket/categories/{id}` | 编辑分类 |
| DELETE | `/api/ticket/categories/{id}` | 删除分类 |

### 4.4 客户 `/api/ticket/customers`
| 方法 | 路径 | 功能 | 展示能力 |
|------|------|------|---------|
| POST | `/api/ticket/customers` | 创建客户 | - |
| GET | `/api/ticket/customers` | 客户列表 | 数据脱敏 |
| GET | `/api/ticket/customers/{id}` | 客户详情 | 数据脱敏 |

### 4.5 SLA `/api/ticket/sla`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/ticket/sla` | 创建SLA策略 |
| GET | `/api/ticket/sla` | 策略列表 |
| PUT | `/api/ticket/sla/{id}` | 编辑策略 |
| DELETE | `/api/ticket/sla/{id}` | 删除策略 |

### 4.6 看板 `/api/ticket/dashboard`
| 方法 | 路径 | 功能 | 展示能力 |
|------|------|------|---------|
| GET | `/api/ticket/dashboard/stats` | 今日统计 | 缓存 |
| GET | `/api/ticket/dashboard/trend` | 趋势图 | 缓存 |
| GET | `/api/ticket/dashboard/sla` | SLA 达标率 | 缓存 |
| GET | `/api/ticket/dashboard/satisfaction` | 满意度统计 | 缓存 |

### 4.7 AI `/api/ticket/ai`
| 方法 | 路径 | 功能 | 展示能力 |
|------|------|------|---------|
| POST | `/api/ticket/ai/classify` | AI自动分类 | AI模块 |
| POST | `/api/ticket/ai/suggest-reply` | AI回复建议 | AI模块 |
| GET | `/api/ticket/ai/similar/{id}` | 相似工单推荐 | AI+搜索 |

---

## 5. 平台能力集成详解

### 5.1 多租户 (@TenantScope)
- 所有表包含 `tenant_id` 字段
- MyBatis-Plus 拦截器自动注入租户条件
- 不同企业数据完全隔离

### 5.2 数据权限 (@DataScope)
- 工单列表根据用户角色过滤：
  - 普通员工：只看自己创建/被分配的
  - 部门经理：看本部门所有
  - 管理员：看全部
- 通过 `dept_id` + DataScopeInterceptor 实现

### 5.3 数据脱敏 (@Sensitive)
- Customer 表的 `phone`、`email`、`id_card` 字段
- 查询时自动脱敏：`138****1234`、`abc***@qq.com`
- 使用 `@Sensitive(type = SensitiveType.PHONE)` 注解

### 5.4 分布式锁 (@DistributedLock)
- 工单抢单：`@DistributedLock(key = "'ticket:grab:' + #ticketId")`
- 防止多人同时抢同一张工单

### 5.5 幂等 (@Idempotent)
- 创建工单：`@Idempotent(key = "#dto.title + #userId", expireSeconds = 10)`
- 防止重复提交

### 5.6 事件驱动
- `TicketCreatedEvent` → 自动分配 + AI分类 + WebSocket通知
- `TicketAssignedEvent` → 通知处理人
- `TicketStatusChangedEvent` → 更新SLA + 通知相关人

### 5.7 WebSocket 实时推送
- 新工单通知
- 工单状态变更
- 新评论提醒
- SLA 即将超时警告

### 5.8 SLA 定时任务
- 每分钟检查即将超时的工单
- 超时自动升级通知
- 标记 `sla_breached = 1`

### 5.9 缓存
- Dashboard 统计数据缓存 5 分钟
- 分类树缓存
- 热门标签缓存

### 5.10 AI 集成
- 工单创建时自动分类（调用 LLM API）
- 根据历史评论生成回复建议
- 基于标题相似度推荐关联工单

---

## 6. 前端页面设计

### 6.1 页面清单
| 页面 | 路径 | 描述 |
|------|------|------|
| 登录 | `/login` | 登录页 |
| 看板 | `/` | 统计看板（今日工单/SLA/满意度/趋势图） |
| 工单列表 | `/tickets` | ProTable 列表，筛选/搜索/导出 |
| 工单详情 | `/tickets/:id` | 详情+评论+附件+操作日志+AI建议 |
| 我的工单 | `/my-tickets` | 我创建的+分配给我的 |
| 分类管理 | `/categories` | 树形分类管理 |
| 客户管理 | `/customers` | 客户列表（脱敏展示） |
| SLA 策略 | `/sla` | SLA 策略管理 |
| 标签管理 | `/tags` | 标签 CRUD |

### 6.2 核心交互
- **工单列表**: ProTable + 高级筛选（状态/优先级/分类/处理人/日期）
- **工单详情**: 左侧详情+右侧评论时间线，顶部 SLA 倒计时
- **抢单按钮**: 实时显示可抢工单数，点击后乐观锁竞争
- **AI 面板**: 工单详情页侧边栏显示 AI 分类建议和回复建议
- **实时通知**: 右上角铃铛 + WebSocket 推送

### 6.3 UI 风格
- 专业商务风格
- 主色调：#1677ff（Ant Design 蓝）
- ProLayout 侧边栏布局
- 状态标签彩色区分（待处理/处理中/已解决）

---

## 7. 开发计划（6 步）

### Step 1: 后端基础（🐻‍❄️ 后端熊）
- 创建 `basebackend-ticket-api` 模块
- 实体 + Mapper + SQL 建表
- 枚举 + DTO + VO
- 编译通过

### Step 2: 后端核心（🐻‍❄️ 后端熊）
- TicketService + AssignmentService（含分布式锁/幂等）
- CommentService + CustomerService
- 全部 Controller
- 事件驱动（Event + Listener）
- 编译通过

### Step 3: 后端增强（🐻‍❄️ 后端熊）
- SLA 定时检查
- AI 分类/回复建议（对接 RestClient）
- 数据脱敏注解
- Dashboard 统计（含缓存）
- WebSocket 通知
- 搜索集成

### Step 4: 前端框架（🐼 前端熊）
- 创建 `basebackend-ticket-ui`
- 基础框架 + 路由 + Store
- 登录页 + 看板 + 工单列表

### Step 5: 前端完善（🐼 前端熊）
- 工单详情 + 评论 + 附件
- AI 建议面板
- SLA 倒计时
- 客户管理（脱敏展示）
- 实时通知

### Step 6: 测试 + 部署（🐨 测试熊 + 🧸 运维熊）
- 核心 Service 单元测试
- Dockerfile
- 集成验证

---

## 8. 端口与配置
- **后端端口**: 8088
- **前端端口**: 5175（开发模式）
- **API 前缀**: `/api/ticket/`
- **表前缀**: `ticket_`
- **包名**: `com.basebackend.ticket`
- **工单编号格式**: `TK` + 日期 + 4位序号（如 TK202602280001）
