# 实时聊天系统技术规格文档

> **版本**: 1.0.0
> **日期**: 2026-02-27
> **状态**: Draft
> **模块**: basebackend-chat-api
> **技术栈**: Java 25 / Spring Boot 4.0.3 / Spring Cloud 2025.1.1 / MyBatis-Plus 3.5.16

---

## 目录

1. [系统概述](#1-系统概述)
2. [系统架构](#2-系统架构)
3. [数据模型设计](#3-数据模型设计)
4. [API 接口设计](#4-api-接口设计)
5. [WebSocket 协议设计](#5-websocket-协议设计)
6. [消息处理流程](#6-消息处理流程)
7. [搜索集成](#7-搜索集成)
8. [文件处理](#8-文件处理)
9. [前端架构](#9-前端架构)
10. [安全设计](#10-安全设计)
11. [性能设计](#11-性能设计)
12. [项目结构](#12-项目结构)
13. [开发计划](#13-开发计划)
14. [技术决策记录](#14-技术决策记录)

---

## 1. 系统概述

### 1.1 项目定位

基于 basebackend 平台构建的企业级实时聊天系统，作为独立微服务 `basebackend-chat-api` 部署。提供类微信的全功能即时通讯能力，复用平台已有的 JWT 鉴权、分布式 ID、Redis 缓存、文件存储、全文搜索等基础设施。

### 1.2 设计目标

| 目标 | 指标 |
|------|------|
| 消息延迟 | 端到端 ≤ 200ms（同机房） |
| 单机并发连接 | ≥ 50,000 WebSocket 长连接 |
| 消息吞吐 | 单节点 ≥ 10,000 msg/s |
| 消息可靠性 | 不丢失、不重复（at-least-once + 客户端去重） |
| 可用性 | 99.9%（多节点部署，无单点故障） |
| 数据保留 | 消息永久存储，支持历史漫游 |
| 多租户 | 全表 tenant_id 隔离，数据完全隔离 |

### 1.3 功能清单

**聊天核心**

| 功能 | 说明 |
|------|------|
| 1v1 私聊 | 两用户间实时消息通信 |
| 群聊 | 多人群组消息，上限 500 人 |
| 消息类型 | 文本、图片、文件、语音、视频、位置、名片、表情 |
| 消息撤回 | 2 分钟内可撤回已发送消息 |
| 消息转发 | 逐条转发、合并转发 |
| 已读/未读 | 私聊已读回执，群聊已读计数 |
| 在线状态 | 实时在线/离线/忙碌状态展示 |
| 历史消息 | 上拉加载历史，按时间线浏览 |
| 消息搜索 | 全文搜索聊天记录，支持按联系人/群/时间筛选 |
| 会话列表 | 最近会话排序，未读计数角标，置顶/免打扰 |
| @提醒 | 群聊中 @指定成员 或 @所有人 |
| 消息引用 | 引用回复特定消息 |

**好友系统**

| 功能 | 说明 |
|------|------|
| 添加好友 | 发送好友申请，对方同意后建立好友关系 |
| 删除好友 | 单向删除，对方无感知 |
| 好友备注 | 设置好友备注名 |
| 好友分组 | 自定义分组管理好友 |
| 黑名单 | 拉黑/取消拉黑，屏蔽对方消息和好友申请 |
| 好友搜索 | 按用户名/手机号/邮箱搜索用户 |

**群组管理**

| 功能 | 说明 |
|------|------|
| 创建群 | 选择好友创建群聊，设置群名/头像 |
| 解散群 | 仅群主可解散 |
| 邀请入群 | 群成员邀请好友入群 |
| 踢出成员 | 群主/管理员可移除成员 |
| 群公告 | 发布/编辑/删除群公告 |
| 全体禁言 | 群主/管理员可开启/关闭全体禁言 |
| 单人禁言 | 对指定成员设置禁言时长 |
| 群角色 | 群主 > 管理员 > 普通成员，分级权限 |
| 退出群聊 | 成员主动退出 |
| 群成员列表 | 查看群成员，显示角色/在线状态 |

---

## 2. 系统架构

### 2.1 架构总览

```
┌─────────────────────────────────────────────────────────────────────┐
│                        客户端 (React + TypeScript)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐    │
│  │ 会话列表  │  │  聊天窗口 │  │ 通讯录   │  │  群管理/好友管理  │    │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘  └────────┬─────────┘    │
│        │             │             │                 │              │
│  ┌─────┴─────────────┴─────────────┴─────────────────┴─────────┐   │
│  │              WebSocket Client + REST API Client               │   │
│  └───────────────────────┬────────────────────┬─────────────────┘   │
└──────────────────────────┼────────────────────┼─────────────────────┘
                           │ wss://             │ https://
┌──────────────────────────┼────────────────────┼─────────────────────┐
│                   basebackend-gateway                               │
│              (路由 + JWT鉴权 + 限流 + 负载均衡)                      │
└──────────────────────────┼────────────────────┼─────────────────────┘
                           │                    │
┌──────────────────────────┴────────────────────┴─────────────────────┐
│                     basebackend-chat-api (聊天微服务)                 │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                      接入层 (Gateway Layer)                   │   │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐   │   │
│  │  │ REST控制器   │  │ WS消息调度器  │  │ JWT鉴权+租户解析  │   │   │
│  │  └──────┬──────┘  └──────┬───────┘  └───────────────────┘   │   │
│  └─────────┼────────────────┼──────────────────────────────────┘   │
│            │                │                                       │
│  ┌─────────┴────────────────┴──────────────────────────────────┐   │
│  │                      业务层 (Service Layer)                   │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────┐  │   │
│  │  │ 消息服务    │ │ 会话服务    │ │ 好友服务    │ │ 群组服务  │  │   │
│  │  │ MessageSvc │ │ ConvSvc    │ │ FriendSvc  │ │ GroupSvc │  │   │
│  │  └────────────┘ └────────────┘ └────────────┘ └──────────┘  │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐               │   │
│  │  │ 在线状态    │ │ 已读服务    │ │ 搜索服务    │               │   │
│  │  │ PresenceSvc│ │ ReadSvc    │ │ SearchSvc  │               │   │
│  │  └────────────┘ └────────────┘ └────────────┘               │   │
│  └─────────────────────────────────────────────────────────────┘   │
│            │                │                │                       │
│  ┌─────────┴────────────────┴────────────────┴─────────────────┐   │
│  │                     数据层 (Data Layer)                       │   │
│  │  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌────────────┐  │   │
│  │  │ MySQL    │  │ Redis    │  │ ES 搜索   │  │ MinIO 文件 │  │   │
│  │  │ 持久化   │  │ 缓存/推送 │  │ 全文检索  │  │ 对象存储   │  │   │
│  │  └──────────┘  └──────────┘  └───────────┘  └────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘

横向依赖:
┌─────────────────┐  ┌──────────────────┐  ┌─────────────────────┐
│ basebackend-    │  │ basebackend-     │  │ basebackend-        │
│ websocket       │  │ search           │  │ file-service        │
│ (WS基础设施)    │  │ (全文搜索)        │  │ (文件存储)          │
└─────────────────┘  └──────────────────┘  └─────────────────────┘
┌─────────────────┐  ┌──────────────────┐  ┌─────────────────────┐
│ basebackend-    │  │ basebackend-     │  │ basebackend-        │
│ jwt             │  │ cache            │  │ database            │
│ (令牌鉴权)      │  │ (缓存+分布式锁)   │  │ (数据源+多租户)      │
└─────────────────┘  └──────────────────┘  └─────────────────────┘
```

### 2.2 技术栈明细

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 运行时 | Java | 25 | 虚拟线程(Virtual Threads)支持高并发 |
| 框架 | Spring Boot | 4.0.3 | 应用框架 |
| 微服务 | Spring Cloud | 2025.1.1 | 服务注册/发现/配置 |
| ORM | MyBatis-Plus | 3.5.16 | 数据访问层 |
| WebSocket | Spring WebSocket | — | 复用 basebackend-websocket 模块 |
| 数据库 | MySQL | 8.4+ | 消息/关系持久化 |
| 缓存 | Redis + Redisson | 7.x / 3.24+ | 会话缓存/在线状态/未读计数/分布式锁 |
| 搜索 | Elasticsearch | 8.17+ | 消息全文搜索（via basebackend-search） |
| 文件 | MinIO | 8.5+ | 图片/语音/视频/文件存储（via basebackend-file-service） |
| 序列化 | Jackson | — | JSON 序列化/反序列化 |
| ID 生成 | Snowflake | — | 分布式消息 ID，趋势递增 |

### 2.3 模块依赖关系

```
basebackend-chat-api
├── basebackend-common-core       (Result, ErrorCode, PageQuery, PageResult)
├── basebackend-common-dto        (DTO 基类)
├── basebackend-common-context    (UserContextHolder, TenantContextHolder)
├── basebackend-common-security   (权限注解)
├── basebackend-jwt               (JWT 鉴权, JwtUserDetails)
├── basebackend-cache             (CacheService, DistributedLockService, @RateLimit)
├── basebackend-database          (数据源, 多租户拦截器)
├── basebackend-websocket         (SessionManager, ChannelManager, WsMessageHandler)
├── basebackend-search            (SearchClient, SearchQuery, IndexDefinition)
├── basebackend-feign-api         (Feign 声明式调用 file-service)
├── basebackend-logging           (结构化日志, 审计日志)
├── basebackend-web               (Web 层公共配置)
└── basebackend-observability     (Metrics/Tracing)
```

---

## 3. 数据模型设计

### 3.1 ER 关系概览

```
chat_friend_request ─────→ chat_friend ──────── chat_friend_group
                               │
                               │ (user_id / friend_id)
                               │
                          chat_blacklist

chat_conversation ─────────── chat_conversation_member
     │
     │ (conversation_id)
     │
chat_message ──────────────── chat_message_read
     │
     │ (quote_message_id)
     │
chat_message (自引用)

chat_group ─────────────────── chat_group_member
     │
     │ (group_id)
     │
chat_group_announcement
```

### 3.2 建表 SQL

#### 3.2.1 chat_friend — 好友关系表

```sql
CREATE TABLE `chat_friend` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID (Snowflake)',
    `tenant_id`     BIGINT       NOT NULL COMMENT '租户ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `friend_id`     BIGINT       NOT NULL COMMENT '好友用户ID',
    `remark`        VARCHAR(64)  DEFAULT NULL COMMENT '好友备注名',
    `group_id`      BIGINT       DEFAULT NULL COMMENT '好友分组ID (关联chat_friend_group.id)',
    `source`        TINYINT      NOT NULL DEFAULT 0 COMMENT '添加来源: 0-搜索 1-群聊 2-名片 3-扫码',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-待验证 1-正常 2-已删除',
    `extra`         JSON         DEFAULT NULL COMMENT '扩展信息 JSON',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_friend` (`tenant_id`, `user_id`, `friend_id`, `deleted`),
    KEY `idx_tenant_friend` (`tenant_id`, `friend_id`),
    KEY `idx_tenant_user_status` (`tenant_id`, `user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='好友关系表 — 单向存储，A加B产生两条记录(A->B, B->A)';
```

#### 3.2.2 chat_friend_group — 好友分组表

```sql
CREATE TABLE `chat_friend_group` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`     BIGINT       NOT NULL COMMENT '租户ID',
    `user_id`       BIGINT       NOT NULL COMMENT '所属用户ID',
    `name`          VARCHAR(64)  NOT NULL COMMENT '分组名称',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序值 (升序)',
    `is_default`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认分组: 0-否 1-是',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='好友分组表';
```

#### 3.2.3 chat_blacklist — 黑名单表

```sql
CREATE TABLE `chat_blacklist` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`     BIGINT       NOT NULL COMMENT '租户ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID (发起拉黑方)',
    `blocked_id`    BIGINT       NOT NULL COMMENT '被拉黑用户ID',
    `reason`        VARCHAR(256) DEFAULT NULL COMMENT '拉黑原因',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_blocked` (`tenant_id`, `user_id`, `blocked_id`, `deleted`),
    KEY `idx_tenant_blocked` (`tenant_id`, `blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='黑名单表 — 单向拉黑';
```

#### 3.2.4 chat_conversation — 会话表

```sql
CREATE TABLE `chat_conversation` (
    `id`                   BIGINT       NOT NULL COMMENT '主键ID (Snowflake)',
    `tenant_id`            BIGINT       NOT NULL COMMENT '租户ID',
    `type`                 TINYINT      NOT NULL COMMENT '会话类型: 1-私聊 2-群聊',
    `target_id`            BIGINT       NOT NULL COMMENT '目标ID: 私聊=对方用户ID 群聊=群ID',
    `last_message_id`      BIGINT       DEFAULT NULL COMMENT '最后一条消息ID',
    `last_message_time`    DATETIME     DEFAULT NULL COMMENT '最后消息时间 (冗余，用于排序)',
    `last_message_preview` VARCHAR(256) DEFAULT NULL COMMENT '最后消息预览文本',
    `last_sender_id`       BIGINT       DEFAULT NULL COMMENT '最后发送人ID',
    `member_count`         INT          NOT NULL DEFAULT 2 COMMENT '成员数量',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_type_target` (`tenant_id`, `type`, `target_id`),
    KEY `idx_last_msg_time` (`tenant_id`, `last_message_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='会话表 — 每个私聊/群聊对应一条会话记录';
```

#### 3.2.5 chat_conversation_member — 会话成员表

```sql
CREATE TABLE `chat_conversation_member` (
    `id`                    BIGINT        NOT NULL COMMENT '主键ID',
    `tenant_id`             BIGINT        NOT NULL COMMENT '租户ID',
    `conversation_id`       BIGINT        NOT NULL COMMENT '会话ID',
    `user_id`               BIGINT        NOT NULL COMMENT '成员用户ID',
    `unread_count`          INT           NOT NULL DEFAULT 0 COMMENT '未读消息数',
    `last_read_message_id`  BIGINT        DEFAULT NULL COMMENT '最后已读消息ID',
    `last_read_time`        DATETIME      DEFAULT NULL COMMENT '最后已读时间',
    `is_pinned`             TINYINT       NOT NULL DEFAULT 0 COMMENT '是否置顶: 0-否 1-是',
    `is_muted`              TINYINT       NOT NULL DEFAULT 0 COMMENT '是否免打扰: 0-否 1-是',
    `is_hidden`             TINYINT       NOT NULL DEFAULT 0 COMMENT '是否隐藏: 0-否 1-是',
    `draft`                 VARCHAR(1024) DEFAULT NULL COMMENT '草稿内容',
    `join_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conv_user` (`tenant_id`, `conversation_id`, `user_id`, `deleted`),
    KEY `idx_tenant_user_pinned` (`tenant_id`, `user_id`, `is_pinned` DESC, `deleted`),
    KEY `idx_tenant_user_hidden` (`tenant_id`, `user_id`, `is_hidden`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='会话成员表 — 每个用户在每个会话中的独立设置';
```

#### 3.2.6 chat_message — 消息表

```sql
CREATE TABLE `chat_message` (
    `id`               BIGINT        NOT NULL COMMENT '消息ID (Snowflake, 趋势递增, 兼做排序序号)',
    `tenant_id`        BIGINT        NOT NULL COMMENT '租户ID',
    `conversation_id`  BIGINT        NOT NULL COMMENT '会话ID',
    `sender_id`        BIGINT        NOT NULL COMMENT '发送者用户ID',
    `sender_name`      VARCHAR(64)   NOT NULL COMMENT '发送者昵称 (冗余快照)',
    `sender_avatar`    VARCHAR(512)  DEFAULT NULL COMMENT '发送者头像 (冗余快照)',
    `type`             TINYINT       NOT NULL COMMENT '消息类型: 1-文本 2-图片 3-文件 4-语音 5-视频 6-位置 7-名片 8-表情 9-系统通知 10-撤回 11-合并转发',
    `content`          TEXT          DEFAULT NULL COMMENT '消息内容 (文本消息为纯文本, 其他类型为JSON)',
    `reply_to_msg_id`  BIGINT        DEFAULT NULL COMMENT '引用回复的消息ID',
    `forward_from_msg_id` BIGINT     DEFAULT NULL COMMENT '转发来源消息ID',
    `forward_from_conversation_id` BIGINT DEFAULT NULL COMMENT '转发来源会话ID',
    `extra`            JSON          DEFAULT NULL COMMENT '扩展信息 JSON (宽度/高度/时长/文件名/大小等)',
    `quote_message_id` BIGINT        DEFAULT NULL COMMENT '引用消息ID (回复某条消息)',
    `at_user_ids`      JSON          DEFAULT NULL COMMENT '@用户ID列表, JSON数组, ["all"]表示@所有人',
    `client_msg_id`    VARCHAR(64)   DEFAULT NULL COMMENT '客户端消息ID (用于去重)',
    `send_time`        DATETIME(3)   NOT NULL COMMENT '发送时间 (毫秒精度)',
    `status`           TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0-发送中 1-已发送 2-已撤回 3-审核中 4-已屏蔽',
    `revoke_time`      DATETIME      DEFAULT NULL COMMENT '撤回时间',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_conv_sendtime` (`conversation_id`, `send_time` DESC),
    KEY `idx_tenant_conv_id` (`tenant_id`, `conversation_id`, `id` DESC),
    KEY `idx_sender` (`tenant_id`, `sender_id`, `send_time` DESC),
    KEY `idx_client_msg` (`client_msg_id`),
    KEY `idx_reply_to` (`reply_to_msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='消息表 — 按conversation_id写入, ID即为消息序号';
```

#### 3.2.7 chat_message_read — 消息已读状态表

```sql
CREATE TABLE `chat_message_read` (
    `id`              BIGINT    NOT NULL COMMENT '主键ID',
    `tenant_id`       BIGINT    NOT NULL COMMENT '租户ID',
    `conversation_id` BIGINT    NOT NULL COMMENT '会话ID',
    `message_id`      BIGINT    NOT NULL COMMENT '消息ID',
    `user_id`         BIGINT    NOT NULL COMMENT '已读用户ID',
    `read_time`       DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_user` (`tenant_id`, `message_id`, `user_id`),
    KEY `idx_conv_user` (`tenant_id`, `conversation_id`, `user_id`, `message_id` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='消息已读状态表 — 群聊已读回执, 私聊仅记录在conversation_member.last_read_message_id';
```

#### 3.2.8 chat_group — 群组表

```sql
CREATE TABLE `chat_group` (
    `id`              BIGINT       NOT NULL COMMENT '群ID (Snowflake)',
    `tenant_id`       BIGINT       NOT NULL COMMENT '租户ID',
    `name`            VARCHAR(128) NOT NULL COMMENT '群名称',
    `avatar`          VARCHAR(512) DEFAULT NULL COMMENT '群头像URL',
    `description`     VARCHAR(512) DEFAULT NULL COMMENT '群描述/简介',
    `owner_id`        BIGINT       NOT NULL COMMENT '群主用户ID',
    `conversation_id` BIGINT       DEFAULT NULL COMMENT '关联会话ID',
    `max_members`     INT          NOT NULL DEFAULT 500 COMMENT '最大成员数',
    `member_count`    INT          NOT NULL DEFAULT 1 COMMENT '当前成员数',
    `is_muted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '全体禁言: 0-否 1-是',
    `join_mode`       TINYINT      NOT NULL DEFAULT 0 COMMENT '入群方式: 0-自由加入 1-需审批 2-仅邀请',
    `invite_confirm`  TINYINT      NOT NULL DEFAULT 0 COMMENT '邀请需确认: 0-直接入群 1-被邀请人确认',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '群状态: 0-已解散 1-正常 2-封禁',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_owner` (`tenant_id`, `owner_id`),
    KEY `idx_tenant_name` (`tenant_id`, `name`),
    KEY `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='群组信息表';
```

#### 3.2.9 chat_group_member — 群成员表

```sql
CREATE TABLE `chat_group_member` (
    `id`               BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`        BIGINT       NOT NULL COMMENT '租户ID',
    `group_id`         BIGINT       NOT NULL COMMENT '群ID',
    `user_id`          BIGINT       NOT NULL COMMENT '成员用户ID',
    `nickname`         VARCHAR(64)  DEFAULT NULL COMMENT '群内昵称',
    `role`             TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0-普通成员 1-管理员 2-群主',
    `inviter_id`       BIGINT       DEFAULT NULL COMMENT '邀请人用户ID',
    `is_muted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '个人禁言: 0-否 1-是',
    `mute_expire_time` DATETIME     DEFAULT NULL COMMENT '禁言到期时间 (NULL=永久)',
    `join_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入群时间',
    `last_active_time` DATETIME     DEFAULT NULL COMMENT '最后活跃时间',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`tenant_id`, `group_id`, `user_id`, `deleted`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_group_role` (`group_id`, `role` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='群成员表';
```

#### 3.2.10 chat_group_announcement — 群公告表

```sql
CREATE TABLE `chat_group_announcement` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`     BIGINT       NOT NULL COMMENT '租户ID',
    `group_id`      BIGINT       NOT NULL COMMENT '群ID',
    `publisher_id`  BIGINT       NOT NULL COMMENT '发布者用户ID',
    `title`         VARCHAR(128) DEFAULT NULL COMMENT '公告标题',
    `content`       TEXT         NOT NULL COMMENT '公告内容',
    `is_pinned`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶: 0-否 1-是',
    `is_confirmed`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需要确认: 0-否 1-是',
    `confirm_count` INT          NOT NULL DEFAULT 0 COMMENT '已确认人数',
    `publish_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_group_pinned` (`tenant_id`, `group_id`, `is_pinned` DESC, `publish_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='群公告表';
```

#### 3.2.11 chat_friend_request — 好友申请表

```sql
CREATE TABLE `chat_friend_request` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`     BIGINT       NOT NULL COMMENT '租户ID',
    `from_user_id`  BIGINT       NOT NULL COMMENT '申请人用户ID',
    `to_user_id`    BIGINT       NOT NULL COMMENT '被申请人用户ID',
    `message`       VARCHAR(256) DEFAULT NULL COMMENT '验证消息',
    `source`        TINYINT      NOT NULL DEFAULT 0 COMMENT '来源: 0-搜索 1-群聊 2-名片 3-扫码',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-已同意 2-已拒绝 3-已过期',
    `handle_time`   DATETIME     DEFAULT NULL COMMENT '处理时间',
    `expire_time`   DATETIME     NOT NULL COMMENT '过期时间 (默认7天)',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_to_user` (`tenant_id`, `to_user_id`, `status`),
    KEY `idx_from_user` (`tenant_id`, `from_user_id`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='好友申请表';
```

#### 3.2.12 chat_message_forward — 合并转发消息表

```sql
CREATE TABLE `chat_message_forward` (
    `id`                       BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`                BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `forward_id`               VARCHAR(64)  NOT NULL COMMENT '转发批次ID（多条消息打包转发共享同一ID）',
    `original_msg_id`          BIGINT       NOT NULL COMMENT '原始消息ID',
    `original_conversation_id` BIGINT       NOT NULL COMMENT '原始会话ID',
    `original_sender_id`       BIGINT       NOT NULL COMMENT '原始发送者ID',
    `original_sender_name`     VARCHAR(100) DEFAULT NULL COMMENT '原始发送者昵称（快照）',
    `original_content`         TEXT         DEFAULT NULL COMMENT '原始消息内容（快照）',
    `original_content_type`    TINYINT      NOT NULL COMMENT '原始消息类型',
    `original_send_time`       DATETIME     NOT NULL COMMENT '原始发送时间',
    `seq_no`                   INT          NOT NULL DEFAULT 0 COMMENT '在转发包中的排序',
    `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_forward_id` (`forward_id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合并转发消息表';
```

### 3.3 消息 extra 字段 JSON 结构

不同消息类型的 `extra` 字段携带类型特定的附加信息：

```jsonc
// type=2 图片消息
{
    "url": "https://minio.example.com/chat/img/2026/02/abc.jpg",
    "thumbnailUrl": "https://minio.example.com/chat/img/2026/02/abc_thumb.jpg",
    "width": 1920,
    "height": 1080,
    "size": 254890,
    "format": "jpg"
}

// type=3 文件消息
{
    "url": "https://minio.example.com/chat/file/2026/02/report.pdf",
    "fileName": "年度报告.pdf",
    "size": 1048576,
    "format": "pdf"
}

// type=4 语音消息
{
    "url": "https://minio.example.com/chat/voice/2026/02/voice-001.mp3",
    "duration": 15,
    "size": 32000,
    "format": "mp3"
}

// type=5 视频消息
{
    "url": "https://minio.example.com/chat/video/2026/02/video-001.mp4",
    "thumbnailUrl": "https://minio.example.com/chat/video/2026/02/video-001_cover.jpg",
    "duration": 60,
    "width": 1280,
    "height": 720,
    "size": 10485760,
    "format": "mp4"
}

// type=6 位置消息
{
    "latitude": 39.9042,
    "longitude": 116.4074,
    "address": "北京市东城区天安门广场",
    "name": "天安门"
}

// type=7 名片消息
{
    "userId": 10086,
    "nickname": "张三",
    "avatar": "https://minio.example.com/avatar/zhangsan.jpg",
    "username": "zhangsan"
}

// type=8 表情消息
{
    "emojiId": "smile_001",
    "emojiUrl": "https://cdn.example.com/emoji/smile_001.gif",
    "width": 120,
    "height": 120
}

// type=11 合并转发
{
    "title": "张三和李四的聊天记录",
    "summary": ["张三: 明天开会吗？", "李四: 是的，下午3点", "张三: 好的"],
    "messageIds": [101, 102, 103],
    "messages": [
        {"sender": "张三", "content": "明天开会吗？", "time": "2026-02-27 10:00:00"},
        {"sender": "李四", "content": "是的，下午3点", "time": "2026-02-27 10:01:00"},
        {"sender": "张三", "content": "好的", "time": "2026-02-27 10:01:30"}
    ]
}
```

### 3.4 Redis Key 设计

所有 Key 前缀 `chat:`，遵循平台 basebackend-cache 的 `:` 分隔符规范。

| Key Pattern | 类型 | TTL | 说明 |
|-------------|------|-----|------|
| `chat:online:{tenantId}` | SET | — | 在线用户 ID 集合 |
| `chat:status:{tenantId}:{userId}` | STRING | — | 用户状态: `online` / `offline` / `busy` |
| `chat:status:last_active:{tenantId}:{userId}` | STRING | 7d | 最后活跃时间戳 |
| `chat:unread:{tenantId}:{userId}:{conversationId}` | STRING(int) | — | 单会话未读数 |
| `chat:unread:total:{tenantId}:{userId}` | STRING(int) | — | 总未读数 |
| `chat:conv:recent:{tenantId}:{userId}` | ZSET | — | 最近会话列表, score=lastMessageTime |
| `chat:conv:info:{tenantId}:{conversationId}` | HASH | 30m | 会话信息缓存 |
| `chat:group:info:{tenantId}:{groupId}` | HASH | 30m | 群信息缓存 |
| `chat:group:members:{tenantId}:{groupId}` | SET | 30m | 群成员 ID 集合 |
| `chat:msg:dedup:{clientMsgId}` | STRING(1) | 5m | 客户端消息去重 |
| `chat:typing:{tenantId}:{conversationId}:{userId}` | STRING(1) | 5s | 正在输入状态 |
| `chat:friend:list:{tenantId}:{userId}` | SET | 30m | 好友 ID 列表缓存 |
| `chat:blacklist:{tenantId}:{userId}` | SET | 30m | 黑名单 ID 列表缓存 |
| `chat:msg:stream:{tenantId}:{conversationId}` | STREAM | 24h | 消息推送流 (削峰) |
| `chat:lock:conv:{conversationId}` | STRING | 10s | 会话操作分布式锁 |
| `chat:lock:group:{groupId}` | STRING | 10s | 群操作分布式锁 |
| `chat:session:{tenantId}:{userId}` | HASH | — | WS 会话路由表 {nodeId → sessionId} |

---

## 4. API 接口设计

基础路径: `/api/chat`
统一响应包装: `Result<T>`（code, message, data, timestamp）
分页请求: `PageQuery`（pageNum, pageSize, sorts）
分页响应: `PageResult<T>`（current, size, total, pages, records）

错误码范围: `6000-6999`（聊天模块专用，注册于 `ChatErrorCode implements ErrorCode`）

| 错误码 | 含义 |
|--------|------|
| 6001 | 会话不存在 |
| 6002 | 无权限操作该会话 |
| 6003 | 消息发送失败 |
| 6004 | 消息已过撤回时限 |
| 6005 | 好友关系不存在 |
| 6006 | 已在黑名单中 |
| 6007 | 好友申请不存在或已处理 |
| 6010 | 群组不存在 |
| 6011 | 非群成员无权操作 |
| 6012 | 群成员已满 |
| 6013 | 权限不足（需群主/管理员） |
| 6014 | 不能对群主执行该操作 |
| 6015 | 群已解散 |
| 6020 | 禁言中，无法发送消息 |
| 6030 | 文件上传失败 |
| 6040 | 搜索服务不可用 |

### 4.1 好友模块

#### 4.1.1 搜索用户

```
GET /api/chat/friends/search?keyword={keyword}
```

查询参数: `keyword`（用户名/手机号/邮箱），必填

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "userId": 10001,
            "username": "zhangsan",
            "nickname": "张三",
            "avatar": "https://minio.example.com/avatar/zhangsan.jpg",
            "isFriend": true,
            "isBlocked": false
        }
    ],
    "timestamp": 1740000000000
}
```

#### 4.1.2 发送好友申请

```
POST /api/chat/friends/request
```

请求体:
```json
{
    "toUserId": 10002,
    "message": "你好，我是张三，加个好友吧",
    "source": 0
}
```

响应:
```json
{
    "code": 200,
    "message": "好友申请已发送",
    "data": { "requestId": 20001 },
    "timestamp": 1740000000000
}
```

#### 4.1.3 处理好友申请

```
PUT /api/chat/friends/request/{requestId}
```

请求体:
```json
{
    "action": "accept",
    "remark": "三哥",
    "groupId": 3001
}
```

`action`: `accept` | `reject`

响应:
```json
{
    "code": 200,
    "message": "已同意好友申请",
    "data": null,
    "timestamp": 1740000000000
}
```

#### 4.1.4 获取好友申请列表

```
GET /api/chat/friends/request/list?pageNum=1&pageSize=20
```

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "current": 1, "size": 20, "total": 5, "pages": 1,
        "records": [
            {
                "requestId": 20001,
                "fromUser": { "userId": 10001, "nickname": "张三", "avatar": "https://..." },
                "message": "你好，加个好友吧",
                "source": 0,
                "status": 0,
                "createTime": "2026-02-27 10:00:00",
                "expireTime": "2026-03-06 10:00:00"
            }
        ]
    },
    "timestamp": 1740000000000
}
```

#### 4.1.5 获取好友列表

```
GET /api/chat/friends?groupId={groupId}
```

查询参数: `groupId`（可选，按分组筛选）

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "userId": 10002,
            "nickname": "李四",
            "remark": "四弟",
            "avatar": "https://...",
            "status": "online",
            "groupId": 3001,
            "groupName": "同事"
        }
    ],
    "timestamp": 1740000000000
}
```

#### 4.1.6 修改好友备注

```
PUT /api/chat/friends/{friendUserId}/remark
```

请求体: `{ "remark": "老李" }`

#### 4.1.7 删除好友

```
DELETE /api/chat/friends/{friendUserId}
```

#### 4.1.8 好友分组 CRUD

```
GET    /api/chat/friends/groups                      -- 获取分组列表
POST   /api/chat/friends/groups                      -- 创建分组 {name, sortOrder}
PUT    /api/chat/friends/groups/{groupId}             -- 修改分组 {name, sortOrder}
DELETE /api/chat/friends/groups/{groupId}             -- 删除分组 (好友移至默认组)
PUT    /api/chat/friends/{friendUserId}/move-group    -- 移动好友到指定分组 {groupId}
```

#### 4.1.9 黑名单

```
POST   /api/chat/blacklist           -- 拉黑用户 {blockedId, reason}
DELETE /api/chat/blacklist/{userId}   -- 取消拉黑
GET    /api/chat/blacklist            -- 获取黑名单列表
```

黑名单列表响应:
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "userId": 10009,
            "nickname": "某人",
            "avatar": "https://...",
            "reason": "骚扰",
            "blockedTime": "2026-02-27 10:00:00"
        }
    ],
    "timestamp": 1740000000000
}
```

### 4.2 会话模块

#### 4.2.1 获取会话列表

```
GET /api/chat/conversations?pageNum=1&pageSize=20
```

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "current": 1, "size": 20, "total": 50, "pages": 3,
        "records": [
            {
                "conversationId": 50001,
                "type": 1,
                "targetId": 10002,
                "targetName": "李四",
                "targetAvatar": "https://...",
                "lastMessage": {
                    "messageId": 80001,
                    "type": 1,
                    "content": "明天开会吗？",
                    "senderName": "李四",
                    "sendTime": "2026-02-27 15:30:00"
                },
                "unreadCount": 3,
                "isPinned": false,
                "isMuted": false,
                "draft": null,
                "updateTime": "2026-02-27 15:30:00"
            },
            {
                "conversationId": 50002,
                "type": 2,
                "targetId": 60001,
                "targetName": "项目讨论群",
                "targetAvatar": "https://...",
                "lastMessage": {
                    "messageId": 80050,
                    "type": 2,
                    "content": "[图片]",
                    "senderName": "王五",
                    "sendTime": "2026-02-27 15:28:00"
                },
                "unreadCount": 12,
                "isPinned": true,
                "isMuted": false,
                "memberCount": 35,
                "draft": "未发完的消息...",
                "updateTime": "2026-02-27 15:28:00"
            }
        ]
    },
    "timestamp": 1740000000000
}
```

#### 4.2.2 创建/打开会话

```
POST /api/chat/conversations
```

请求体: `{ "type": 1, "targetId": 10002 }`

私聊会话已存在则返回已有会话（幂等），`created` 字段标识是否新建。

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "conversationId": 50001,
        "type": 1,
        "targetId": 10002,
        "targetName": "李四",
        "targetAvatar": "https://...",
        "created": false
    },
    "timestamp": 1740000000000
}
```

#### 4.2.3 会话设置

```
PUT    /api/chat/conversations/{conversationId}/pin     -- 置顶/取消置顶 {isPinned: true}
PUT    /api/chat/conversations/{conversationId}/mute    -- 免打扰 {isMuted: true}
PUT    /api/chat/conversations/{conversationId}/draft   -- 保存草稿 {draft: "内容"}
DELETE /api/chat/conversations/{conversationId}         -- 删除会话 (隐藏, 不删消息)
```

#### 4.2.4 清空未读

```
PUT /api/chat/conversations/{conversationId}/read
```

请求体: `{ "lastReadMessageId": 80050 }`

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": { "clearedCount": 12 },
    "timestamp": 1740000000000
}
```

### 4.3 消息模块

#### 4.3.1 发送消息（REST 备用通道）

主要消息发送通过 WebSocket，REST 作为降级通道（WebSocket 断连时使用）。

```
POST /api/chat/messages
```

请求体:
```json
{
    "conversationId": 50001,
    "type": 1,
    "content": "你好，明天上午有空吗？",
    "clientMsgId": "client-uuid-001",
    "quoteMessageId": null,
    "atUserIds": null,
    "extra": null
}
```

响应:
```json
{
    "code": 200,
    "message": "发送成功",
    "data": {
        "messageId": 80100,
        "conversationId": 50001,
        "sendTime": "2026-02-27 16:00:00.123",
        "status": 1
    },
    "timestamp": 1740000000000
}
```

#### 4.3.2 获取历史消息

```
GET /api/chat/messages?conversationId=50001&beforeId=80100&limit=30
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| conversationId | Long | 是 | 会话ID |
| beforeId | Long | 否 | 从此消息ID向前加载（不含），不传则从最新开始 |
| afterId | Long | 否 | 从此消息ID向后加载（不含），用于加载新消息 |
| limit | Integer | 否 | 条数，默认30，最大100 |

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "messages": [
            {
                "messageId": 80098,
                "conversationId": 50001,
                "senderId": 10001,
                "senderName": "张三",
                "senderAvatar": "https://...",
                "type": 1,
                "content": "下午三点开会",
                "extra": null,
                "quoteMessage": null,
                "atUserIds": null,
                "clientMsgId": "client-uuid-098",
                "sendTime": "2026-02-27 15:58:00.100",
                "status": 1
            },
            {
                "messageId": 80095,
                "conversationId": 50001,
                "senderId": 10002,
                "senderName": "李四",
                "senderAvatar": "https://...",
                "type": 2,
                "content": null,
                "extra": {
                    "url": "https://minio.example.com/chat/img/2026/02/abc.jpg",
                    "thumbnailUrl": "https://minio.example.com/chat/img/2026/02/abc_thumb.jpg",
                    "width": 1920, "height": 1080, "size": 254890, "format": "jpg"
                },
                "quoteMessage": null,
                "atUserIds": null,
                "clientMsgId": "client-uuid-095",
                "sendTime": "2026-02-27 15:55:00.200",
                "status": 1
            }
        ],
        "hasMore": true
    },
    "timestamp": 1740000000000
}
```

#### 4.3.3 撤回消息

```
POST /api/chat/messages/{messageId}/revoke
```

前置校验: 仅发送者本人可撤回，且在发送后 2 分钟内。

响应:
```json
{
    "code": 200,
    "message": "消息已撤回",
    "data": { "messageId": 80100, "revokeTime": "2026-02-27 16:01:30" },
    "timestamp": 1740000000000
}
```

#### 4.3.4 转发消息

```
POST /api/chat/messages/forward
```

请求体:
```json
{
    "messageIds": [80095, 80098],
    "targetConversationIds": [50002, 50003],
    "forwardType": "merge",
    "title": "张三和李四的聊天记录"
}
```

`forwardType`: `single`（逐条转发）| `merge`（合并转发）

响应:
```json
{
    "code": 200,
    "message": "转发成功",
    "data": { "forwardedCount": 2, "targetCount": 2 },
    "timestamp": 1740000000000
}
```

#### 4.3.5 群已读回执详情

```
GET /api/chat/messages/{messageId}/read-status
```

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "messageId": 80050,
        "totalMembers": 35,
        "readCount": 20,
        "unreadCount": 15,
        "readUsers": [
            {"userId": 10001, "nickname": "张三", "readTime": "2026-02-27 15:30:00"},
            {"userId": 10003, "nickname": "王五", "readTime": "2026-02-27 15:31:00"}
        ]
    },
    "timestamp": 1740000000000
}
```

### 4.4 群组模块

#### 4.4.1 创建群

```
POST /api/chat/groups
```

请求体:
```json
{
    "name": "项目讨论群",
    "avatar": null,
    "description": "项目日常讨论",
    "memberIds": [10002, 10003, 10004]
}
```

响应:
```json
{
    "code": 200,
    "message": "群创建成功",
    "data": {
        "groupId": 60001,
        "conversationId": 50100,
        "name": "项目讨论群",
        "memberCount": 4
    },
    "timestamp": 1740000000000
}
```

#### 4.4.2 获取群信息

```
GET /api/chat/groups/{groupId}
```

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "groupId": 60001,
        "name": "项目讨论群",
        "avatar": "https://...",
        "description": "项目日常讨论",
        "ownerId": 10001,
        "ownerName": "张三",
        "conversationId": 50100,
        "maxMembers": 500,
        "memberCount": 35,
        "isMuted": false,
        "joinMode": 0,
        "inviteConfirm": false,
        "myRole": 2,
        "createTime": "2026-02-20 10:00:00"
    },
    "timestamp": 1740000000000
}
```

#### 4.4.3 修改群信息

```
PUT /api/chat/groups/{groupId}
```

请求体: `{ "name": "新群名", "avatar": "https://...", "description": "新描述", "joinMode": 1, "inviteConfirm": true }`

权限: 群主、管理员

#### 4.4.4 解散群

```
DELETE /api/chat/groups/{groupId}
```

权限: 仅群主

#### 4.4.5 群成员管理

```
GET    /api/chat/groups/{groupId}/members                -- 成员列表
POST   /api/chat/groups/{groupId}/members                -- 邀请入群 {userIds: [10005, 10006]}
DELETE /api/chat/groups/{groupId}/members/{userId}        -- 踢出成员 (群主/管理员)
POST   /api/chat/groups/{groupId}/leave                   -- 退出群聊
PUT    /api/chat/groups/{groupId}/members/{userId}/role   -- 设置角色 {role: 1}
PUT    /api/chat/groups/{groupId}/members/{userId}/mute   -- 禁言 {isMuted: true, duration: 3600}
PUT    /api/chat/groups/{groupId}/mute-all                -- 全体禁言 {isMuted: true}
PUT    /api/chat/groups/{groupId}/nickname                -- 修改群内昵称 {nickname: "小张"}
```

群成员列表响应:
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "userId": 10001,
            "nickname": "张三",
            "groupNickname": "群主张三",
            "avatar": "https://...",
            "role": 2,
            "status": "online",
            "isMuted": false,
            "joinTime": "2026-02-20 10:00:00"
        },
        {
            "userId": 10002,
            "nickname": "李四",
            "groupNickname": null,
            "avatar": "https://...",
            "role": 1,
            "status": "offline",
            "isMuted": false,
            "joinTime": "2026-02-20 10:01:00"
        }
    ],
    "timestamp": 1740000000000
}
```

#### 4.4.6 群公告

```
GET    /api/chat/groups/{groupId}/announcements              -- 公告列表
POST   /api/chat/groups/{groupId}/announcements              -- 发布公告 {title, content, isPinned}
PUT    /api/chat/groups/{groupId}/announcements/{id}         -- 编辑公告
DELETE /api/chat/groups/{groupId}/announcements/{id}         -- 删除公告
POST   /api/chat/groups/{groupId}/announcements/{id}/confirm -- 确认公告
```

#### 4.4.7 转让群主

```
PUT /api/chat/groups/{groupId}/transfer
```

请求体: `{ "newOwnerId": 10002 }`

### 4.5 搜索模块

#### 4.5.1 搜索消息

```
GET /api/chat/search/messages?keyword=开会&conversationId=50001&startTime=2026-02-01&endTime=2026-02-28&pageNum=1&pageSize=20
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 是 | 搜索关键词 |
| conversationId | Long | 否 | 限定会话 |
| senderId | Long | 否 | 限定发送人 |
| type | Integer | 否 | 限定消息类型 |
| startTime | String | 否 | 起始时间 yyyy-MM-dd |
| endTime | String | 否 | 结束时间 yyyy-MM-dd |

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "current": 1, "size": 20, "total": 8, "pages": 1,
        "records": [
            {
                "messageId": 80098,
                "conversationId": 50001,
                "conversationName": "李四",
                "conversationType": 1,
                "senderId": 10001,
                "senderName": "张三",
                "type": 1,
                "content": "下午三点<em>开会</em>",
                "sendTime": "2026-02-27 15:58:00",
                "score": 5.62
            }
        ]
    },
    "timestamp": 1740000000000
}
```

#### 4.5.2 搜索联系人

```
GET /api/chat/search/contacts?keyword=张三
```

#### 4.5.3 搜索群组

```
GET /api/chat/search/groups?keyword=项目
```

### 4.6 在线状态模块

```
GET /api/chat/presence/batch?userIds=10001,10002,10003
```

响应:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "10001": {"status": "online", "lastActive": null},
        "10002": {"status": "offline", "lastActive": "2026-02-27 14:00:00"},
        "10003": {"status": "busy", "lastActive": null}
    },
    "timestamp": 1740000000000
}
```

```
PUT /api/chat/presence/status
```

请求体: `{ "status": "busy" }`

---

## 5. WebSocket 协议设计

### 5.1 连接流程

```
客户端                                                    服务端
  │                                                        │
  │─── GET /ws/chat?token=<JWT>&deviceId=<ID> ──────────→ │
  │                              [HTTP 101 Upgrade]        │
  │← ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
  │                                                        │
  │    ChatAuthInterceptor:                                │
  │    1. 从 query param 提取 token                        │
  │    2. JwtService.validateToken(token)                  │
  │    3. 解析 userId / tenantId / roles                   │
  │    4. 存入 WebSocketSession attributes                 │
  │    5. 鉴权失败 → 拒绝握手 (403)                        │
  │                                                        │
  │← ── {type:"connected", userId:10001, serverTime:...} ─┤
  │                                                        │
  │    SessionManager.register(session, userId)            │
  │    Redis SADD chat:online:{tenantId} userId            │
  │    Redis SET  chat:status:{tenantId}:{userId} "online" │
  │    广播上线事件给好友                                    │
  │                                                        │
  │─── {type:"ping"} ──────────────────────────────────→  │
  │← ── {type:"pong", serverTime:1740000025000} ─────── ─┤
  │           (每25秒一次心跳)                              │
  │                                                        │
  │─── {type:"chat", ...} ────────────────────────────→   │
  │← ── {type:"chat_ack", ...} ────────────────────── ─  ┤
  │                                                        │
  │    [异常断开: 60s无心跳]                                │
  │    SessionManager.unregister(sessionId)                │
  │    Redis SREM chat:online:{tenantId} userId            │
  │    Redis SET  chat:status:{tenantId}:{userId} "offline"│
  │    Redis SET  chat:status:last_active:{tenantId}:      │
  │              {userId} <timestamp>                      │
  │    广播离线事件给好友                                    │
```

### 5.2 消息帧格式

所有通信使用 JSON 文本帧，每帧包含 `type` 字段用于路由分发。

#### 5.2.1 客户端 → 服务端（上行帧）

**发送聊天消息**
```json
{
    "type": "chat",
    "clientMsgId": "uuid-abc-001",
    "conversationId": 50001,
    "msgType": 1,
    "content": "你好，在吗？",
    "extra": null,
    "quoteMessageId": null,
    "atUserIds": null
}
```

**撤回消息**
```json
{
    "type": "revoke",
    "messageId": 80100,
    "conversationId": 50001
}
```

**已读上报**
```json
{
    "type": "read",
    "conversationId": 50001,
    "lastReadMessageId": 80100
}
```

**正在输入**
```json
{
    "type": "typing",
    "conversationId": 50001
}
```

**心跳**
```json
{
    "type": "ping"
}
```

**状态设置**
```json
{
    "type": "presence",
    "status": "busy"
}
```

**重连增量同步**
```json
{
    "type": "sync",
    "conversations": {
        "50001": 80095,
        "50002": 80040
    }
}
```

`conversations`: `{conversationId: lastReceivedMessageId}` 映射

#### 5.2.2 服务端 → 客户端（下行帧）

**连接成功**
```json
{
    "type": "connected",
    "userId": 10001,
    "serverTime": 1740000000000
}
```

**消息发送确认（ACK）**
```json
{
    "type": "chat_ack",
    "clientMsgId": "uuid-abc-001",
    "messageId": 80100,
    "sendTime": "2026-02-27 16:00:00.123",
    "status": 1
}
```

**接收新消息**
```json
{
    "type": "chat",
    "messageId": 80100,
    "conversationId": 50001,
    "senderId": 10002,
    "senderName": "李四",
    "senderAvatar": "https://...",
    "msgType": 1,
    "content": "你好，在吗？",
    "extra": null,
    "quoteMessage": null,
    "atUserIds": null,
    "sendTime": "2026-02-27 16:00:00.123"
}
```

**消息撤回通知**
```json
{
    "type": "revoke",
    "messageId": 80100,
    "conversationId": 50001,
    "operatorId": 10002,
    "operatorName": "李四",
    "revokeTime": "2026-02-27 16:01:30"
}
```

**已读回执**
```json
{
    "type": "read_receipt",
    "conversationId": 50001,
    "userId": 10002,
    "lastReadMessageId": 80100,
    "readTime": "2026-02-27 16:02:00"
}
```

**正在输入通知**
```json
{
    "type": "typing",
    "conversationId": 50001,
    "userId": 10002,
    "nickname": "李四"
}
```

**好友在线状态变更**
```json
{
    "type": "presence",
    "userId": 10002,
    "status": "online",
    "timestamp": 1740000000000
}
```

**会话更新通知**
```json
{
    "type": "conversation_update",
    "conversationId": 50001,
    "lastMessage": {
        "messageId": 80100,
        "type": 1,
        "content": "你好，在吗？",
        "senderName": "李四",
        "sendTime": "2026-02-27 16:00:00.123"
    },
    "unreadCount": 4
}
```

**群变更通知**
```json
{
    "type": "group_event",
    "event": "member_join",
    "groupId": 60001,
    "conversationId": 50100,
    "data": {
        "userId": 10005,
        "nickname": "赵六",
        "inviterId": 10001,
        "inviterName": "张三"
    },
    "timestamp": 1740000000000
}
```

群事件 `event` 枚举:

| event | 说明 |
|-------|------|
| `member_join` | 成员加入 |
| `member_leave` | 成员退出 |
| `member_kick` | 成员被踢 |
| `group_dismiss` | 群解散 |
| `group_update` | 群信息修改 |
| `role_change` | 角色变更 |
| `mute_change` | 禁言变更 |
| `announcement` | 新公告 |
| `owner_transfer` | 群主转让 |

**好友事件通知**
```json
{
    "type": "friend_event",
    "event": "request",
    "data": {
        "requestId": 20001,
        "fromUser": { "userId": 10005, "nickname": "赵六", "avatar": "https://..." },
        "message": "你好，加个好友"
    },
    "timestamp": 1740000000000
}
```

好友事件 `event` 枚举: `request`（新申请）, `accept`（已同意）, `delete`（被删除）, `block`（被拉黑）

**增量同步响应**
```json
{
    "type": "sync_resp",
    "conversations": {
        "50001": { "messages": [...], "unreadCount": 3 },
        "50002": { "messages": [...], "unreadCount": 0 }
    }
}
```

**心跳响应**
```json
{
    "type": "pong",
    "serverTime": 1740000025000
}
```

**错误帧**
```json
{
    "type": "error",
    "code": 6020,
    "message": "禁言中，无法发送消息",
    "refClientMsgId": "uuid-abc-002"
}
```

### 5.3 心跳与断线重连策略

| 参数 | 值 | 说明 |
|------|-----|------|
| 心跳间隔 | 25s | 客户端每 25 秒发送 `ping` |
| 超时阈值 | 60s | 服务端 60s 未收到 `ping` 则断开连接 |
| 重连策略 | 指数退避 | 1s → 2s → 4s → 8s → 16s → 30s（上限） |
| 最大重连次数 | 无限 | 持续重连直到成功 |
| 重连恢复 | 增量同步 | 重连后客户端发送 `sync` 帧，服务端推送增量消息 |

---

## 6. 消息处理流程

### 6.1 消息发送时序（私聊）

```
发送方Client    Chat-API服务     MySQL      Redis      接收方Client
    │               │              │          │             │
    │── chat msg ──→│              │          │             │
    │               │              │          │             │
    │               │─ 去重检查 ──────────────→│             │
    │               │  SET NX clientMsgId     │             │
    │               │              │          │             │
    │               │─ 校验权限 ───│          │             │
    │               │  好友关系?   │          │             │
    │               │  黑名单?     │          │             │
    │               │              │          │             │
    │               │─ 敏感词过滤  │          │             │
    │               │  (DFA内存)   │          │             │
    │               │              │          │             │
    │               │─ Snowflake ID 生成 ──── │             │
    │               │              │          │             │
    │               │─ INSERT ────→│          │             │
    │               │  chat_message│          │             │
    │               │              │          │             │
    │               │─ UPDATE ────→│          │             │
    │               │  会话最后消息 │          │             │
    │               │              │          │             │
    │               │──────────────│── INCR ─→│             │
    │               │              │  unread  │             │
    │               │              │  count   │             │
    │               │              │          │             │
    │               │──────────────│── ZADD ─→│             │
    │               │              │  recent  │             │
    │               │              │  conv    │             │
    │               │              │          │             │
    │←── chat_ack ──│              │          │             │
    │ (messageId,   │              │          │             │
    │  sendTime)    │              │          │             │
    │               │              │          │             │
    │               │── 查在线 ───→│──────── →│             │
    │               │              │  online? │             │
    │               │              │          │             │
    │               │──────── 在线推送 ────── │─── chat ──→│
    │               │              │          │             │
    │               │─ 异步索引 ES │          │  (离线则等  │
    │               │              │          │   重连同步) │
```

### 6.2 消息发送时序（群聊 — 写扩散）

```
发送方Client    Chat-API服务           MySQL        Redis        群成员Clients
    │               │                    │            │              │
    │── chat msg ──→│                    │            │              │
    │               │                    │            │              │
    │               │─ 去重 + 权限校验 ──│            │              │
    │               │  是否群成员?        │            │              │
    │               │  是否被禁言?        │            │              │
    │               │                    │            │              │
    │               │─ 敏感词过滤        │            │              │
    │               │                    │            │              │
    │               │─ INSERT ──────────→│            │              │
    │               │  chat_message      │            │              │
    │               │                    │            │              │
    │               │─ 获取群成员列表 ──→│  ← SMEMBERS│              │
    │               │                    │   members  │              │
    │               │                    │            │              │
    │               │─ 批量更新 ────────→│            │              │
    │               │  每个成员的         │            │              │
    │               │  conversation      │            │              │
    │               │  _member 未读数     │            │              │
    │               │                    │            │              │
    │               │────────── PIPELINE ─│── INCR ──→│              │
    │               │                    │   N个成员   │              │
    │               │                    │   unread   │              │
    │               │                    │            │              │
    │←── chat_ack ──│                    │            │              │
    │               │                    │            │              │
    │               │── 逐个检查在线 ───→│── 在线? ──→│              │
    │               │                    │            │              │
    │               │──────────── 在线推送(并行) ─────│── chat ────→│
    │               │                    │            │  (在线成员)  │
```

### 6.3 离线消息处理

不使用独立离线消息表。离线消息通过以下机制保证：

1. **消息持久化**: 所有消息写入 `chat_message` 表，永久存储
2. **未读计数**: 每条消息发送时，对所有离线成员执行 Redis `INCR` 更新未读计数
3. **重连同步**: 客户端重连后发送 `sync` 帧，携带每个会话最后收到的 messageId
4. **增量查询**: 服务端根据 `WHERE id > lastReceivedMessageId` 查询增量消息，按批次推送
5. **分批推送**: 增量消息超过 100 条时分批推送，每批 50 条，间隔 100ms

### 6.4 消息撤回流程

```
发送方Client     Chat-API服务        MySQL         Redis         接收方Clients
    │                │                 │              │               │
    │── revoke ─────→│                 │              │               │
    │                │                 │              │               │
    │                │─ 校验: 是否本人消息?            │               │
    │                │─ 校验: 是否在2分钟内?           │               │
    │                │                 │              │               │
    │                │─ UPDATE ───────→│              │               │
    │                │  status=2       │              │               │
    │                │  revoke_time    │              │               │
    │                │                 │              │               │
    │                │─ 更新会话预览 ──→│              │               │
    │                │  "XX撤回了一条消息"             │               │
    │                │                 │              │               │
    │                │─ 删除ES索引文档 (异步)          │               │
    │                │                 │              │               │
    │                │──────── 推送撤回通知 ──────────→│─── revoke ──→│
    │                │                 │              │   (在线成员)  │
    │                │                 │              │               │
    │←─ revoke ACK ──│                 │              │               │
```

### 6.5 消息转发流程

- **逐条转发 (`single`)**: 为每条源消息在每个目标会话创建新的消息记录，`content` 复制原文，`sender_id` 为转发者。
- **合并转发 (`merge`)**: 在每个目标会话创建一条 `type=11`（合并转发）的消息，`extra` 字段包含原始消息的摘要列表（见 3.3 节合并转发 JSON 结构）。

### 6.6 群消息写扩散方案

选择 **混合写扩散**（详见第 14 章 ADR-001），核心策略：

| 维度 | 方案 |
|------|------|
| 消息存储 | 一条群消息只存一条 `chat_message` 记录（按 conversation_id 存储） |
| 索引更新 | 写入消息后，批量 INCR 每个成员的未读计数（Redis PIPELINE） |
| 会话排序 | ZADD 更新每个成员的最近会话 ZSET（Redis PIPELINE） |
| 实时推送 | 遍历群成员，在线者 WebSocket 推送，离线者仅 INCR 未读 |
| 大群优化 | 成员 > 100 时走 Redis Stream 异步推送 |

---

## 7. 搜索集成

### 7.1 与 basebackend-search 集成

复用平台 `basebackend-search` 模块的 `SearchClient` 接口，通过 `RestClientSearchClient` 连接 Elasticsearch。使用 `SearchQuery` fluent builder 构建查询，`SearchResult<T>` 接收结果。

默认分词：索引时 `ik_max_word`（细粒度），搜索时 `ik_smart`（智能分词）。

### 7.2 索引定义

#### 7.2.1 消息索引 chat_message

索引名: `{indexPrefix}chat_message`，shards=3, replicas=1

```java
IndexDefinition messageIndex = IndexDefinition.builder("chat_message")
    .textField("content", "ik_max_word")        // 消息内容, IK中文分词
    .keywordField("tenantId")                    // 租户ID, 精确过滤
    .keywordField("conversationId")              // 会话ID
    .keywordField("senderId")                    // 发送者ID
    .keywordField("senderName")                  // 发送者昵称
    .keywordField("type")                        // 消息类型
    .keywordField("status")                      // 消息状态
    .dateField("sendTime")                       // 发送时间
    .shards(3).replicas(1)
    .build();
```

等效 ES Mapping:
```json
{
    "settings": { "number_of_shards": 3, "number_of_replicas": 1 },
    "mappings": {
        "properties": {
            "content":        {"type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart"},
            "tenantId":       {"type": "keyword"},
            "conversationId": {"type": "keyword"},
            "senderId":       {"type": "keyword"},
            "senderName":     {"type": "keyword"},
            "type":           {"type": "keyword"},
            "status":         {"type": "keyword"},
            "sendTime":       {"type": "date", "format": "yyyy-MM-dd HH:mm:ss||epoch_millis"}
        }
    }
}
```

#### 7.2.2 联系人索引 chat_contact

索引名: `{indexPrefix}chat_contact`，shards=1, replicas=1

```json
{
    "mappings": {
        "properties": {
            "userId":   {"type": "keyword"},
            "tenantId": {"type": "keyword"},
            "username": {"type": "text", "analyzer": "ik_max_word", "fields": {"keyword": {"type": "keyword"}}},
            "nickname": {"type": "text", "analyzer": "ik_max_word", "fields": {"keyword": {"type": "keyword"}}},
            "phone":    {"type": "keyword"},
            "email":    {"type": "keyword"},
            "avatar":   {"type": "keyword", "index": false}
        }
    }
}
```

#### 7.2.3 群组索引 chat_group

索引名: `{indexPrefix}chat_group`，shards=1, replicas=1

```json
{
    "mappings": {
        "properties": {
            "groupId":     {"type": "keyword"},
            "tenantId":    {"type": "keyword"},
            "name":        {"type": "text", "analyzer": "ik_max_word", "fields": {"keyword": {"type": "keyword"}}},
            "description": {"type": "text", "analyzer": "ik_max_word"},
            "ownerId":     {"type": "keyword"},
            "memberCount": {"type": "integer"},
            "status":      {"type": "keyword"},
            "createTime":  {"type": "date"}
        }
    }
}
```

### 7.3 数据同步策略

| 策略 | 场景 | 实现 |
|------|------|------|
| 同步写入 | 新消息发送 | 消息入库后异步索引到 ES（`@Async` 虚拟线程池） |
| 同步删除 | 消息撤回 | 撤回后异步从 ES 删除该 document |
| 批量重建 | 历史数据/故障恢复 | 定时任务全量/增量 scan MySQL → bulkIndex ES |
| 写入失败补偿 | ES 不可用 | 失败记录写入 `chat_search_sync_fail` 表，补偿任务每分钟重试 |

### 7.4 搜索查询示例

```java
// 在指定会话中搜索消息
SearchQuery query = SearchQuery.builder("chat_message")
    .must(Condition.match("content", keyword))
    .filter(Condition.term("tenantId", String.valueOf(tenantId)))
    .filter(Condition.term("conversationId", String.valueOf(conversationId)))
    .filter(Condition.term("status", "1"))  // 仅已发送状态
    .highlight("content")
    .sortBy("sendTime", SortOrder.DESC)
    .page(pageNum, pageSize)
    .build();

SearchResult<MessageSearchDoc> result = searchClient.search(query, MessageSearchDoc.class);

// 跨会话全局搜索（限定用户有权限的会话列表）
SearchQuery globalQuery = SearchQuery.builder("chat_message")
    .must(Condition.match("content", keyword))
    .filter(Condition.term("tenantId", String.valueOf(tenantId)))
    .filter(Condition.terms("conversationId", userConversationIds))
    .filter(Condition.range("sendTime", startTime, endTime))
    .highlight("content")
    .sortBy("_score", SortOrder.DESC)
    .page(pageNum, pageSize)
    .build();
```

---

## 8. 文件处理

### 8.1 总览

聊天文件通过 `basebackend-file-service` 提供的 REST API 统一管理。chat-api 通过 Feign 客户端调用 file-service，文件实际存储在 MinIO 对象存储。

### 8.2 文件上传流程

```
客户端             Chat-API             File-Service          MinIO
  │                  │                      │                   │
  │── POST ─────────→│                      │                   │
  │  /api/chat/files/ │                      │                   │
  │  upload           │                      │                   │
  │  (multipart)      │                      │                   │
  │                   │                      │                   │
  │                   │─ 校验: 文件类型/大小  │                   │
  │                   │─ 校验: 会话成员权限   │                   │
  │                   │                      │                   │
  │                   │── Feign POST ───────→│                   │
  │                   │   /api/files/upload   │                   │
  │                   │                      │── PUT ───────────→│
  │                   │                      │  chat/{yyyy/MM/dd} │
  │                   │                      │  /{uuid}.{ext}    │
  │                   │                      │                   │
  │                   │   [图片/视频]         │                   │
  │                   │── 生成缩略图/封面 ──→│── PUT thumb ────→│
  │                   │                      │                   │
  │                   │← 返回 fileUrl ───────│                   │
  │                   │   + thumbnailUrl     │                   │
  │                   │                      │                   │
  │← 返回文件信息 ────│                      │                   │
  │   {url, thumbUrl,  │                      │                   │
  │    size, format}   │                      │                   │
```

### 8.3 文件类型限制

| 消息类型 | 允许格式 | 大小上限 |
|---------|---------|---------|
| 图片 | jpg, jpeg, png, gif, webp, bmp | 20MB |
| 文件 | 不限（排除可执行文件 .exe .bat .sh .cmd） | 100MB |
| 语音 | mp3, wav, ogg, m4a, aac | 10MB |
| 视频 | mp4, avi, mov, mkv, webm | 200MB |
| 表情 | gif, png, webp | 2MB |

### 8.4 缩略图生成

- **图片缩略图**: 等比缩放，宽度不超过 200px，质量 80%，格式 JPEG
- **视频封面**: 截取第 1 秒帧作为封面图，宽度不超过 400px
- **存储路径**: `chat/thumb/{yyyy/MM/dd}/{uuid}_thumb.jpg`

### 8.5 Feign 客户端定义

```java
@FeignClient(name = "basebackend-file-service", path = "/api/files")
public interface FileServiceClient {

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<String> upload(@RequestPart("file") MultipartFile file);

    @GetMapping("/{fileId}")
    Result<FileMetadataDTO> getFileInfo(@PathVariable("fileId") String fileId);

    @GetMapping("/{fileId}/thumbnail-url")
    Result<String> getThumbnailUrl(@PathVariable("fileId") String fileId);

    @DeleteMapping("/{fileId}")
    Result<Void> deleteFile(@PathVariable("fileId") String fileId);
}
```

### 8.6 聊天文件上传接口

```
POST /api/chat/files/upload
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 文件 |
| type | String | 是 | 类型: image / file / voice / video / emoji |
| conversationId | Long | 否 | 关联会话ID（用于权限校验） |

响应:
```json
{
    "code": 200,
    "message": "上传成功",
    "data": {
        "fileId": "file-uuid-001",
        "url": "https://minio.example.com/chat/img/2026/02/abc.jpg",
        "thumbnailUrl": "https://minio.example.com/chat/thumb/2026/02/abc_thumb.jpg",
        "fileName": "screenshot.jpg",
        "size": 254890,
        "format": "jpg",
        "width": 1920,
        "height": 1080
    },
    "timestamp": 1740000000000
}
```

---

## 9. 前端架构

### 9.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18 | UI 框架 |
| TypeScript | 5.x | 类型安全 |
| Ant Design | 5.x | UI 组件库（与 React 生态最契合、组件丰富、中文文档完善、IM 类组件如 Avatar/Badge/List/Popover 开箱即用） |
| Zustand | 5.x | 状态管理 |
| React Router | 6 | 路由 |
| Vite | 5.x | 构建工具 |
| dayjs | 1.x | 日期处理 |
| emoji-mart | 5.x | 表情选择器 |
| react-virtuoso | 4.x | 虚拟滚动列表 |

### 9.2 页面路由

```
/chat                           -- 聊天主页面 (三栏布局)
/chat/conversation/:id          -- 指定会话聊天窗口
/chat/contacts                  -- 通讯录
/chat/contacts/friends          -- 好友列表
/chat/contacts/groups           -- 群组列表
/chat/contacts/requests         -- 好友申请
/chat/contacts/blacklist        -- 黑名单
/chat/group/:groupId/settings   -- 群设置页
/chat/search                    -- 搜索页
```

### 9.3 组件结构

```
src/features/chat/
├── pages/
│   ├── ChatPage.tsx                  -- 聊天主页面 (三栏布局容器)
│   ├── ContactsPage.tsx              -- 通讯录页面
│   └── SearchPage.tsx                -- 搜索页面
│
├── components/
│   ├── conversation/
│   │   ├── ConversationList.tsx       -- 会话列表 (左栏)
│   │   ├── ConversationItem.tsx       -- 单个会话条目
│   │   ├── ConversationSearch.tsx     -- 会话搜索框
│   │   └── ConversationActions.tsx    -- 右键菜单 (置顶/免打扰/删除)
│   │
│   ├── message/
│   │   ├── MessagePanel.tsx           -- 消息面板 (中栏)
│   │   ├── MessageList.tsx            -- 消息列表 (虚拟滚动)
│   │   ├── MessageItem.tsx            -- 消息气泡
│   │   ├── MessageInput.tsx           -- 输入框 (文本/表情/文件/语音)
│   │   ├── MessageToolbar.tsx         -- 工具栏 (表情/图片/文件/...)
│   │   ├── MessageTypes/
│   │   │   ├── TextMessage.tsx        -- 文本消息
│   │   │   ├── ImageMessage.tsx       -- 图片消息 (点击预览)
│   │   │   ├── FileMessage.tsx        -- 文件消息 (下载)
│   │   │   ├── VoiceMessage.tsx       -- 语音消息 (播放)
│   │   │   ├── VideoMessage.tsx       -- 视频消息 (播放)
│   │   │   ├── LocationMessage.tsx    -- 位置消息 (地图)
│   │   │   ├── ContactCardMessage.tsx -- 名片消息
│   │   │   ├── EmojiMessage.tsx       -- 表情消息
│   │   │   ├── RevokedMessage.tsx     -- 已撤回消息
│   │   │   └── MergedForwardMessage.tsx -- 合并转发消息
│   │   ├── QuoteReply.tsx             -- 引用回复
│   │   └── ReadStatus.tsx             -- 已读状态
│   │
│   ├── contact/
│   │   ├── FriendList.tsx             -- 好友列表
│   │   ├── FriendRequestList.tsx      -- 好友申请列表
│   │   ├── FriendGroupList.tsx        -- 好友分组
│   │   ├── BlackList.tsx              -- 黑名单
│   │   ├── AddFriendModal.tsx         -- 添加好友弹窗
│   │   └── UserProfileCard.tsx        -- 用户名片
│   │
│   ├── group/
│   │   ├── GroupSettings.tsx           -- 群设置面板 (右栏)
│   │   ├── GroupMemberList.tsx         -- 群成员列表
│   │   ├── GroupAnnouncementList.tsx   -- 群公告列表
│   │   ├── CreateGroupModal.tsx        -- 创建群弹窗
│   │   ├── InviteMemberModal.tsx       -- 邀请成员弹窗
│   │   └── GroupRoleTag.tsx            -- 角色标签 (群主/管理员)
│   │
│   ├── common/
│   │   ├── Avatar.tsx                 -- 头像 (在线状态圆点)
│   │   ├── OnlineStatus.tsx           -- 在线状态指示器
│   │   ├── TypingIndicator.tsx        -- 正在输入指示器
│   │   ├── EmptyState.tsx             -- 空状态占位
│   │   ├── TimeLabel.tsx              -- 时间标签 (智能格式化)
│   │   └── UnreadBadge.tsx            -- 未读角标
│   │
│   └── search/
│       ├── GlobalSearch.tsx           -- 全局搜索
│       ├── MessageSearchResult.tsx    -- 消息搜索结果
│       └── ContactSearchResult.tsx    -- 联系人搜索结果
│
├── stores/
│   ├── useChatStore.ts                -- 聊天核心状态 (当前会话等)
│   ├── useConversationStore.ts        -- 会话列表状态
│   ├── useMessageStore.ts             -- 消息状态 (按conversationId分桶)
│   ├── useContactStore.ts             -- 通讯录状态
│   ├── useGroupStore.ts               -- 群组状态
│   ├── usePresenceStore.ts            -- 在线状态
│   └── useWebSocketStore.ts           -- WebSocket 连接状态
│
├── hooks/
│   ├── useWebSocket.ts                -- WebSocket 连接/重连/消息分发
│   ├── useMessages.ts                 -- 消息加载/发送/撤回
│   ├── useConversation.ts             -- 会话操作
│   ├── useVirtualScroll.ts            -- 虚拟滚动 Hook
│   ├── useTypingStatus.ts             -- 输入状态管理
│   ├── useFileUpload.ts               -- 文件上传 Hook
│   └── useMessageSearch.ts            -- 消息搜索 Hook
│
├── api/
│   ├── chatApi.ts                     -- REST API 基础封装
│   ├── friendApi.ts                   -- 好友 API
│   ├── groupApi.ts                    -- 群组 API
│   ├── messageApi.ts                  -- 消息 API
│   ├── searchApi.ts                   -- 搜索 API
│   └── fileApi.ts                     -- 文件上传 API
│
├── types/
│   ├── message.ts                     -- 消息类型定义
│   ├── conversation.ts                -- 会话类型定义
│   ├── contact.ts                     -- 联系人类型定义
│   ├── group.ts                       -- 群组类型定义
│   ├── websocket.ts                   -- WebSocket 帧类型定义
│   └── enums.ts                       -- 枚举常量
│
└── utils/
    ├── messageFormatter.ts            -- 消息格式化 (预览文本/时间显示)
    ├── fileHelper.ts                  -- 文件类型判断/大小格式化
    └── notificationHelper.ts          -- 浏览器通知
```

### 9.4 WebSocket 封装核心逻辑

```typescript
// hooks/useWebSocket.ts 核心逻辑

interface WebSocketOptions {
  url: string;
  token: string;
  deviceId: string;
  onMessage: (frame: WsFrame) => void;
  onConnectionChange: (connected: boolean) => void;
}

function useWebSocket(options: WebSocketOptions) {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectAttempt = useRef(0);
  const heartbeatTimer = useRef<number>();

  const connect = useCallback(() => {
    const ws = new WebSocket(
      `${options.url}?token=${options.token}&deviceId=${options.deviceId}`
    );

    ws.onopen = () => {
      reconnectAttempt.current = 0;
      options.onConnectionChange(true);
      startHeartbeat();
    };

    ws.onmessage = (event) => {
      const frame = JSON.parse(event.data) as WsFrame;
      if (frame.type === 'pong') return;
      options.onMessage(frame);
    };

    ws.onclose = () => {
      options.onConnectionChange(false);
      stopHeartbeat();
      scheduleReconnect();
    };

    wsRef.current = ws;
  }, [options]);

  // 心跳: 每25秒发 ping
  const startHeartbeat = () => {
    heartbeatTimer.current = window.setInterval(() => {
      send({ type: 'ping' });
    }, 25_000);
  };

  // 指数退避重连: 1s -> 2s -> 4s -> 8s -> 16s -> 30s(上限)
  const scheduleReconnect = () => {
    const delay = Math.min(1000 * 2 ** reconnectAttempt.current, 30_000);
    reconnectAttempt.current++;
    setTimeout(connect, delay);
  };

  const send = useCallback((frame: object) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(frame));
    }
  }, []);

  // 重连后增量同步
  const syncAfterReconnect = useCallback(() => {
    const conversations = getLastReceivedMessageIds(); // 从store读取
    send({ type: 'sync', conversations });
  }, [send]);

  return { connect, send, disconnect };
}
```

### 9.5 虚拟滚动（消息列表）

消息列表使用 `react-virtuoso` 实现反向虚拟滚动（最新消息在底部）：

- **反向加载**: 滚动到顶部触发 `startReached` 加载更多历史消息
- **自动滚动到底**: 新消息到达时 `followOutput="smooth"` 自动滚动（仅在底部附近时）
- **动态高度**: 每条消息高度不固定，virtuoso 自动测量
- **锚点保持**: 加载历史消息时通过 `firstItemIndex` 动态调整，保持当前滚动位置不跳动

```tsx
<Virtuoso
  ref={virtuosoRef}
  data={messages}
  firstItemIndex={firstItemIndex}
  initialTopMostItemIndex={messages.length - 1}
  followOutput="smooth"
  startReached={loadMoreHistory}
  itemContent={(index, message) => <MessageItem message={message} />}
  alignToBottom
  atBottomStateChange={setIsAtBottom}
/>
```

---

## 10. 安全设计

### 10.1 认证与授权

| 层面 | 方案 |
|------|------|
| HTTP API 鉴权 | 复用平台 JWT 双令牌机制（access 30min + refresh 7d），via `basebackend-jwt` |
| WebSocket 鉴权 | 握手阶段通过 `?token=<JWT>` 校验，`ChatAuthInterceptor` 鉴权失败拒绝升级 |
| 租户隔离 | `TenantContextHolder` 注入 tenantId，所有查询自动附加 `WHERE tenant_id = ?` |
| 操作权限 | 群操作校验角色（群主 > 管理员 > 成员），好友操作校验关系 |
| 会话权限 | 读写消息前校验用户是否为 `chat_conversation_member` 成员 |

### 10.2 XSS 防护

| 场景 | 策略 |
|------|------|
| 文本消息 | 入库前 HTML 实体转义，复用 `basebackend-security` 的 OWASP HTML Sanitizer |
| 链接 | 白名单过滤，移除 `javascript:` 协议 |
| 文件名 | 服务端重命名为 UUID，原始文件名仅存库展示 |
| 前端输出 | React JSX 默认转义，禁止 `dangerouslySetInnerHTML` |

### 10.3 敏感词过滤

| 项目 | 方案 |
|------|------|
| 算法 | DFA（Deterministic Finite Automaton）多模式匹配 |
| 词库 | 内置默认词库 + 租户自定义词库（管理后台维护） |
| 加载 | 应用启动时构建内存 Trie/DFA 自动机，支持 Redis Pub/Sub 热更新 |
| 策略 | `replace`（替换为 `***`）/ `reject`（拒绝发送）/ `audit`（标记审核） |
| 性能 | 预构建 DFA，单消息过滤 O(n)，n 为消息长度，耗时 < 1ms |

### 10.4 频率限制

复用平台 `basebackend-cache` 的 `@RateLimit` 注解，基于 Redis 滑动窗口计数。

| 操作 | 限制 | Key Pattern |
|------|------|-------------|
| 消息发送 | 30条/10秒 | `rate_limiter:chat:send:{userId}` |
| 文件上传 | 10次/60秒 | `rate_limiter:chat:upload:{userId}` |
| 好友申请 | 20次/小时 | `rate_limiter:chat:friend_req:{userId}` |
| 全局搜索 | 10次/10秒 | `rate_limiter:chat:search:{userId}` |
| WebSocket 帧 | 50帧/10秒 | 连接级别内存计数器，超限断开 |

### 10.5 其他安全措施

| 措施 | 说明 |
|------|------|
| 传输加密 | 全链路 TLS（HTTPS + WSS） |
| 文件扫描 | 上传文件通过 ClamAV 病毒扫描（异步，不阻塞上传） |
| SQL 注入 | MyBatis-Plus 参数化查询，禁止字符串拼接 SQL |
| 路径遍历 | 文件存储使用 UUID 命名，禁止用户指定存储路径 |
| 消息回溯 | 仅会话成员可查看历史消息，非成员返回 6002 |
| 撤回限制 | 仅消息发送者本人可撤回，限时 2 分钟 |

---

## 11. 性能设计

### 11.1 消息表分表策略

按 `conversation_id` 哈希取模分 16 张表（`chat_message_00` ~ `chat_message_15`）：

| 项目 | 方案 |
|------|------|
| 分表键 | `conversation_id` |
| 分表算法 | `conversation_id % 16` |
| 表数量 | 16 张: `chat_message_00` ~ `chat_message_15` |
| 路由 | MyBatis-Plus 拦截器根据 conversation_id 路由 |
| 优势 | 同会话消息在同一张表，历史查询无需跨表 |

选择哈希分表而非按时间分表的原因：同一会话的消息总是一起查询，哈希分表保证同会话消息在同一张表，避免跨表查询；相比按时间分表，不会出现热点表（最近时间段的表写入集中）。

### 11.2 缓存策略

| 数据 | 缓存位置 | TTL | 更新方式 |
|------|---------|-----|---------|
| 会话列表 | Redis ZSET | 不过期 | 消息到达时 ZADD 更新 score |
| 会话详情 | Redis HASH | 30min | 主动更新 + 过期重加载 |
| 群信息 | Redis HASH | 30min | 修改时主动失效 |
| 群成员列表 | Redis SET | 30min | 成员变动时主动失效 |
| 好友列表 | Redis SET | 30min | 好友变动时主动失效 |
| 黑名单 | Redis SET | 30min | 拉黑/取消时主动失效 |
| 未读计数 | Redis STRING | 不过期 | 消息到达 INCR，标记已读 SET 0 |
| 在线状态 | Redis SET + STRING | 不过期 | 上线 SADD/SET，下线 SREM/DEL |
| 消息去重 | Redis STRING | 5min | SET NX |
| 最近消息 | Caffeine 本地 | 5min | LRU 淘汰，最近 50 条/会话 |

### 11.3 热点群消息削峰（Redis Stream）

超大群（成员 > 100）的消息推送通过 Redis Stream 异步化：

```
发送方 ──→ Chat-API ──→ MySQL 持久化 ──→ ACK 返回发送方
                   │
                   └──→ XADD chat:msg:stream:{tenantId}:{conversationId}
                              │
                              ├──→ Consumer Group 1 (推送在线成员 0-499)
                              └──→ Consumer Group 2 (推送在线成员 500-999)
```

| 参数 | 值 |
|------|-----|
| Stream maxLen | ~10000（近似裁剪） |
| Consumer Group | 按群成员数动态分配，每 500 人一组 |
| 消费者 | 虚拟线程处理，每次批量拉取 10 条 |
| ACK | 推送成功后 XACK |
| 超时重试 | Pending 消息超 30s 未 ACK 自动重投 |

小群（成员 ≤ 100）直接遍历推送，不走 Stream。

### 11.4 连接层优化

| 策略 | 说明 |
|------|------|
| 虚拟线程 | Java 25 虚拟线程处理 WebSocket 消息分发，避免线程池耗尽 |
| 消息批量推送 | 同一会话 50ms 内的多条消息合并为一次推送 |
| 压缩 | WebSocket 帧启用 permessage-deflate 压缩 |
| 空闲回收 | 60s 无心跳的连接自动关闭 |
| 背压 | 单连接发送队列超过 1000 条时丢弃旧消息并通知客户端 |

### 11.5 数据库优化

| 策略 | 说明 |
|------|------|
| 批量写入 | 群消息的未读计数更新使用 `saveBatch` |
| 读写分离 | 消息写入走主库，历史查询走从库 |
| 连接池 | Druid，初始 10，最大 50，慢 SQL 监控 |
| 索引覆盖 | 会话列表查询使用覆盖索引避免回表 |
| 延迟回写 | 会话 `last_message_*` 字段 Redis 缓存，每 5 秒批量回写 MySQL |

---

## 12. 项目结构

### 12.1 后端 Maven 模块

```
basebackend-chat-api/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/basebackend/chat/
    │   │   ├── ChatApiApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── ChatWebSocketConfig.java          -- WebSocket 扩展配置
    │   │   │   ├── ChatProperties.java               -- 聊天服务配置属性
    │   │   │   ├── AsyncConfig.java                   -- 异步线程池配置
    │   │   │   └── SearchIndexInitializer.java        -- ES索引初始化
    │   │   │
    │   │   ├── controller/
    │   │   │   ├── ConversationController.java        -- 会话 REST API
    │   │   │   ├── MessageController.java             -- 消息 REST API
    │   │   │   ├── FriendController.java              -- 好友 REST API
    │   │   │   ├── GroupController.java                -- 群组 REST API
    │   │   │   ├── ChatSearchController.java          -- 搜索 REST API
    │   │   │   ├── ChatFileController.java            -- 文件上传 REST API
    │   │   │   └── PresenceController.java            -- 在线状态 REST API
    │   │   │
    │   │   ├── service/
    │   │   │   ├── ConversationService.java
    │   │   │   ├── MessageService.java
    │   │   │   ├── FriendService.java
    │   │   │   ├── GroupService.java
    │   │   │   ├── PresenceService.java
    │   │   │   ├── ChatSearchService.java
    │   │   │   ├── SensitiveWordService.java
    │   │   │   └── impl/
    │   │   │       ├── ConversationServiceImpl.java
    │   │   │       ├── MessageServiceImpl.java
    │   │   │       ├── FriendServiceImpl.java
    │   │   │       ├── GroupServiceImpl.java
    │   │   │       ├── PresenceServiceImpl.java
    │   │   │       ├── ChatSearchServiceImpl.java
    │   │   │       └── SensitiveWordServiceImpl.java
    │   │   │
    │   │   ├── ws/
    │   │   │   ├── ChatWsMessageHandler.java          -- 聊天WS消息处理器
    │   │   │   ├── ChatAuthInterceptor.java           -- JWT鉴权握手拦截器
    │   │   │   ├── ChatSessionManager.java            -- 扩展SessionManager(Redis多节点)
    │   │   │   ├── WsFrameDispatcher.java             -- 帧类型→Handler分发
    │   │   │   └── handler/
    │   │   │       ├── ChatFrameHandler.java           -- 聊天消息帧
    │   │   │       ├── ReadFrameHandler.java           -- 已读上报帧
    │   │   │       ├── TypingFrameHandler.java         -- 输入状态帧
    │   │   │       ├── RevokeFrameHandler.java         -- 撤回帧
    │   │   │       ├── SyncFrameHandler.java           -- 增量同步帧
    │   │   │       └── PresenceFrameHandler.java       -- 状态帧
    │   │   │
    │   │   ├── mapper/
    │   │   │   ├── ChatFriendMapper.java
    │   │   │   ├── ChatFriendGroupMapper.java
    │   │   │   ├── ChatFriendRequestMapper.java
    │   │   │   ├── ChatBlacklistMapper.java
    │   │   │   ├── ChatConversationMapper.java
    │   │   │   ├── ChatConversationMemberMapper.java
    │   │   │   ├── ChatMessageMapper.java
    │   │   │   ├── ChatMessageReadMapper.java
    │   │   │   ├── ChatGroupMapper.java
    │   │   │   ├── ChatGroupMemberMapper.java
    │   │   │   └── ChatGroupAnnouncementMapper.java
    │   │   │
    │   │   ├── entity/
    │   │   │   ├── ChatFriend.java
    │   │   │   ├── ChatFriendGroup.java
    │   │   │   ├── ChatFriendRequest.java
    │   │   │   ├── ChatBlacklist.java
    │   │   │   ├── ChatConversation.java
    │   │   │   ├── ChatConversationMember.java
    │   │   │   ├── ChatMessage.java
    │   │   │   ├── ChatMessageRead.java
    │   │   │   ├── ChatGroup.java
    │   │   │   ├── ChatGroupMember.java
    │   │   │   └── ChatGroupAnnouncement.java
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── SendMessageRequest.java
    │   │   │   │   ├── ForwardMessageRequest.java
    │   │   │   │   ├── FriendRequestDTO.java
    │   │   │   │   ├── CreateGroupRequest.java
    │   │   │   │   ├── UpdateGroupRequest.java
    │   │   │   │   └── MessageSearchRequest.java
    │   │   │   └── response/
    │   │   │       ├── ConversationVO.java
    │   │   │       ├── MessageVO.java
    │   │   │       ├── FriendVO.java
    │   │   │       ├── GroupVO.java
    │   │   │       ├── GroupMemberVO.java
    │   │   │       ├── MessageSearchResultVO.java
    │   │   │       └── PresenceVO.java
    │   │   │
    │   │   ├── enums/
    │   │   │   ├── ConversationType.java              -- PRIVATE(1), GROUP(2)
    │   │   │   ├── MessageType.java                   -- TEXT(1)..MERGED_FORWARD(11)
    │   │   │   ├── MessageStatus.java                 -- SENDING(0)..BLOCKED(4)
    │   │   │   ├── GroupRole.java                      -- MEMBER(0), ADMIN(1), OWNER(2)
    │   │   │   ├── FriendSource.java                  -- SEARCH(0)..QR_CODE(3)
    │   │   │   ├── FriendRequestStatus.java           -- PENDING(0)..EXPIRED(3)
    │   │   │   ├── UserOnlineStatus.java              -- ONLINE, OFFLINE, BUSY
    │   │   │   └── ChatErrorCode.java                 -- implements ErrorCode, 6000-6999
    │   │   │
    │   │   ├── feign/
    │   │   │   └── FileServiceClient.java             -- file-service Feign客户端
    │   │   │
    │   │   └── util/
    │   │       ├── ChatIdGenerator.java               -- Snowflake ID生成器
    │   │       └── MessagePreviewUtil.java            -- 消息预览文本生成工具
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── bootstrap.yml                          -- Nacos配置
    │       ├── sensitive-words.txt                     -- 默认敏感词库
    │       ├── db/migration/
    │       │   ├── V1.0__chat_init.sql                -- 全量建表
    │       │   └── V1.1__chat_indexes.sql             -- 索引补充
    │       └── mapper/
    │           ├── ChatMessageMapper.xml
    │           └── ChatConversationMapper.xml
    │
    └── test/
        └── java/com/basebackend/chat/
            ├── service/
            │   ├── MessageServiceTest.java
            │   ├── ConversationServiceTest.java
            │   ├── FriendServiceTest.java
            │   └── GroupServiceTest.java
            ├── controller/
            │   └── MessageControllerTest.java
            └── ws/
                └── ChatWsMessageHandlerTest.java
```

### 12.2 前端目录（在 basebackend-admin-web 中）

```
basebackend-admin-web/src/
├── features/chat/                    -- 见 9.3 节完整组件结构
├── api/chat/                         -- REST API 函数
├── router/
│   └── chatRoutes.tsx                -- 聊天路由注册
└── i18n/locales/
    ├── zh-CN/chat.json               -- 中文语言包
    └── en-US/chat.json               -- 英文语言包
```

---

## 13. 开发计划

### 阶段一: MVP — 核心聊天

**目标**: 实现基础 1v1 聊天和消息收发

| 任务 | 内容 |
|------|------|
| 基础设施 | Maven 模块搭建、Flyway 建表、配置文件、Nacos 注册 |
| 数据层 | 全部 Entity / Mapper 创建，MyBatis-Plus 配置 |
| WebSocket | 扩展 basebackend-websocket，实现 JWT 鉴权握手、ChatWsMessageHandler、帧分发 |
| 私聊消息 | 文本消息发送/接收、消息持久化、ACK 确认 |
| 会话管理 | 会话列表、创建会话、未读计数、会话排序 |
| 在线状态 | 上线/下线检测、Redis 状态存储、好友状态广播 |
| 历史消息 | 游标分页加载历史（beforeId） |
| 前端 MVP | 三栏布局、会话列表、消息面板、输入框、虚拟滚动 |

**交付物**: 可运行的 1v1 文本聊天，含前后端

### 阶段二: 社交功能

**目标**: 好友系统、群聊、消息交互

| 任务 | 内容 |
|------|------|
| 好友系统 | 好友申请/同意/拒绝、好友列表、备注、分组、黑名单 |
| 群聊 | 创建群/解散群、邀请/踢人、群消息收发、群成员管理 |
| 群角色 | 群主/管理员/成员权限划分 |
| 群公告 | 发布/编辑/删除/确认 |
| 禁言 | 全体禁言、单人禁言（含时长） |
| 消息撤回 | 2 分钟内撤回、通知所有接收方 |
| 消息转发 | 逐条转发、合并转发 |
| 已读回执 | 私聊已读状态、群聊已读计数 |
| @提醒 | @指定成员、@所有人 |
| 消息引用 | 引用回复特定消息 |
| 正在输入 | 输入状态检测和显示 |
| 前端通讯录 | 好友列表、好友申请、群组管理、黑名单页面 |

**交付物**: 完整社交聊天功能

### 阶段三: 多媒体与搜索

**目标**: 富媒体消息、文件处理、全文搜索

| 任务 | 内容 |
|------|------|
| 图片消息 | 上传/缩略图/预览/发送 |
| 文件消息 | 上传/下载/文件类型限制 |
| 语音消息 | 录音/上传/播放 |
| 视频消息 | 上传/封面截取/播放 |
| 位置消息 | 坐标发送/地图展示 |
| 名片消息 | 用户名片分享 |
| 表情消息 | 表情面板/GIF 表情 |
| 全文搜索 | ES 索引创建、消息索引同步、搜索 API |
| 搜索 UI | 全局搜索页、按会话/联系人/时间筛选 |
| 文件管理 | 聊天文件聚合列表、按类型筛选 |

**交付物**: 富媒体聊天 + 消息搜索

### 阶段四: 高级功能与优化

**目标**: 性能优化、运维能力、高级特性

| 任务 | 内容 |
|------|------|
| 消息分表 | 按 conversation_id 分 64 张表，历史数据迁移 |
| Redis Stream | 大群消息异步推送、Consumer Group 消费 |
| 多节点 WS | Redis Pub/Sub 跨节点消息推送、会话路由表 |
| 敏感词 | DFA 过滤引擎、租户自定义词库、热更新 |
| 消息审计 | 操作日志、管理员后台查看聊天记录 |
| 数据导出 | 聊天记录导出（CSV/PDF） |
| 浏览器通知 | Web Notification API 集成 |
| 性能调优 | 消息批量推送、连接背压、数据库读写分离 |
| 监控告警 | Prometheus 指标（在线数/消息吞吐/延迟）、Grafana 面板 |
| 压测 | WebSocket 并发连接压测、消息吞吐压测 |

**交付物**: 生产就绪的完整聊天系统

---

## 14. 技术决策记录

### ADR-001: 写扩散 vs 读扩散

**决策**: 采用**混合方案** — 消息存储不扩散（一条消息存一份），未读计数和推送采用写扩散。

**背景**: 群消息处理有两种经典模型：
- **写扩散 (fan-out-on-write)**: 消息写入时为每个接收者生成一份副本
- **读扩散 (fan-out-on-read)**: 消息仅存一份，读取时聚合查询

**分析**:

| 维度 | 纯写扩散 | 纯读扩散 | 本方案（混合） |
|------|---------|---------|-------------|
| 存储开销 | 高（N 份） | 低（1 份） | 低（1 份） |
| 写入延迟 | 高 | 低 | 中（Redis INCR 扩散） |
| 读取性能 | 高 | 需聚合计算 | 高 |
| 未读计数 | 天然准确 | 需额外计算 | Redis INCR 准确 |
| 实现复杂度 | 中 | 高 | 中 |

**理由**:
1. 消息体复制 N 份浪费存储，500 人群发一条消息存 500 条不可接受
2. 消息按 `conversation_id` 聚合存储，群内所有消息在同一分表，历史查询高效
3. 未读计数通过 Redis PIPELINE 批量 INCR 实现写扩散，成本低且原子准确
4. 在线推送通过遍历群成员实现，大群通过 Redis Stream 异步化削峰

### ADR-002: 消息 ID 策略 — Snowflake

**决策**: 使用 **Snowflake** 算法生成消息 ID（`chat_message.id`）。

**备选方案**:

| 方案 | 优点 | 缺点 |
|------|------|------|
| MySQL AUTO_INCREMENT | 简单 | 分表后不连续，无法跨表排序 |
| UUID | 全局唯一 | 无序，不能用于排序和游标分页 |
| **Snowflake** | 全局唯一 + 趋势递增 + 含时间 | 依赖时钟同步 |
| Redis INCR per conv | 连续递增 | 并发瓶颈，Redis 故障影响 |

**理由**:
1. 消息 ID 同时作为主键和排序序号，必须趋势递增
2. Long 类型 8 字节，B+Tree 索引效率高于 UUID 的 16 字节
3. ID 编码了毫秒级时间戳，`ORDER BY id DESC` 等效于按时间排序
4. 支持多节点，workerId 从 Nacos 实例序号或环境变量获取
5. 游标分页 `WHERE id < #{beforeId} ORDER BY id DESC LIMIT 30` 性能优异

### ADR-003: 会话模型 — 共享会话

**决策**: 采用**共享会话**模型（`chat_conversation` + `chat_conversation_member`）。

**备选方案**:
- **独立收件箱**: 每个用户一份会话记录（N 条 conversation），天然支持个性化
- **共享会话**: 一个会话一条记录，通过 member 表存个性化设置

**理由**:
1. 共享会话节省存储，私聊仅 1 条 conversation + 2 条 member
2. 群聊场景，会话级信息（最后消息/成员数）只维护一处，一致性好
3. 个性化设置（置顶/免打扰/未读数/草稿）存 `conversation_member` 表
4. 查询会话列表 JOIN member 表即可获取个性化视图

### ADR-004: 私聊已读 vs 群聊已读

**决策**: 私聊和群聊采用**不同的已读策略**。

| 场景 | 策略 | 存储 |
|------|------|------|
| 私聊 | 整体标记式 | `conversation_member.last_read_message_id` |
| 群聊 | 逐条记录式 | `chat_message_read` 表 |

**理由**:
1. 私聊只有两人，"我已读到第 N 条"即可标记全部已读，无需逐条记录
2. 群聊需显示"已读 20 人 / 未读 15 人"，需逐条记录谁读了
3. `chat_message_read` 数据量大（每条消息 × 已读人数），超 7 天可定期归档

### ADR-005: WebSocket 多节点路由

**决策**: 使用 **Redis Pub/Sub** 实现跨节点消息推送。

**方案**:

```
Node-A (用户A在此)          Redis             Node-B (用户B在此)
       │                      │                        │
       │── PUBLISH ─────────→ │                        │
       │   chat:push:{tenant} │                        │
       │   {to:B, frame:{..}} │── SUBSCRIBE ─────────→│
       │                      │   chat:push:{tenant}   │
       │                      │                        │── 推送给用户B
```

1. 每个节点启动时 SUBSCRIBE `chat:push:{tenantId}` 频道
2. 发送消息时先查 `chat:session:{tenantId}:{userId}` 确定目标节点
3. 目标在本节点则直接推送，否则 PUBLISH 到 Redis 频道
4. 目标节点收到后查找本地 session 推送

### ADR-006: 消息分页 — 游标分页

**决策**: 使用**游标分页**（Cursor-based Pagination），不使用偏移分页。

```sql
-- 游标分页: O(1) 索引定位
SELECT * FROM chat_message
WHERE conversation_id = ? AND id < ? AND deleted = 0
ORDER BY id DESC LIMIT 30;

-- 偏移分页: O(N) 扫描跳过 (不采用)
SELECT * FROM chat_message
WHERE conversation_id = ?
ORDER BY id DESC LIMIT 30 OFFSET 10000;
```

**理由**:
1. 消息列表是"加载更多"场景，非跳页场景
2. Snowflake ID 趋势递增，`WHERE id < beforeId` 走 B+Tree 索引
3. 偏移分页随 OFFSET 增大需扫描并跳过大量行，性能线性恶化
4. 前端只需传 `beforeId`（上一页最后一条 ID），接口简洁直观

### ADR-007: 敏感词过滤 — 应用层 DFA

**决策**: 应用层 DFA 内存过滤，不依赖外部服务。

**备选方案**:

| 方案 | 优点 | 缺点 |
|------|------|------|
| **DFA 内存过滤** | 延迟 <1ms、无外部依赖 | 内存 ~50MB/百万词 |
| AC 自动机 | 性能相当 | 实现更复杂 |
| 外部审核服务 | 功能强（图片/语义） | 引入延迟和网络依赖 |
| 正则表达式 | 简单灵活 | 大词库性能差 |

**理由**:
1. 消息过滤延迟必须极低，不能影响发送体验
2. DFA 预构建后匹配 O(n)，n 为消息长度，与词库大小无关
3. 词库 1-10 万条，内存占用 5-50MB，完全可接受
4. 通过 Redis Pub/Sub 通知各节点热更新词库，重建 DFA 自动机

---

*文档结束*
