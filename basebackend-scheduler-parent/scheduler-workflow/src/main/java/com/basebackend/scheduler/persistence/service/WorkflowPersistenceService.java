package com.basebackend.scheduler.persistence.service;

import com.basebackend.scheduler.persistence.dto.WorkflowInstanceDTO;
import com.basebackend.scheduler.persistence.repository.WorkflowInstanceRepository;
import com.basebackend.scheduler.workflow.WorkflowInstance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流持久化服务
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class WorkflowPersistenceService {

    private final WorkflowInstanceRepository repository;
    private final ObjectMapper objectMapper;

    public WorkflowPersistenceService(WorkflowInstanceRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存工作流实例
     */
    @Transactional
    public WorkflowInstance save(WorkflowInstance instance) {
        WorkflowInstanceDTO dto = convertToDTO(instance);
        log.debug("Saving workflow instance [{}] with status [{}]", instance.getId(), instance.getStatus());
        WorkflowInstanceDTO savedDto = repository.save(dto);
        return convertFromDTO(savedDto);
    }

    /**
     * 恢复工作流实例
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowInstance> restore(String instanceId) {
        Optional<WorkflowInstanceDTO> dtoOpt = repository.findById(instanceId);
        if (dtoOpt.isPresent()) {
            WorkflowInstanceDTO dto = dtoOpt.get();
            log.info("Restored workflow instance [{}] with status [{}]", dto.getId(), dto.getStatus());
            return Optional.of(convertFromDTO(dto));
        }
        return Optional.empty();
    }

    /**
     * 查询运行中的实例
     */
    @Transactional(readOnly = true)
    public List<WorkflowInstance> findRunningInstances(String definitionId) {
        List<WorkflowInstanceDTO> dtos = repository.findRunningByDefinitionId(definitionId);
        log.debug("Found [{}] running instances for definition [{}]", dtos.size(), definitionId);
        return dtos.stream()
                .map(this::convertFromDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新实例状态
     */
    @Transactional
    public boolean updateStatus(String instanceId, WorkflowInstance.Status status, Instant endTime, String errorMessage) {
        boolean success = repository.updateStatus(instanceId, status, endTime, errorMessage);
        if (success) {
            log.debug("Updated workflow instance [{}] status to [{}]", instanceId, status);
        } else {
            log.warn("Failed to update workflow instance [{}] status to [{}]", instanceId, status);
        }
        return success;
    }

    /**
     * 更新活跃节点
     */
    @Transactional
    public boolean updateActiveNodes(String instanceId, Set<String> activeNodes, Long expectedVersion) {
        boolean success = repository.updateActiveNodes(instanceId, activeNodes, expectedVersion);
        if (success) {
            log.debug("Updated active nodes for workflow instance [{}]", instanceId);
        } else {
            log.warn("Failed to update active nodes for workflow instance [{}], version mismatch", instanceId);
        }
        return success;
    }

    /**
     * 更新上下文
     */
    @Transactional
    public boolean updateContext(String instanceId, Map<String, Object> context, Long expectedVersion) {
        boolean success = repository.updateContext(instanceId, context, expectedVersion);
        if (success) {
            log.debug("Updated context for workflow instance [{}]", instanceId);
        } else {
            log.warn("Failed to update context for workflow instance [{}], version mismatch", instanceId);
        }
        return success;
    }

    /**
     * 查询超时实例
     */
    @Transactional(readOnly = true)
    public List<WorkflowInstance> findTimeoutInstances(Instant timeoutTime) {
        List<WorkflowInstanceDTO> dtos = repository.findTimeoutInstances(timeoutTime);
        log.debug("Found [{}] timeout instances", dtos.size());
        return dtos.stream()
                .map(this::convertFromDTO)
                .collect(Collectors.toList());
    }

    /**
     * 清理过期实例
     */
    @Transactional
    public int cleanupExpiredInstances(Instant expireTime) {
        int count = repository.cleanupExpiredInstances(expireTime);
        if (count > 0) {
            log.info("Cleaned up [{}] expired workflow instances", count);
        }
        return count;
    }

    /**
     * 转换为DTO
     */
    private WorkflowInstanceDTO convertToDTO(WorkflowInstance instance) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setId(instance.getId());
        dto.setDefinitionId(instance.getDefinitionId());
        dto.setStatus(instance.getStatus().name());
        dto.setActiveNodes(instance.getActiveNodes());
        dto.setContext(instance.getContext());
        dto.setStartTime(instance.getStartTime());
        dto.setEndTime(instance.getEndTime());
        dto.setErrorMessage(instance.getErrorMessage());
        dto.setCreateTime(Instant.now());
        dto.setUpdateTime(Instant.now());
        dto.setVersion(1L);
        return dto;
    }

    /**
     * 从DTO转换
     */
    private WorkflowInstance convertFromDTO(WorkflowInstanceDTO dto) {
        Set<String> activeNodes = dto.getActiveNodes() != null ?
                dto.getActiveNodes() : Collections.emptySet();

        Map<String, Object> context = dto.getContext() != null ?
                dto.getContext() : Collections.emptyMap();

        return WorkflowInstance.builder(dto.getId(), dto.getDefinitionId())
                .status(WorkflowInstance.Status.valueOf(dto.getStatus()))
                .activeNodes(activeNodes)
                .context(context)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .errorMessage(dto.getErrorMessage())
                .build();
    }
}
