package com.basebackend.admin.entity.nacos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Nacos服务注册实体
 */
@Data
@TableName("sys_nacos_service")
public class SysNacosService {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 服务名
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 分组名
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 命名空间
     */
    @TableField("namespace")
    private String namespace;

    /**
     * 集群名
     */
    @TableField("cluster_name")
    private String clusterName;

    /**
     * 实例总数
     */
    @TableField("instance_count")
    private Integer instanceCount;

    /**
     * 健康实例数
     */
    @TableField("healthy_count")
    private Integer healthyCount;

    /**
     * 服务状态
     */
    @TableField("status")
    private String status;

    /**
     * 服务元数据
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
