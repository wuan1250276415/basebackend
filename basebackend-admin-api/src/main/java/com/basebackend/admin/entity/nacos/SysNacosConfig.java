package com.basebackend.admin.entity.nacos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Nacos配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_nacos_config")
public class SysNacosConfig extends BaseEntity {

    /**
     * 配置Data ID
     */
    @TableField("data_id")
    private String dataId;

    /**
     * 配置分组
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 命名空间
     */
    @TableField("namespace")
    private String namespace;

    /**
     * 配置内容
     */
    @TableField("content")
    private String content;

    /**
     * 配置类型
     */
    @TableField("type")
    private String type;

    /**
     * 环境
     */
    @TableField("environment")
    private String environment;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 应用ID
     */
    @TableField("app_id")
    private Long appId;

    /**
     * 配置版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 配置状态
     */
    @TableField("status")
    private String status;

    /**
     * 是否关键配置
     */
    @TableField("is_critical")
    private Boolean isCritical;

    /**
     * 发布类型
     */
    @TableField("publish_type")
    private String publishType;

    /**
     * 配置描述
     */
    @TableField("description")
    private String description;

    /**
     * MD5值
     */
    @TableField("md5")
    private String md5;
}
