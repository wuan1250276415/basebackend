package com.basebackend.admin.entity.nacos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Nacos配置发布任务实体
 */
@Data
@TableName("sys_nacos_publish_task")
public class SysNacosPublishTask {

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
     * 发布类型
     */
    @TableField("publish_type")
    private String publishType;

    /**
     * 任务状态
     */
    @TableField("status")
    private String status;

    /**
     * 执行人ID
     */
    @TableField("executor")
    private Long executor;

    /**
     * 执行人姓名
     */
    @TableField("executor_name")
    private String executorName;

    /**
     * 目标实例列表
     */
    @TableField("target_instances")
    private String targetInstances;

    /**
     * 执行结果
     */
    @TableField("result")
    private String result;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    @TableField("finish_time")
    private LocalDateTime finishTime;
}
