package com.basebackend.admin.dto.nacos;

import jakarta.validation.constraints.NotBlank;

/**
 * Nacos配置DTO
 */
public record NacosConfigDTO(
    /** 配置ID */
    Long id,
    /** 配置Data ID */
    @NotBlank(message = "配置Data ID不能为空") String dataId,
    /** 配置分组 */
    String groupName,
    /** 命名空间 */
    String namespace,
    /** 配置内容 */
    @NotBlank(message = "配置内容不能为空") String content,
    /** 配置类型 */
    String type,
    /** 环境 */
    String environment,
    /** 租户ID */
    String tenantId,
    /** 应用ID */
    Long appId,
    /** 配置状态 */
    String status,
    /** 是否关键配置 */
    Boolean isCritical,
    /** 发布类型 */
    String publishType,
    /** 配置描述 */
    String description
) {}
