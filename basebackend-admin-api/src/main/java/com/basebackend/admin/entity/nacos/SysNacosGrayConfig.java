package com.basebackend.admin.entity.nacos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Nacos灰度发布配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_nacos_gray_config")
public class SysNacosGrayConfig extends BaseEntity {

    /**
     * 关联的配置ID
     */
    @TableField("config_id")
    private Long configId;

    /**
     * 灰度策略类型
     */
    @TableField("strategy_type")
    private String strategyType;

    /**
     * 目标实例列表
     */
    @TableField("target_instances")
    private String targetInstances;

    /**
     * 灰度百分比
     */
    @TableField("percentage")
    private Integer percentage;

    /**
     * 实例标签
     */
    @TableField("labels")
    private String labels;

    /**
     * 灰度状态
     */
    @TableField("status")
    private String status;

    /**
     * 灰度开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 灰度结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 灰度配置内容
     */
    @TableField("gray_content")
    private String grayContent;
}
