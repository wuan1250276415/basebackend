package com.basebackend.admin.dto.nacos;

/**
 * Nacos配置查询DTO
 */
public record NacosConfigQueryDTO(
    /** 配置Data ID（模糊查询） */
    String dataId,
    /** 配置分组 */
    String groupName,
    /** 命名空间 */
    String namespace,
    /** 环境 */
    String environment,
    /** 租户ID */
    String tenantId,
    /** 应用ID */
    Long appId,
    /** 配置状态 */
    String status,
    /** 配置类型 */
    String type,
    /** 是否关键配置 */
    Boolean isCritical,
    /** 页码 */
    Integer pageNum,
    /** 每页大小 */
    Integer pageSize
) {}
