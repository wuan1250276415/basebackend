package com.basebackend.scheduler.persistence.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * 工作流实例数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
public class WorkflowInstanceDTO {

    /**
     * 实例ID
     */
    private String id;

    /**
     * 工作流定义ID
     */
    private String definitionId;

    /**
     * 实例状态
     */
    private String status;

    /**
     * 活跃节点集合
     */
    private Set<String> activeNodes;

    /**
     * 上下文参数
     */
    private Map<String, Object> context;

    /**
     * 开始时间
     */
    private Instant startTime;

    /**
     * 结束时间
     */
    private Instant endTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private Instant createTime;

    /**
     * 更新时间
     */
    private Instant updateTime;

    /**
     * 版本号
     */
    private Long version;

    /**
     * 活跃节点集合 (JSON格式) - 用于数据库持久化
     */
    private String activeNodesJson;

    /**
     * 上下文参数 (JSON格式) - 用于数据库持久化
     */
    private String contextJson;

    /**
     * 逻辑删除标志
     */
    private Integer deleted;
}
