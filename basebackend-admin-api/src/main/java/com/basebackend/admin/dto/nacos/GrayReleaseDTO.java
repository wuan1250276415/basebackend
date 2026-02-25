package com.basebackend.admin.dto.nacos;

import jakarta.validation.constraints.NotNull;

/**
 * 灰度发布DTO
 */
public record GrayReleaseDTO(
    /** 灰度发布ID */
    Long id,
    /** 配置ID */
    @NotNull(message = "配置ID不能为空") Long configId,
    /** 灰度策略类型（ip/percentage/label） */
    @NotNull(message = "灰度策略类型不能为空") String strategyType,
    /** 目标实例列表（IP列表，逗号分隔） */
    String targetInstances,
    /** 灰度百分比（0-100） */
    Integer percentage,
    /** 实例标签（JSON格式） */
    String labels,
    /** 灰度配置内容 */
    String grayContent
) {}
