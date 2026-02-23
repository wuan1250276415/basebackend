package com.basebackend.scheduler.persistence.repository.impl;

import com.basebackend.scheduler.persistence.dto.WorkflowInstanceDTO;
import com.basebackend.scheduler.persistence.mapper.WorkflowInstanceMapper;
import com.basebackend.scheduler.persistence.repository.WorkflowInstanceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流实例仓储实现
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Repository
public class WorkflowInstanceRepositoryImpl implements WorkflowInstanceRepository {

    private final WorkflowInstanceMapper mapper;
    private final ObjectMapper objectMapper;

    public WorkflowInstanceRepositoryImpl(WorkflowInstanceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public WorkflowInstanceDTO save(WorkflowInstanceDTO dto) {
        // 设置创建和更新时间
        Instant now = Instant.now();
        if (dto.getCreateTime() == null) {
            dto.setCreateTime(now);
        }
        dto.setUpdateTime(now);

        // 序列化活跃节点和上下文
        dto.setActiveNodesJson(dto.getActiveNodes() != null ?
                toJson(dto.getActiveNodes()) : null);
        dto.setContextJson(dto.getContext() != null ?
                toJson(dto.getContext()) : null);

        mapper.insert(dto);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowInstanceDTO> findById(String id) {
        WorkflowInstanceDTO dto = mapper.selectById(id);
        if (dto != null) {
            deserializeFields(dto);
            return Optional.of(dto);
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstanceDTO> findByDefinitionId(String definitionId) {
        List<WorkflowInstanceDTO> dtos = mapper.selectList(
                com.baomidou.mybatisplus.core условия.Wrappers.<WorkflowInstanceDTO>query()
                        .eq("definition_id", definitionId)
        );

        dtos.forEach(this::deserializeFields);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstanceDTO> findRunningByDefinitionId(String definitionId) {
        List<WorkflowInstanceDTO> dtos = mapper.selectRunningByDefinitionId(definitionId);
        dtos.forEach(this::deserializeFields);
        return dtos;
    }

    @Override
    @Transactional
    public boolean updateStatus(String id, com.basebackend.scheduler.workflow.WorkflowInstance.Status status,
                                Instant endTime, String errorMessage) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setId(id);
        dto.setStatus(status.name());
        dto.setEndTime(endTime);
        dto.setErrorMessage(errorMessage);
        dto.setUpdateTime(Instant.now());

        int rows = mapper.updateById(dto);
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean updateActiveNodes(String id, Set<String> activeNodes, Long expectedVersion) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setId(id);
        dto.setActiveNodesJson(toJson(activeNodes));
        dto.setUpdateTime(Instant.now());

        int rows = mapper.updateActiveNodes(id, toJson(activeNodes), expectedVersion);
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean updateContext(String id, Map<String, Object> context, Long expectedVersion) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setId(id);
        dto.setContextJson(toJson(context));
        dto.setUpdateTime(Instant.now());

        int rows = mapper.updateContext(id, toJson(context), expectedVersion);
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean deleteById(String id) {
        int rows = mapper.deleteById(id);
        return rows > 0;
    }

    @Override
    @Transactional
    public int deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return mapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstanceDTO> findTimeoutInstances(Instant timeoutTime) {
        List<WorkflowInstanceDTO> dtos = mapper.selectTimeoutInstances(timeoutTime);
        dtos.forEach(this::deserializeFields);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstanceDTO> findFailedRecentMinutes(int minutes) {
        List<WorkflowInstanceDTO> dtos = mapper.selectFailedRecentMinutes(minutes);
        dtos.forEach(this::deserializeFields);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveInstances() {
        return mapper.countActiveInstances();
    }

    @Override
    @Transactional
    public int cleanupExpiredInstances(Instant expireTime) {
        // 物理删除已过期的实例（已标记为删除的记录）
        return mapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowInstanceDTO>()
                        .lt(WorkflowInstanceDTO::getCreateTime, expireTime)
                        .eq(WorkflowInstanceDTO::getDeleted, 1)
        );
    }

    /**
     * 反序列化JSON字段
     */
    private void deserializeFields(WorkflowInstanceDTO dto) {
        try {
            // 反序列化活跃节点
            if (StringUtils.hasText(dto.getActiveNodesJson())) {
                Set<String> activeNodes = objectMapper.readValue(
                        dto.getActiveNodesJson(),
                        new TypeReference<Set<String>>() {}
                );
                dto.setActiveNodes(activeNodes);
            }

            // 反序列化上下文
            if (StringUtils.hasText(dto.getContextJson())) {
                Map<String, Object> context = objectMapper.readValue(
                        dto.getContextJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
                dto.setContext(context);
            }
        } catch (Exception e) {
            log.error("Failed to deserialize workflow instance [{}]", dto.getId(), e);
        }
    }

    /**
     * 序列化对象为JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON", e);
            return null;
        }
    }
}
