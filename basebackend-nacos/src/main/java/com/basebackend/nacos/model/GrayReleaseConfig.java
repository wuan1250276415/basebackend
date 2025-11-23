package com.basebackend.nacos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 灰度发布配置模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrayReleaseConfig implements Serializable {

    /**
     * 灰度发布ID
     */
    private Long id;

    /**
     * 关联的配置ID
     */
    private Long configId;

    /**
     * 配置Data ID
     */
    private String dataId;

    /**
     * 灰度策略类型（ip/percentage/label）
     */
    private String strategyType;

    /**
     * 目标实例列表（IP列表，逗号分隔）
     */
    private String targetInstances;

    /**
     * 灰度百分比（0-100）
     */
    private Integer percentage;

    /**
     * 实例标签（JSON格式）
     */
    private String labels;

    /**
     * 灰度状态（preparing/running/completed/rollback）
     */
    private String status;

    /**
     * 灰度开始时间
     */
    private LocalDateTime startTime;

    /**
     * 灰度结束时间
     */
    private LocalDateTime endTime;

    /**
     * 灰度全量发布时间
     */
    private LocalDateTime promoteTime;

    /**
     * 灰度回滚时间
     */
    private LocalDateTime rollbackTime;

    /**
     * 灰度配置内容
     */
    private String grayContent;

    /**
     * 实际生效实例列表
     */
    private transient List<String> effectiveInstances;
}
