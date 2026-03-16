-- =============================================
-- 商城交易域初始化脚本
-- V1.0__mall_trade_init.sql
-- =============================================

-- 1. 购物车主表
CREATE TABLE mall_cart (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    checked_all     TINYINT         DEFAULT 0 COMMENT '是否全选: 0=否 1=是',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_tenant_user (tenant_id, user_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城购物车';

-- 2. 订单主表
CREATE TABLE mall_order (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    order_no        VARCHAR(64)     NOT NULL COMMENT '订单号',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    order_status    VARCHAR(32)     NOT NULL COMMENT '订单状态',
    total_amount    DECIMAL(10,2)   NOT NULL COMMENT '订单总金额',
    pay_amount      DECIMAL(10,2)   NOT NULL COMMENT '实付金额',
    pay_status      VARCHAR(32)     NOT NULL COMMENT '支付状态',
    remark          VARCHAR(500)    DEFAULT '' COMMENT '买家备注',
    submit_time     DATETIME        COMMENT '下单时间',
    pay_time        DATETIME        COMMENT '支付时间',
    close_time      DATETIME        COMMENT '关单时间',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_order_no (order_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_user (user_id),
    INDEX idx_order_status (order_status),
    INDEX idx_pay_status (pay_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城订单';

-- 3. 订单明细表
CREATE TABLE mall_order_item (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(64)     NOT NULL COMMENT '订单号',
    sku_id          BIGINT          NOT NULL COMMENT 'SKU ID',
    sku_name        VARCHAR(200)    NOT NULL COMMENT 'SKU名称',
    unit_price      DECIMAL(10,2)   NOT NULL COMMENT '单价',
    quantity        INT             NOT NULL COMMENT '数量',
    line_amount     DECIMAL(10,2)   NOT NULL COMMENT '行总金额',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_order (order_id),
    INDEX idx_order_no (order_no),
    INDEX idx_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城订单明细';
