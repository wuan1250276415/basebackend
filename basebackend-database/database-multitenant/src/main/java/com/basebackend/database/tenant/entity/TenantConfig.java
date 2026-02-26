package com.basebackend.database.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户配置实体
 * 存储租户的配置信息，包括隔离模式、数据源等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_config")
public class TenantConfig extends BaseEntity {
    
    /**
     * 租户 ID（唯一标识）
     */
    private String tenantId;
    
    /**
     * 租户名称
     */
    private String tenantName;
    
    /**
     * 隔离模式
     * SHARED_DB: 共享数据库（通过 tenant_id 字段隔离）
     * SEPARATE_DB: 独立数据库（每个租户使用独立的数据库）
     * SEPARATE_SCHEMA: 独立 Schema（每个租户使用独立的 Schema）
     */
    private String isolationMode;
    
    /**
     * 数据源键（独立数据库模式使用）
     * 对应动态数据源中的数据源名称
     */
    private String dataSourceKey;
    
    /**
     * Schema 名称（独立 Schema 模式使用）
     */
    private String schemaName;
    
    /**
     * 状态
     * ACTIVE: 激活
     * INACTIVE: 停用
     */
    private String status;
    
    /**
     * 备注
     */
    private String remark;
}
