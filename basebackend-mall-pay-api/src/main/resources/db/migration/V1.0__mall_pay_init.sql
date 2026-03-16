-- =============================================
-- 商城支付域初始化脚本
-- V1.0__mall_pay_init.sql
-- =============================================

-- 1. 支付单主表
CREATE TABLE mall_payment (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    pay_no              VARCHAR(64)     NOT NULL COMMENT '支付单号',
    order_id            BIGINT          NOT NULL COMMENT '订单ID',
    order_no            VARCHAR(64)     NOT NULL COMMENT '订单号',
    pay_channel         VARCHAR(32)     NOT NULL COMMENT '支付渠道',
    pay_status          VARCHAR(32)     NOT NULL COMMENT '支付状态',
    pay_amount          DECIMAL(10,2)   NOT NULL COMMENT '支付金额',
    third_party_trade_no VARCHAR(128)   DEFAULT '' COMMENT '三方支付流水号',
    order_items_json    JSON            COMMENT '订单商品快照(JSON)',
    paid_time           DATETIME        COMMENT '支付成功时间',
    expire_time         DATETIME        COMMENT '支付过期时间',
    create_by           BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by           BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_pay_no (pay_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_order_no (order_no),
    INDEX idx_pay_status (pay_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城支付单';

-- 2. 支付回调日志
CREATE TABLE mall_payment_callback_log (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    pay_no              VARCHAR(64)     NOT NULL COMMENT '支付单号',
    pay_channel         VARCHAR(32)     NOT NULL COMMENT '支付渠道',
    callback_payload    JSON            COMMENT '回调原始报文',
    sign_verified       TINYINT         DEFAULT 0 COMMENT '签名校验结果: 0=失败 1=成功',
    process_status      VARCHAR(32)     DEFAULT 'INIT' COMMENT '处理状态',
    process_message     VARCHAR(500)    DEFAULT '' COMMENT '处理结果说明',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_pay_no (pay_no),
    INDEX idx_channel (pay_channel),
    INDEX idx_process_status (process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付回调日志';

-- 3. 退款单
CREATE TABLE mall_refund (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    refund_no           VARCHAR(64)     NOT NULL COMMENT '退款单号',
    pay_no              VARCHAR(64)     NOT NULL COMMENT '支付单号',
    order_no            VARCHAR(64)     NOT NULL COMMENT '订单号',
    refund_status       VARCHAR(32)     NOT NULL COMMENT '退款状态',
    refund_amount       DECIMAL(10,2)   NOT NULL COMMENT '退款金额',
    reason              VARCHAR(500)    DEFAULT '' COMMENT '退款原因',
    third_party_refund_no VARCHAR(128)  DEFAULT '' COMMENT '三方退款流水号',
    refund_time         DATETIME        COMMENT '退款成功时间',
    create_by           BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by           BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_refund_no (refund_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_pay_no (pay_no),
    INDEX idx_order_no (order_no),
    INDEX idx_refund_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城退款单';
