package com.basebackend.admin.dto.nacos;

import lombok.Data;

/**
 * Nacos配置查询DTO
 */
@Data
public class NacosConfigQueryDTO {

    /**
     * 配置Data ID（模糊查询）
     */
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
     * 配置类型
     */
    private String type;

    /**
     * 是否关键配置
     */
    private Boolean isCritical;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}
