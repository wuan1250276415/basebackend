package com.basebackend.admin.dto.nacos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 配置发布DTO
 */
@Data
public class ConfigPublishDTO {

    /**
     * 配置ID
     */
    @NotNull(message = "配置ID不能为空")
    private Long configId;

    /**
     * 是否强制发布（忽略关键配置检查）
     */
    private Boolean force;

    /**
     * 目标实例列表（可选，用于指定实例发布）
     */
    private String targetInstances;
}
