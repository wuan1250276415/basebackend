package com.basebackend.admin.entity.nacos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Nacos配置历史实体
 */
@Data
@TableName("sys_nacos_config_history")
public class SysNacosConfigHistory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置ID
     */
    @TableField("config_id")
    private Long configId;

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
     * 配置版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 操作类型
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作人ID
     */
    @TableField("operator")
    private Long operator;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 回滚来源版本
     */
    @TableField("rollback_from")
    private Integer rollbackFrom;

    /**
     * MD5值
     */
    @TableField("md5")
    private String md5;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
