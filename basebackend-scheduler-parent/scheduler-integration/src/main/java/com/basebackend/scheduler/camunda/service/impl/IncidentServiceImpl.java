package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.config.PaginationConstants;
import com.basebackend.scheduler.camunda.dto.IncidentDTO;
import com.basebackend.scheduler.camunda.dto.IncidentPageQuery;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.IncidentQuery;
import org.camunda.bpm.engine.runtime.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 异常事件管理服务实现类
 *
 * <p>
 * 提供 Camunda 异常事件的查询、解决等管理功能。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final RuntimeService runtimeService;
    private final ManagementService managementService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<IncidentDTO> page(IncidentPageQuery query) {
        try {
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying incidents, page={}, size={}, incidentType={}",
                    pageNum, pageSize, query.getIncidentType());

            IncidentQuery incidentQuery = runtimeService.createIncidentQuery();

            // 应用查询条件
            applyQueryFilters(incidentQuery, query);

            // 应用排序
            applySorting(incidentQuery, query);

            // 统计总数
            long total = incidentQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<Incident> incidents = incidentQuery.listPage(firstResult, pageSize);

            // 转换为 DTO
            List<IncidentDTO> dtoList = incidents.stream()
                    .map(IncidentDTO::from)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long) pageNum, (long) pageSize);
        } catch (Exception ex) {
            log.error("Failed to query incidents", ex);
            throw new CamundaServiceException("查询异常事件失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentDTO detail(String incidentId) {
        try {
            log.info("Getting incident detail, incidentId={}", incidentId);

            Incident incident = runtimeService.createIncidentQuery()
                    .incidentId(incidentId)
                    .singleResult();

            if (incident == null) {
                throw new CamundaServiceException("异常事件不存在: " + incidentId);
            }

            return IncidentDTO.from(incident);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get incident detail, incidentId={}", incidentId, ex);
            throw new CamundaServiceException("获取异常事件详情失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentDTO> listByProcessInstance(String processInstanceId) {
        try {
            log.info("Listing incidents by process instance, processInstanceId={}", processInstanceId);

            List<Incident> incidents = runtimeService.createIncidentQuery()
                    .processInstanceId(processInstanceId)
                    .orderByIncidentTimestamp().desc()
                    .list();

            return incidents.stream()
                    .map(IncidentDTO::from)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Failed to list incidents by process instance, processInstanceId={}", processInstanceId, ex);
            throw new CamundaServiceException("查询流程实例异常事件失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentDTO> listByExecution(String executionId) {
        try {
            log.info("Listing incidents by execution, executionId={}", executionId);

            List<Incident> incidents = runtimeService.createIncidentQuery()
                    .executionId(executionId)
                    .orderByIncidentTimestamp().desc()
                    .list();

            return incidents.stream()
                    .map(IncidentDTO::from)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Failed to list incidents by execution, executionId={}", executionId, ex);
            throw new CamundaServiceException("查询执行实例异常事件失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(String incidentId) {
        try {
            log.info("Resolving incident, incidentId={}", incidentId);

            Incident incident = runtimeService.createIncidentQuery()
                    .incidentId(incidentId)
                    .singleResult();

            if (incident == null) {
                throw new CamundaServiceException("异常事件不存在: " + incidentId);
            }

            // 获取关联的作业配置（通常是作业 ID）
            String configuration = incident.getConfiguration();
            if (StringUtils.hasText(configuration)) {
                // 尝试将作业重试次数设置为 3
                try {
                    Job job = managementService.createJobQuery()
                            .jobId(configuration)
                            .singleResult();
                    if (job != null) {
                        managementService.setJobRetries(configuration, 3);
                        log.info("Associated job retries set to 3, jobId={}", configuration);
                    }
                } catch (Exception e) {
                    log.warn("Failed to retry associated job: {}, error: {}", configuration, e.getMessage());
                }
            }

            // Camunda 的 Incident 会在作业重试成功后自动解决
            // 这里手动调用 resolve 的方法（如果需要）
            // runtimeService.resolveIncident(incidentId); // Camunda 7.x 没有此方法

            log.info("Incident resolution initiated, incidentId={}", incidentId);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to resolve incident, incidentId={}", incidentId, ex);
            throw new CamundaServiceException("解决异常事件失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAnnotation(String incidentId, String annotation) {
        try {
            log.info("Setting incident annotation, incidentId={}, annotation={}", incidentId, annotation);

            Incident incident = runtimeService.createIncidentQuery()
                    .incidentId(incidentId)
                    .singleResult();

            if (incident == null) {
                throw new CamundaServiceException("异常事件不存在: " + incidentId);
            }

            runtimeService.setAnnotationForIncidentById(incidentId, annotation);

            log.info("Incident annotation set successfully, incidentId={}", incidentId);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to set incident annotation, incidentId={}", incidentId, ex);
            throw new CamundaServiceException("设置异常事件注解失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countIncidents() {
        return runtimeService.createIncidentQuery().count();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByType() {
        List<Incident> incidents = runtimeService.createIncidentQuery().list();

        return incidents.stream()
                .collect(Collectors.groupingBy(
                        Incident::getIncidentType,
                        Collectors.counting()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByProcessDefinition() {
        List<Incident> incidents = runtimeService.createIncidentQuery().list();

        return incidents.stream()
                .filter(incident -> incident.getProcessDefinitionId() != null)
                .collect(Collectors.groupingBy(
                        Incident::getProcessDefinitionId,
                        Collectors.counting()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentDTO> listRecentIncidents(int maxResults) {
        List<Incident> incidents = runtimeService.createIncidentQuery()
                .orderByIncidentTimestamp().desc()
                .listPage(0, maxResults);

        return incidents.stream()
                .map(IncidentDTO::from)
                .collect(Collectors.toList());
    }

    // ========== 私有辅助方法 ==========

    private void applyQueryFilters(IncidentQuery query, IncidentPageQuery pageQuery) {
        if (StringUtils.hasText(pageQuery.getIncidentType())) {
            query.incidentType(pageQuery.getIncidentType());
        }
        if (StringUtils.hasText(pageQuery.getProcessInstanceId())) {
            query.processInstanceId(pageQuery.getProcessInstanceId());
        }
        if (StringUtils.hasText(pageQuery.getProcessDefinitionId())) {
            query.processDefinitionId(pageQuery.getProcessDefinitionId());
        }
        if (StringUtils.hasText(pageQuery.getProcessDefinitionKey())) {
            query.processDefinitionKeyIn(pageQuery.getProcessDefinitionKey());
        }
        if (StringUtils.hasText(pageQuery.getActivityId())) {
            query.activityId(pageQuery.getActivityId());
        }
        if (StringUtils.hasText(pageQuery.getExecutionId())) {
            query.executionId(pageQuery.getExecutionId());
        }
        // IncidentQuery 可能不支持 jobDefinitionId 方法，取决于 Camunda 版本
        // if (StringUtils.hasText(pageQuery.getJobDefinitionId())) {
        // query.jobDefinitionId(pageQuery.getJobDefinitionId());
        // }
        if (StringUtils.hasText(pageQuery.getTenantId())) {
            query.tenantIdIn(pageQuery.getTenantId());
        }
        if (pageQuery.getIncidentTimestampBefore() != null) {
            query.incidentTimestampBefore(pageQuery.getIncidentTimestampBefore());
        }
        if (pageQuery.getIncidentTimestampAfter() != null) {
            query.incidentTimestampAfter(pageQuery.getIncidentTimestampAfter());
        }
    }

    private void applySorting(IncidentQuery query, IncidentPageQuery pageQuery) {
        String sortBy = pageQuery.getSortBy();
        boolean asc = !"desc".equalsIgnoreCase(pageQuery.getSortOrder());

        if (sortBy == null) {
            // 默认按发生时间倒序
            query.orderByIncidentTimestamp().desc();
            return;
        }

        switch (sortBy.toLowerCase()) {
            case "incidentid":
                if (asc)
                    query.orderByIncidentId().asc();
                else
                    query.orderByIncidentId().desc();
                break;
            case "incidenttimestamp":
                if (asc)
                    query.orderByIncidentTimestamp().asc();
                else
                    query.orderByIncidentTimestamp().desc();
                break;
            case "incidenttype":
                if (asc)
                    query.orderByIncidentType().asc();
                else
                    query.orderByIncidentType().desc();
                break;
            case "executionid":
                if (asc)
                    query.orderByExecutionId().asc();
                else
                    query.orderByExecutionId().desc();
                break;
            case "activityid":
                if (asc)
                    query.orderByActivityId().asc();
                else
                    query.orderByActivityId().desc();
                break;
            case "processinstanceid":
                if (asc)
                    query.orderByProcessInstanceId().asc();
                else
                    query.orderByProcessInstanceId().desc();
                break;
            case "processdefinitionid":
                if (asc)
                    query.orderByProcessDefinitionId().asc();
                else
                    query.orderByProcessDefinitionId().desc();
                break;
            default:
                query.orderByIncidentTimestamp().desc();
        }
    }
}
