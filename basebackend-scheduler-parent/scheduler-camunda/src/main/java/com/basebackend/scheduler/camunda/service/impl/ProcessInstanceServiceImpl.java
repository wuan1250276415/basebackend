package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.config.PaginationConstants;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDeleteRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDetailDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceHistoryQuery;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceMigrationRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstancePageQuery;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceVariablesRequest;
import com.basebackend.scheduler.camunda.dto.ProcessVariableDTO;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionStartRequest;

/**
 * 流程实例服务实现类
 *
 * <p>
 * 提供流程实例的查询、挂起、激活、删除、变量管理、迁移等功能。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final org.camunda.bpm.engine.RepositoryService repositoryService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceDTO start(com.basebackend.scheduler.camunda.dto.ProcessDefinitionStartRequest request) {
        // 1. 验证 BusinessKey
        if (!StringUtils.hasText(request.getBusinessKey())) {
            throw new CamundaServiceException("INVALID_REQUEST", "启动流程必须提供业务关联键(BusinessKey)");
        }

        // 2. 验证变量大小 (简单估算)
        if (request.getVariables() != null) {
            try {
                String jsonVariables = objectMapper.writeValueAsString(request.getVariables());
                if (jsonVariables.length() > 50 * 1024) { // 50KB 限制
                    throw new CamundaServiceException("INVALID_REQUEST", "流程变量总大小超过限制(50KB)，请使用业务表存储大对象");
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.warn("Failed to calculate variable size", e);
            }
        }

        // 3. 确定 Definition ID
        String definitionId = request.getProcessDefinitionId();
        if (!StringUtils.hasText(definitionId) && StringUtils.hasText(request.getProcessDefinitionKey())) {
            org.camunda.bpm.engine.repository.ProcessDefinitionQuery query = repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionKey(request.getProcessDefinitionKey())
                    .latestVersion();

            if (StringUtils.hasText(request.getTenantId())) {
                query.tenantIdIn(request.getTenantId());
            }

            org.camunda.bpm.engine.repository.ProcessDefinition definition = query.singleResult();
            if (definition == null) {
                throw new CamundaServiceException("DEFINITION_NOT_FOUND",
                        "找不到流程定义: " + request.getProcessDefinitionKey());
            }
            definitionId = definition.getId();
        }

        if (!StringUtils.hasText(definitionId)) {
            throw new CamundaServiceException("INVALID_REQUEST", "必须提供流程定义ID或Key");
        }

        // 4. 启动流程
        try {
            log.info("Starting process instance, definitionId={}, businessKey={}", definitionId,
                    request.getBusinessKey());
            ProcessInstance instance = runtimeService.startProcessInstanceById(
                    definitionId,
                    request.getBusinessKey(),
                    request.getVariables());
            log.info("Process instance started, id={}", instance.getId());
            return convertToDTO(instance);
        } catch (Exception e) {
            log.error("Failed to start process instance", e);
            throw new CamundaServiceException("启动流程实例失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProcessInstanceDTO> page(ProcessInstancePageQuery query) {
        try {
            // 验证分页参数
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying process instances, page={}, size={}", pageNum, pageSize);

            ProcessInstanceQuery instanceQuery = runtimeService.createProcessInstanceQuery();

            // 应用查询条件
            applyQueryFilters(instanceQuery, query);

            // 统计总数
            long total = instanceQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<ProcessInstance> instances = instanceQuery
                    .orderByProcessInstanceId().desc()
                    .listPage(firstResult, pageSize);

            // 转换为DTO
            List<ProcessInstanceDTO> dtoList = instances.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query process instances", ex);
            throw new CamundaServiceException("查询流程实例失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessInstanceDetailDTO detail(String instanceId, boolean withVariables) {
        try {
            log.info("Getting process instance detail, instanceId={}, withVariables={}",
                    instanceId, withVariables);

            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();

            if (instance == null) {
                throw new CamundaServiceException("流程实例不存在: " + instanceId);
            }

            ProcessInstanceDetailDTO dto = convertToDetailDTO(instance);

            // 获取变量
            if (withVariables) {
                Map<String, Object> variables = runtimeService.getVariables(instanceId);
                dto.setVariables(variables);
            }

            return dto;
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get process instance detail, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("获取流程实例详情失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspend(String instanceId) {
        try {
            log.info("Suspending process instance, instanceId={}", instanceId);

            runtimeService.suspendProcessInstanceById(instanceId);

            log.info("Process instance suspended successfully, instanceId={}", instanceId);
        } catch (Exception ex) {
            log.error("Failed to suspend process instance, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("挂起流程实例失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(String instanceId) {
        try {
            log.info("Activating process instance, instanceId={}", instanceId);

            runtimeService.activateProcessInstanceById(instanceId);

            log.info("Process instance activated successfully, instanceId={}", instanceId);
        } catch (Exception ex) {
            log.error("Failed to activate process instance, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("激活流程实例失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String instanceId, ProcessInstanceDeleteRequest request) {
        try {
            log.info("Deleting process instance, instanceId={}, reason={}",
                    instanceId, request.getDeleteReason());

            runtimeService.deleteProcessInstance(
                    instanceId,
                    request.getDeleteReason(),
                    request.isSkipCustomListeners(),
                    request.isExternallyTerminated(),
                    request.isSkipIoMappings());

            log.info("Process instance deleted successfully, instanceId={}", instanceId);
        } catch (Exception ex) {
            log.error("Failed to delete process instance, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("删除流程实例失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminate(String instanceId, String reason) {
        // terminate 本质上是 delete，但语义上是强制终止
        ProcessInstanceDeleteRequest request = new ProcessInstanceDeleteRequest();
        request.setDeleteReason(StringUtils.hasText(reason) ? reason : "人工强制终止");
        request.setExternallyTerminated(true);
        request.setSkipCustomListeners(false); // 通常终止时可能需要触发结束监听器
        delete(instanceId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String draft(com.basebackend.scheduler.camunda.dto.ProcessDefinitionStartRequest request) {
        // 草稿功能的简易实现：
        // 实际生产中草稿通常存放在业务表，或者一个专门的 mongodb/json 表
        // 这里为了演示，我们假设草稿只是生成一个 BusinessKey 并返回，实际由前端暂存或存业务侧

        // 如果提供了 businessKey，直接返回
        if (StringUtils.hasText(request.getBusinessKey())) {
            return request.getBusinessKey();
        }

        // 否则生成一个草稿ID (例如 DRAFT_UUID)
        return "DRAFT_" + java.util.UUID.randomUUID().toString();

        // NOTE: 真正的草稿应该存库。鉴于 scheduler-integration 定位，
        // 建议业务方调用自己的 DraftService，这里仅提供接口契约的空实现或简单逻辑。
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessVariableDTO> variables(String instanceId, boolean local) {
        try {
            log.info("Getting process variables, instanceId={}, local={}", instanceId, local);

            List<VariableInstance> variables;
            if (local) {
                // 查询局部变量（仅限于流程实例执行级别的变量）
                variables = runtimeService.createVariableInstanceQuery()
                        .variableScopeIdIn(instanceId)
                        .list();
            } else {
                // 查询所有变量（包括全局和局部）
                variables = runtimeService.createVariableInstanceQuery()
                        .processInstanceIdIn(instanceId)
                        .list();
            }

            return variables.stream()
                    .map(this::convertVariableToDTO)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Failed to get process variables, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("获取流程变量失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessVariableDTO variable(String instanceId, String variableName, boolean local) {
        try {
            log.info("Getting process variable, instanceId={}, name={}, local={}",
                    instanceId, variableName, local);

            Object value;
            if (local) {
                value = runtimeService.getVariableLocal(instanceId, variableName);
            } else {
                value = runtimeService.getVariable(instanceId, variableName);
            }

            // 注意：null可能表示变量值为null，也可能表示变量不存在
            // 这里使用VariableInstance来区分
            VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
                    .processInstanceIdIn(instanceId)
                    .variableName(variableName)
                    .singleResult();

            if (variableInstance == null) {
                throw new CamundaServiceException("变量不存在: " + variableName);
            }

            ProcessVariableDTO dto = new ProcessVariableDTO();
            dto.setName(variableName);
            dto.setValue(value);
            dto.setType(variableInstance.getTypeName());

            return dto;
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get process variable, instanceId={}, name={}",
                    instanceId, variableName, ex);
            throw new CamundaServiceException("获取流程变量失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setVariables(String instanceId, ProcessInstanceVariablesRequest request) {
        try {
            log.info("Setting process variables, instanceId={}, count={}",
                    instanceId, request.getVariables().size());

            if (request.getLocal()) {
                runtimeService.setVariablesLocal(instanceId, request.getVariables());
            } else {
                runtimeService.setVariables(instanceId, request.getVariables());
            }

            log.info("Process variables set successfully, instanceId={}", instanceId);
        } catch (Exception ex) {
            log.error("Failed to set process variables, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("设置流程变量失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVariable(String instanceId, String variableName, boolean local) {
        try {
            log.info("Deleting process variable, instanceId={}, name={}", instanceId, variableName);

            if (local) {
                runtimeService.removeVariableLocal(instanceId, variableName);
            } else {
                runtimeService.removeVariable(instanceId, variableName);
            }

            log.info("Process variable deleted successfully, instanceId={}, name={}",
                    instanceId, variableName);
        } catch (Exception ex) {
            log.error("Failed to delete process variable, instanceId={}, name={}",
                    instanceId, variableName, ex);
            throw new CamundaServiceException("删除流程变量失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void migrate(String instanceId, ProcessInstanceMigrationRequest request) {
        try {
            log.info("Migrating process instance, instanceId={}, targetDefinitionId={}",
                    instanceId, request.getTargetProcessDefinitionId());

            // 获取源流程定义
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();

            if (instance == null) {
                throw new CamundaServiceException("流程实例不存在: " + instanceId);
            }

            // 创建迁移计划
            MigrationPlan migrationPlan = runtimeService.createMigrationPlan(
                    instance.getProcessDefinitionId(),
                    request.getTargetProcessDefinitionId()).mapEqualActivities().build();

            // 执行迁移
            runtimeService.newMigration(migrationPlan)
                    .processInstanceIds(instanceId)
                    .execute();

            log.info("Process instance migrated successfully, instanceId={}", instanceId);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to migrate process instance, instanceId={}", instanceId, ex);
            throw new CamundaServiceException("迁移流程实例失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<HistoricProcessInstanceDTO> history(ProcessInstanceHistoryQuery query) {
        try {
            // 验证分页参数
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying historic process instances, page={}, size={}", pageNum, pageSize);

            HistoricProcessInstanceQuery historyQuery = historyService
                    .createHistoricProcessInstanceQuery();

            // 应用查询条件
            applyHistoryQueryFilters(historyQuery, query);

            // 统计总数
            long total = historyQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<HistoricProcessInstance> instances = historyQuery
                    .orderByProcessInstanceStartTime().desc()
                    .listPage(firstResult, pageSize);

            // 转换为DTO
            List<HistoricProcessInstanceDTO> dtoList = instances.stream()
                    .map(this::convertHistoricToDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query historic process instances", ex);
            throw new CamundaServiceException("查询历史流程实例失败: " + ex.getMessage(), ex);
        }
    }

    // ========== 私有辅助方法 ==========

    private void applyQueryFilters(ProcessInstanceQuery query, ProcessInstancePageQuery pageQuery) {
        if (StringUtils.hasText(pageQuery.getProcessDefinitionKey())) {
            query.processDefinitionKey(pageQuery.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(pageQuery.getBusinessKey())) {
            query.processInstanceBusinessKeyLike("%" + pageQuery.getBusinessKey() + "%");
        }
        if (StringUtils.hasText(pageQuery.getTenantId())) {
            query.tenantIdIn(pageQuery.getTenantId());
        }
        if (pageQuery.getSuspended() != null) {
            if (pageQuery.getSuspended()) {
                query.suspended();
            } else {
                query.active();
            }
        }
    }

    private void applyHistoryQueryFilters(HistoricProcessInstanceQuery query,
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
            query.startedAfter(Date.from(pageQuery.getStartedAfter()));
        }
        if (pageQuery.getStartedBefore() != null) {
            query.startedBefore(Date.from(pageQuery.getStartedBefore()));
        }
    }

    private ProcessInstanceDTO convertToDTO(ProcessInstance instance) {
        ProcessInstanceDTO dto = new ProcessInstanceDTO();
        dto.setId(instance.getId());
        dto.setProcessDefinitionId(instance.getProcessDefinitionId());
        dto.setProcessInstanceId(instance.getProcessInstanceId());
        dto.setBusinessKey(instance.getBusinessKey());
        dto.setCaseInstanceId(instance.getCaseInstanceId());
        dto.setSuspended(instance.isSuspended());
        dto.setEnded(instance.isEnded());
        dto.setTenantId(instance.getTenantId());
        return dto;
    }

    private ProcessInstanceDetailDTO convertToDetailDTO(ProcessInstance instance) {
        ProcessInstanceDetailDTO dto = new ProcessInstanceDetailDTO();
        dto.setId(instance.getId());
        dto.setProcessDefinitionId(instance.getProcessDefinitionId());
        dto.setProcessInstanceId(instance.getProcessInstanceId());
        dto.setBusinessKey(instance.getBusinessKey());
        dto.setCaseInstanceId(instance.getCaseInstanceId());
        dto.setSuspended(instance.isSuspended());
        dto.setEnded(instance.isEnded());
        dto.setTenantId(instance.getTenantId());
        return dto;
    }

    private HistoricProcessInstanceDTO convertHistoricToDTO(HistoricProcessInstance instance) {
        HistoricProcessInstanceDTO dto = new HistoricProcessInstanceDTO();
        dto.setId(instance.getId());
        dto.setProcessDefinitionId(instance.getProcessDefinitionId());
        dto.setProcessDefinitionKey(instance.getProcessDefinitionKey());
        dto.setProcessDefinitionName(instance.getProcessDefinitionName());
        dto.setBusinessKey(instance.getBusinessKey());
        dto.setStartTime(instance.getStartTime().toInstant());
        dto.setEndTime(com.basebackend.scheduler.util.DateTimeUtil.toInstant(instance.getEndTime()));
        dto.setDurationInMillis(instance.getDurationInMillis());
        dto.setStartUserId(instance.getStartUserId());
        dto.setDeleteReason(instance.getDeleteReason());
        dto.setState(instance.getState());
        dto.setTenantId(instance.getTenantId());
        return dto;
    }

    private ProcessVariableDTO convertVariableToDTO(VariableInstance variable) {
        ProcessVariableDTO dto = new ProcessVariableDTO();
        dto.setName(variable.getName());
        dto.setValue(variable.getValue());
        dto.setType(variable.getTypeName());
        dto.setProcessInstanceId(variable.getProcessInstanceId());
        dto.setExecutionId(variable.getExecutionId());
        dto.setTaskId(variable.getTaskId());
        return dto;
    }
}
