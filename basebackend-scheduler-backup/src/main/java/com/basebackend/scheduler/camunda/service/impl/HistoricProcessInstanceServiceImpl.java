package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.config.PaginationConstants;
import com.basebackend.scheduler.camunda.dto.HistoricActivityInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDetailDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceStatusDTO;
import com.basebackend.scheduler.camunda.dto.HistoricVariableInstanceDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceHistoryQuery;
import com.basebackend.scheduler.camunda.dto.UserOperationLogDTO;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.HistoricProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.history.UserOperationLogEntry;
import org.camunda.bpm.engine.history.UserOperationLogQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 历史流程实例服务实现类
 *
 * <p>提供历史流程实例的查询、详情、状态、活动实例、审计日志等功能。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricProcessInstanceServiceImpl implements HistoricProcessInstanceService {

    private final HistoryService historyService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<HistoricProcessInstanceDTO> page(ProcessInstanceHistoryQuery query) {
        try {
            // 验证分页参数
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying historic process instances, page={}, size={}", pageNum, pageSize);

            HistoricProcessInstanceQuery historyQuery = historyService
                    .createHistoricProcessInstanceQuery();

            // 应用查询条件
            applyQueryFilters(historyQuery, query);

            // 统计总数
            long total = historyQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<HistoricProcessInstance> instances = historyQuery
                    .orderByProcessInstanceStartTime().desc()
                    .listPage(firstResult, pageSize);

            // 转换为DTO
            List<HistoricProcessInstanceDTO> dtoList = instances.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query historic process instances", ex);
            throw new CamundaServiceException("查询历史流程实例失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HistoricProcessInstanceDetailDTO detail(String instanceId) {
        try {
            log.info("Getting historic process instance detail, instanceId={}", instanceId);

            HistoricProcessInstance instance = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();

            if (instance == null) {
                throw new CamundaServiceException("历史流程实例不存在: " + instanceId);
            }

            // Note: includeProcessVariables() is not available in this Camunda version
            // Variables need to be queried separately

            return convertToDetailDTO(instance);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get historic process instance detail, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("获取历史流程实例详情失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HistoricProcessInstanceStatusDTO status(String instanceId) {
        try {
            log.info("Getting historic process instance status, instanceId={}", instanceId);

            HistoricProcessInstance instance = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();

            if (instance == null) {
                throw new CamundaServiceException("历史流程实例不存在: " + instanceId);
            }

            // 获取活动实例统计
            long completedActivities = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(instanceId)
                    .finished()
                    .count();

            long totalActivities = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(instanceId)
                    .count();

            HistoricProcessInstanceStatusDTO dto = new HistoricProcessInstanceStatusDTO();
            dto.setInstanceId(instanceId);
            dto.setState(instance.getState());
            dto.setStartTime(com.basebackend.scheduler.util.DateTimeUtil.toInstant(instance.getStartTime()));
            dto.setEndTime(com.basebackend.scheduler.util.DateTimeUtil.toInstant(instance.getEndTime()));
            dto.setDurationInMillis(instance.getDurationInMillis());
            dto.setDeleteReason(instance.getDeleteReason());
            dto.setCompletedActivities(completedActivities);
            dto.setTotalActivities(totalActivities);

            return dto;
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get historic process instance status, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("获取历史流程实例状态失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<HistoricActivityInstanceDTO> activities(String instanceId,
                                                               ProcessInstanceHistoryQuery query) {
        try {
            // 验证分页参数
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying historic activity instances, instanceId={}, page={}, size={}",
                    instanceId, pageNum, pageSize);

            HistoricActivityInstanceQuery activityQuery = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(instanceId);

            // 统计总数
            long total = activityQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<HistoricActivityInstance> activities = activityQuery
                    .orderByHistoricActivityInstanceStartTime().desc()
                    .listPage(firstResult, pageSize);

            // 转换为DTO
            List<HistoricActivityInstanceDTO> dtoList = activities.stream()
                    .map(this::convertActivityToDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query historic activity instances, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("查询历史活动实例失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserOperationLogDTO> auditLogs(String instanceId,
                                                      ProcessInstanceHistoryQuery query) {
        try {
            // 验证分页参数
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying user operation logs, instanceId={}, page={}, size={}",
                    instanceId, pageNum, pageSize);

            UserOperationLogQuery logQuery = historyService
                    .createUserOperationLogQuery()
                    .processInstanceId(instanceId);

            // 统计总数
            long total = logQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<UserOperationLogEntry> logs = logQuery
                    .orderByTimestamp().desc()
                    .listPage(firstResult, pageSize);

            // 转换为DTO
            List<UserOperationLogDTO> dtoList = logs.stream()
                    .map(this::convertLogToDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query user operation logs, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("查询用户操作审计日志失败: " + ex.getMessage(), ex);
        }
    }

    // ========== 私有辅助方法 ==========

    private void applyQueryFilters(HistoricProcessInstanceQuery query,
                                    ProcessInstanceHistoryQuery pageQuery) {
        if (StringUtils.hasText(pageQuery.getProcessDefinitionKey())) {
            query.processDefinitionKey(pageQuery.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(pageQuery.getBusinessKey())) {
            query.processInstanceBusinessKeyLike("%" + pageQuery.getBusinessKey() + "%");
        }
        if (pageQuery.isFinished() != null) {
            if (pageQuery.isFinished()) {
                query.finished();
            } else {
                query.unfinished();
            }
        }
        if (pageQuery.getStartedAfter() != null) {
            query.startedAfter(com.basebackend.scheduler.util.DateTimeUtil.toDate(pageQuery.getStartedAfter()));
        }
        if (pageQuery.getStartedBefore() != null) {
            query.startedBefore(com.basebackend.scheduler.util.DateTimeUtil.toDate(pageQuery.getStartedBefore()));
        }
        if (StringUtils.hasText(pageQuery.getStartedBy())) {
            query.startedBy(pageQuery.getStartedBy());
        }
    }

    private HistoricProcessInstanceDTO convertToDTO(HistoricProcessInstance instance) {
        HistoricProcessInstanceDTO dto = new HistoricProcessInstanceDTO();
        dto.setId(instance.getId());
        dto.setProcessDefinitionId(instance.getProcessDefinitionId());
        dto.setProcessDefinitionKey(instance.getProcessDefinitionKey());
        // processDefinitionName 需要从 ProcessDefinition 获取
        dto.setProcessDefinitionName(null);
        dto.setBusinessKey(instance.getBusinessKey());
        // 转换 Date to Instant
        dto.setStartTime(instance.getStartTime() != null ? instance.getStartTime().toInstant() : null);
        dto.setEndTime(instance.getEndTime() != null ? instance.getEndTime().toInstant() : null);
        dto.setDurationInMillis(instance.getDurationInMillis());
        dto.setStartUserId(instance.getStartUserId());
        dto.setDeleteReason(instance.getDeleteReason());
        dto.setState(instance.getState());
        dto.setTenantId(instance.getTenantId());
        return dto;
    }

    private HistoricProcessInstanceDetailDTO convertToDetailDTO(HistoricProcessInstance instance) {
        HistoricProcessInstanceDetailDTO dto = new HistoricProcessInstanceDetailDTO();
        dto.setId(instance.getId());
        dto.setProcessDefinitionId(instance.getProcessDefinitionId());
        dto.setProcessDefinitionKey(instance.getProcessDefinitionKey());
        // processDefinitionName 和 processDefinitionVersion 需要从 ProcessDefinition 获取
        dto.setProcessDefinitionName(null);
        dto.setProcessDefinitionVersion(null);
        dto.setBusinessKey(instance.getBusinessKey());
        // 转换 Date to Instant
        dto.setStartTime(instance.getStartTime() != null ? instance.getStartTime().toInstant() : null);
        dto.setEndTime(instance.getEndTime() != null ? instance.getEndTime().toInstant() : null);
        dto.setDurationInMillis(instance.getDurationInMillis());
        dto.setStartUserId(instance.getStartUserId());
        // startActivityId, endActivityId 等需要从活动历史中获取
        dto.setStartActivityId(null);
        dto.setEndActivityId(null);
        dto.setSuperProcessInstanceId(instance.getSuperProcessInstanceId());
        dto.setSuperCaseInstanceId(instance.getSuperCaseInstanceId());
        dto.setCaseInstanceId(instance.getCaseInstanceId());
        dto.setDeleteReason(instance.getDeleteReason());
        dto.setState(instance.getState());
        dto.setTenantId(instance.getTenantId());

        // 获取流程变量（需要单独查询）
        // Note: getProcessVariables() is not available in this Camunda version
        // Variables need to be queried separately using historyService
        try {
            List<HistoricVariableInstance> variables = historyService
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(instance.getId())
                    .list();
            if (variables != null && !variables.isEmpty()) {
                List<HistoricVariableInstanceDTO> variableDTOs = variables.stream()
                        .map(HistoricVariableInstanceDTO::from)
                        .collect(Collectors.toList());
                dto.setVariables(variableDTOs);
            }
        } catch (Exception ex) {
            log.warn("Failed to query process variables for instance: {}", instance.getId(), ex);
        }

        return dto;
    }

    private HistoricActivityInstanceDTO convertActivityToDTO(HistoricActivityInstance activity) {
        HistoricActivityInstanceDTO dto = new HistoricActivityInstanceDTO();
        dto.setId(activity.getId());
        dto.setActivityId(activity.getActivityId());
        dto.setActivityName(activity.getActivityName());
        dto.setActivityType(activity.getActivityType());
        dto.setProcessInstanceId(activity.getProcessInstanceId());
        dto.setProcessDefinitionId(activity.getProcessDefinitionId());
        dto.setExecutionId(activity.getExecutionId());
        dto.setTaskId(activity.getTaskId());
        dto.setAssignee(activity.getAssignee());
        dto.setStartTime(com.basebackend.scheduler.util.DateTimeUtil.toInstant(activity.getStartTime()));
        dto.setEndTime(com.basebackend.scheduler.util.DateTimeUtil.toInstant(activity.getEndTime()));
        dto.setDurationInMillis(activity.getDurationInMillis());
        dto.setCalledProcessInstanceId(activity.getCalledProcessInstanceId());
        dto.setCalledCaseInstanceId(activity.getCalledCaseInstanceId());
        dto.setTenantId(activity.getTenantId());
        return dto;
    }

    private UserOperationLogDTO convertLogToDTO(UserOperationLogEntry log) {
        UserOperationLogDTO dto = new UserOperationLogDTO();
        dto.setId(log.getId());
        dto.setDeploymentId(log.getDeploymentId());
        dto.setProcessDefinitionId(log.getProcessDefinitionId());
        dto.setProcessInstanceId(log.getProcessInstanceId());
        dto.setExecutionId(log.getExecutionId());
        dto.setTaskId(log.getTaskId());
        dto.setUserId(log.getUserId());
        dto.setOperationType(log.getOperationType());
        dto.setOperationId(log.getOperationId());
        dto.setEntityType(log.getEntityType());
        dto.setProperty(log.getProperty());
        dto.setOrgValue(log.getOrgValue());
        dto.setNewValue(log.getNewValue());
        dto.setTimestamp(com.basebackend.scheduler.util.DateTimeUtil.toInstant(log.getTimestamp()));
        return dto;
    }
}
