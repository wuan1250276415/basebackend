package com.basebackend.admin.dto.nacos;

/**
 * 服务实例DTO
 */
public record ServiceInstanceDTO(
    /** 服务名 */
    String serviceName,
    /** 分组名 */
    String groupName,
    /** 集群名 */
    String clusterName,
    /** 实例IP */
    String ip,
    /** 实例端口 */
    Integer port,
    /** 权重 */
    Double weight,
    /** 是否健康 */
    Boolean healthy,
    /** 是否启用 */
    Boolean enabled,
    /** 元数据（JSON字符串） */
    String metadata,
    /** 实例ID */
    String instanceId
) {}
