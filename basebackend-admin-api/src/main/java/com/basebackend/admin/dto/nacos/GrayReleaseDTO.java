package com.basebackend.admin.dto.nacos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 灰度发布DTO
 */
@Data
public class GrayReleaseDTO {

    /**
     * 灰度发布ID
     */
    private Long id;

    /**
     * 配置ID
     */
    @NotNull(message = "配置ID不能为空")
    private Long configId;

    /**
     * 灰度策略类型（ip/percentage/label）
     */
    @NotNull(message = "灰度策略类型不能为空")
    private String strategyType;

    /**
     * 目标实例列表（IP列表，逗号分隔）
     */
    private String targetInstances;

    /**
     * 灰度百分比（0-100）
     */
    private Integer percentage;

    /**
     * 实例标签（JSON格式）
     */
    private String labels;

    /**
     * 灰度配置内容
     */
    private String grayContent;
}
