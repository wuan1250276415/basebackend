package com.basebackend.scheduler.persistence.repository;

import com.basebackend.scheduler.persistence.dto.WorkflowInstanceDTO;
import com.basebackend.scheduler.workflow.WorkflowInstance;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 工作流实例仓储接口
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface WorkflowInstanceRepository {

    /**
     * 保存工作流实例
     */
    WorkflowInstanceDTO save(WorkflowInstanceDTO dto);

    /**
     * 根据ID查询实例
     */
    Optional<WorkflowInstanceDTO> findById(String id);

    /**
     * 查询定义下的所有实例
     */
    List<WorkflowInstanceDTO> findByDefinitionId(String definitionId);

    /**
     * 查询运行中的实例
     */
    List<WorkflowInstanceDTO> findRunningByDefinitionId(String definitionId);

    /**
     * 更新实例状态
     */
    boolean updateStatus(String id, WorkflowInstance.Status status, Instant endTime, String errorMessage);

    /**
     * 更新活跃节点
     */
    boolean updateActiveNodes(String id, Set<String> activeNodes, Long expectedVersion);

    /**
     * 更新上下文
     */
    boolean updateContext(String id, Map<String, Object> context, Long expectedVersion);

    /**
     * 删除实例
     */
    boolean deleteById(String id);

    /**
     * 批量删除实例
     */
    int deleteByIds(List<String> ids);

    /**
     * 查询超时实例
     */
    List<WorkflowInstanceDTO> findTimeoutInstances(Instant timeoutTime);

    /**
     * 查询最近失败的实例
     */
    List<WorkflowInstanceDTO> findFailedRecentMinutes(int minutes);

    /**
     * 统计活跃实例数量
     */
    int countActiveInstances();

    /**
     * 清理过期实例
     */
    int cleanupExpiredInstances(Instant expireTime);
}
