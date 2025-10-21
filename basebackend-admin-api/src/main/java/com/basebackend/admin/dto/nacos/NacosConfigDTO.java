package com.basebackend.admin.dto.nacos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Nacos配置DTO
 */
@Data
public class NacosConfigDTO {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 配置Data ID
     */
    @NotBlank(message = "配置Data ID不能为空")
    private String dataId;

    /**
     * 配置分组
     */
    private String groupName;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 配置内容
     */
    @NotBlank(message = "配置内容不能为空")
    private String content;

    /**
     * 配置类型
     */
    private String type;

    /**
     * 环境
     */
    private String environment;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 配置状态
     */
    private String status;

    /**
     * 是否关键配置
     */
    private Boolean isCritical;

    /**
     * 发布类型
     */
    private String publishType;

    /**
     * 配置描述
     */
    private String description;
}
