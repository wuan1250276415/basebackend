package com.basebackend.admin.dto.nacos;

import lombok.Data;

/**
 * 服务实例DTO
 */
@Data
public class ServiceInstanceDTO {

    /**
     * 服务名
     */
    private String serviceName;

    /**
     * 分组名
     */
    private String groupName;

    /**
     * 集群名
     */
    private String clusterName;

    /**
     * 实例IP
     */
    private String ip;

    /**
     * 实例端口
     */
    private Integer port;

    /**
     * 权重
     */
    private Double weight;

    /**
     * 是否健康
     */
    private Boolean healthy;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 元数据（JSON字符串）
     */
    private String metadata;

    /**
     * 实例ID
     */
    private String instanceId;
}
