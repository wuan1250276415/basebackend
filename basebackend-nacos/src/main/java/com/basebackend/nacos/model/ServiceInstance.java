package com.basebackend.nacos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 服务实例信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance implements Serializable {

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
     * 是否临时实例
     */
    private Boolean ephemeral;

    /**
     * 元数据
     */
    private Map<String, String> metadata;

    /**
     * 实例唯一标识
     */
    private String instanceId;
}
