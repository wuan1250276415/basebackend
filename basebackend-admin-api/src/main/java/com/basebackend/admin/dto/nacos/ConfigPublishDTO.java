package com.basebackend.admin.dto.nacos;

import jakarta.validation.constraints.NotNull;

/**
 * 配置发布DTO
 */
public record ConfigPublishDTO(
    /** 配置ID */
    @NotNull(message = "配置ID不能为空") Long configId,
    /** 是否强制发布（忽略关键配置检查） */
    Boolean force,
    /** 目标实例列表（可选，用于指定实例发布） */
    String targetInstances
) {}
