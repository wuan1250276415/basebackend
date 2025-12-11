package com.basebackend.scheduler.persistence.repository.impl;

import com.basebackend.scheduler.persistence.dto.WorkflowInstanceDTO;
import com.basebackend.scheduler.persistence.entity.WorkflowInstanceEntity;
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

        // 转换为Entity并保存
        WorkflowInstanceEntity entity = dtoToEntity(dto);
        mapper.insert(entity);
        return entityToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowInstanceDTO> findById(String id) {
        WorkflowInstanceEntity entity = mapper.selectById(id);
        if (entity != null) {
            WorkflowInstanceDTO dto = entityToDto(entity);
            deserializeFields(dto);
            return Optional.of(dto);
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstanceDTO> findByDefinitionId(String definitionId) {
        List<WorkflowInstanceEntity> entities = mapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowInstanceEntity>()
                        .eq(WorkflowInstanceEntity::getDefinitionId, definitionId)
        );

        List<WorkflowInstanceDTO> dtos = entities.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
        dtos.forEach(this::deserializeFields);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstanceDTO> findRunningByDefinitionId(String definitionId) {
        List<WorkflowInstanceEntity> entities = mapper.selectRunningByDefinitionId(definitionId);
        List<WorkflowInstanceDTO> dtos = entities.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
        dtos.forEach(this::deserializeFields);
        return dtos;
    }

    @Override
    @Transactional
    public boolean updateStatus(String id, com.basebackend.scheduler.workflow.WorkflowInstance.Status status,
                                Instant endTime, String errorMessage) {
        WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
        entity.setId(id);
        entity.setStatus(status.name());
        entity.setEndTime(endTime);
        entity.setErrorMessage(errorMessage);
        entity.setUpdateTime(Instant.now());

        int rows = mapper.updateById(entity);
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean updateActiveNodes(String id, Set<String> activeNodes, Long expectedVersion) {
        int rows = mapper.updateActiveNodes(id, toJson(activeNodes), expectedVersion);
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean updateContext(String id, Map<String, Object> context, Long expectedVersion) {
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
        List<WorkflowInstanceEntity> entities = mapper.selectTimeoutInstances(timeoutTime);
        List<WorkflowInstanceDTO> dtos = entities.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
        dtos.forEach(this::deserializeFields);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstanceDTO> findFailedRecentMinutes(int minutes) {
        List<WorkflowInstanceEntity> entities = mapper.selectFailedRecentMinutes(minutes);
        List<WorkflowInstanceDTO> dtos = entities.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
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
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowInstanceEntity>()
                        .lt(WorkflowInstanceEntity::getCreateTime, expireTime)
                        .eq(WorkflowInstanceEntity::getDeleted, 1)
        );
    }

    /**
     * 将DTO转换为Entity
     */
    private WorkflowInstanceEntity dtoToEntity(WorkflowInstanceDTO dto) {
        WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
        entity.setId(dto.getId());
        entity.setDefinitionId(dto.getDefinitionId());
        entity.setStatus(dto.getStatus());
        entity.setActiveNodesJson(dto.getActiveNodesJson());
        entity.setContextJson(dto.getContextJson());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setErrorMessage(dto.getErrorMessage());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());
        entity.setVersion(dto.getVersion());
        entity.setDeleted(dto.getDeleted());
        return entity;
    }

    /**
     * 将Entity转换为DTO
     */
    private WorkflowInstanceDTO entityToDto(WorkflowInstanceEntity entity) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setId(entity.getId());
        dto.setDefinitionId(entity.getDefinitionId());
        dto.setStatus(entity.getStatus());
        dto.setActiveNodesJson(entity.getActiveNodesJson());
        dto.setContextJson(entity.getContextJson());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setVersion(entity.getVersion());
        dto.setDeleted(entity.getDeleted());
        return dto;
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
