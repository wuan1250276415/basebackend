-- ============================================================
-- 聊天系统初始化 DDL
-- 模块: basebackend-chat-api
-- 版本: V1
-- ============================================================

-- 1. 好友关系表 — 单向存储，A加B产生两条记录(A->B, B->A)
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
    `create_by`     BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`     BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_friend` (`tenant_id`, `user_id`, `friend_id`, `deleted`),
    KEY `idx_tenant_friend` (`tenant_id`, `friend_id`),
    KEY `idx_tenant_user_status` (`tenant_id`, `user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='好友关系表';

-- 2. 好友分组表
CREATE TABLE `chat_friend_group` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`     BIGINT       NOT NULL COMMENT '租户ID',
    `user_id`       BIGINT       NOT NULL COMMENT '所属用户ID',
    `name`          VARCHAR(64)  NOT NULL COMMENT '分组名称',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序值 (升序)',
    `is_default`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认分组: 0-否 1-是',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`     BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='好友分组表';

-- 3. 黑名单表 — 单向拉黑
CREATE TABLE `chat_blacklist` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `tenant_id`     BIGINT       NOT NULL COMMENT '租户ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID (发起拉黑方)',
    `blocked_id`    BIGINT       NOT NULL COMMENT '被拉黑用户ID',
    `reason`        VARCHAR(256) DEFAULT NULL COMMENT '拉黑原因',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`     BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_blocked` (`tenant_id`, `user_id`, `blocked_id`, `deleted`),
    KEY `idx_tenant_blocked` (`tenant_id`, `blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='黑名单表';

-- 4. 会话表 — 每个私聊/群聊对应一条会话记录
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
    `create_by`            BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`            BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_type_target` (`tenant_id`, `type`, `target_id`),
    KEY `idx_last_msg_time` (`tenant_id`, `last_message_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='会话表';

-- 5. 会话成员表 — 每个用户在每个会话中的独立设置
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
    `create_by`             BIGINT        DEFAULT NULL COMMENT '创建人',
    `update_by`             BIGINT        DEFAULT NULL COMMENT '更新人',
    `deleted`               TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conv_user` (`tenant_id`, `conversation_id`, `user_id`, `deleted`),
    KEY `idx_tenant_user_pinned` (`tenant_id`, `user_id`, `is_pinned` DESC, `deleted`),
    KEY `idx_tenant_user_hidden` (`tenant_id`, `user_id`, `is_hidden`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='会话成员表';

-- 6. 消息表 — 按conversation_id写入, ID即为消息序号
CREATE TABLE `chat_message` (
    `id`                           BIGINT        NOT NULL COMMENT '消息ID (Snowflake, 趋势递增, 兼做排序序号)',
    `tenant_id`                    BIGINT        NOT NULL COMMENT '租户ID',
    `conversation_id`              BIGINT        NOT NULL COMMENT '会话ID',
    `sender_id`                    BIGINT        NOT NULL COMMENT '发送者用户ID',
    `sender_name`                  VARCHAR(64)   NOT NULL COMMENT '发送者昵称 (冗余快照)',
    `sender_avatar`                VARCHAR(512)  DEFAULT NULL COMMENT '发送者头像 (冗余快照)',
    `type`                         TINYINT       NOT NULL COMMENT '消息类型: 1-文本 2-图片 3-文件 4-语音 5-视频 6-位置 7-名片 8-表情 9-系统通知 10-撤回 11-合并转发',
    `content`                      TEXT          DEFAULT NULL COMMENT '消息内容 (文本消息为纯文本, 其他类型为JSON)',
    `reply_to_msg_id`              BIGINT        DEFAULT NULL COMMENT '引用回复的消息ID',
    `forward_from_msg_id`          BIGINT        DEFAULT NULL COMMENT '转发来源消息ID',
    `forward_from_conversation_id` BIGINT        DEFAULT NULL COMMENT '转发来源会话ID',
    `extra`                        JSON          DEFAULT NULL COMMENT '扩展信息 JSON (宽度/高度/时长/文件名/大小等)',
    `quote_message_id`             BIGINT        DEFAULT NULL COMMENT '引用消息ID (回复某条消息)',
    `at_user_ids`                  JSON          DEFAULT NULL COMMENT '@用户ID列表, JSON数组, ["all"]表示@所有人',
    `client_msg_id`                VARCHAR(64)   DEFAULT NULL COMMENT '客户端消息ID (用于去重)',
    `send_time`                    DATETIME(3)   NOT NULL COMMENT '发送时间 (毫秒精度)',
    `status`                       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0-发送中 1-已发送 2-已撤回 3-审核中 4-已屏蔽',
    `revoke_time`                  DATETIME      DEFAULT NULL COMMENT '撤回时间',
    `create_time`                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`                    BIGINT        DEFAULT NULL COMMENT '创建人',
    `update_by`                    BIGINT        DEFAULT NULL COMMENT '更新人',
    `deleted`                      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_conv_sendtime` (`conversation_id`, `send_time` DESC),
    KEY `idx_tenant_conv_id` (`tenant_id`, `conversation_id`, `id` DESC),
    KEY `idx_sender` (`tenant_id`, `sender_id`, `send_time` DESC),
    KEY `idx_client_msg` (`client_msg_id`),
    KEY `idx_reply_to` (`reply_to_msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='消息表';

-- 7. 消息已读状态表 — 群聊已读回执
CREATE TABLE `chat_message_read` (
    `id`              BIGINT    NOT NULL COMMENT '主键ID',
    `tenant_id`       BIGINT    NOT NULL COMMENT '租户ID',
    `conversation_id` BIGINT    NOT NULL COMMENT '会话ID',
    `message_id`      BIGINT    NOT NULL COMMENT '消息ID',
    `user_id`         BIGINT    NOT NULL COMMENT '已读用户ID',
    `read_time`       DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    `create_time`     DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       BIGINT    DEFAULT NULL COMMENT '创建人',
    `update_by`       BIGINT    DEFAULT NULL COMMENT '更新人',
    `deleted`         TINYINT   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_user` (`tenant_id`, `message_id`, `user_id`),
    KEY `idx_conv_user` (`tenant_id`, `conversation_id`, `user_id`, `message_id` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='消息已读状态表';

-- 8. 群组信息表
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
    `create_by`       BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`       BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_owner` (`tenant_id`, `owner_id`),
    KEY `idx_tenant_name` (`tenant_id`, `name`),
    KEY `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='群组信息表';

-- 9. 群成员表
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
    `create_by`        BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`        BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`tenant_id`, `group_id`, `user_id`, `deleted`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_group_role` (`group_id`, `role` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='群成员表';

-- 10. 群公告表
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
    `create_by`     BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`     BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_group_pinned` (`tenant_id`, `group_id`, `is_pinned` DESC, `publish_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='群公告表';

-- 11. 好友申请表
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
    `create_by`     BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`     BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_to_user` (`tenant_id`, `to_user_id`, `status`),
    KEY `idx_from_user` (`tenant_id`, `from_user_id`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='好友申请表';

-- 12. 合并转发消息表
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
    `create_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`                BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`                BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`                  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_forward_id` (`forward_id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='合并转发消息表';
