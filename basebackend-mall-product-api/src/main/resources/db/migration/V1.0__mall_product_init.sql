-- =============================================
-- 商城商品域初始化脚本
-- V1.0__mall_product_init.sql
-- =============================================

-- 1. 商品类目
CREATE TABLE mall_category (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    category_name   VARCHAR(100)    NOT NULL COMMENT '类目名称',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父类目ID',
    sort_order      INT             DEFAULT 0 COMMENT '排序',
    status          TINYINT         DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_parent (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城类目';

-- 2. SPU 主表
CREATE TABLE mall_spu (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    category_id     BIGINT          NOT NULL COMMENT '类目ID',
    spu_name        VARCHAR(200)    NOT NULL COMMENT 'SPU名称',
    brand_name      VARCHAR(100)    DEFAULT '' COMMENT '品牌名称',
    sale_status     TINYINT         DEFAULT 0 COMMENT '销售状态: 0=下架 1=上架',
    description     VARCHAR(1000)   DEFAULT '' COMMENT '商品描述',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_category (category_id),
    INDEX idx_sale_status (sale_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城SPU';

-- 3. SKU 明细表
CREATE TABLE mall_sku (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    spu_id          BIGINT          NOT NULL COMMENT 'SPU ID',
    sku_code        VARCHAR(64)     NOT NULL COMMENT 'SKU编码',
    sku_name        VARCHAR(200)    NOT NULL COMMENT 'SKU名称',
    sale_price      DECIMAL(10,2)   NOT NULL COMMENT '销售价',
    market_price    DECIMAL(10,2)   DEFAULT 0.00 COMMENT '市场价',
    stock_quantity  INT             DEFAULT 0 COMMENT '库存数量',
    lock_quantity   INT             DEFAULT 0 COMMENT '锁定库存',
    sale_status     TINYINT         DEFAULT 0 COMMENT '销售状态: 0=下架 1=上架',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_sku_code (sku_code),
    INDEX idx_tenant (tenant_id),
    INDEX idx_spu (spu_id),
    INDEX idx_sale_status (sale_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商城SKU';
