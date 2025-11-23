package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.config.PaginationConstants;
import com.basebackend.scheduler.camunda.dto.*;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.ProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现类
 *
 * <p>提供流程定义的部署、查询、启动、挂起、激活等功能。
 * 集成缓存机制提升查询性能，支持租户隔离。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "processDefinitions", allEntries = true)
    public String deploy(ProcessDefinitionDeployRequest request) {
        try {
            log.info("Deploying process definition, name={}, tenantId={}",
                    request.getName(), request.getTenantId());

            DeploymentBuilder builder = repositoryService.createDeployment()
                    .name(request.getName())
                    .source(request.getSource());

            // 设置租户ID
            if (StringUtils.hasText(request.getTenantId())) {
                builder.tenantId(request.getTenantId());
            }

            // 添加BPMN资源
            if (request.getBpmnContent() != null && request.getBpmnContent().length > 0) {
                builder.addInputStream(
                        request.getResourceName(),
                        new java.io.ByteArrayInputStream(request.getBpmnContent())
                );
            }

            // 启用重复过滤
            if (request.isEnableDuplicateFiltering()) {
                builder.enableDuplicateFiltering(request.isDeployChangedOnly());
            }

            Deployment deployment = builder.deploy();

            log.info("Process definition deployed successfully, deploymentId={}", deployment.getId());
            return deployment.getId();
        } catch (Exception ex) {
            log.error("Failed to deploy process definition, name={}", request.getName(), ex);
            throw new CamundaServiceException("部署流程定义失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProcessDefinitionDTO> page(ProcessDefinitionPageQuery query) {
        try {
            // 验证分页参数
            int pageNum = Math.max(1, query.getPageNum());
            int pageSize = Math.min(Math.max(1, query.getPageSize()), PaginationConstants.MAX_PAGE_SIZE);

            log.info("Querying process definitions, page={}, size={}", pageNum, pageSize);

            ProcessDefinitionQuery definitionQuery = repositoryService
                    .createProcessDefinitionQuery();

            // 应用查询条件
            applyQueryFilters(definitionQuery, query);

            // 统计总数
            long total = definitionQuery.count();

            // 分页查询
            int firstResult = (pageNum - 1) * pageSize;
            List<ProcessDefinition> definitions = definitionQuery
                    .orderByProcessDefinitionKey().asc()
                    .orderByProcessDefinitionVersion().desc()
                    .listPage(firstResult, pageSize);

            // 转换为DTO
            List<ProcessDefinitionDTO> dtoList = definitions.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return PageResult.of(dtoList, total, (long)pageNum, (long)pageSize);
        } catch (Exception ex) {
            log.error("Failed to query process definitions", ex);
            throw new CamundaServiceException("查询流程定义失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "processDefinitions", key = "'detail_' + #definitionId")
    public ProcessDefinitionDetailDTO detail(String definitionId) {
        try {
            log.info("Getting process definition detail, definitionId={}", definitionId);

            ProcessDefinition definition = repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionId(definitionId)
                    .singleResult();

            if (definition == null) {
                throw new CamundaServiceException("流程定义不存在: " + definitionId);
            }

            return convertToDetailDTO(definition);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get process definition detail, definitionId={}", definitionId, ex);
            throw new CamundaServiceException("获取流程定义详情失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 删除流程部署（完整参数版）
     *
     * @param deploymentId         部署 ID
     * @param cascade              是否级联删除关联的流程实例
     * @param skipCustomListeners  是否跳过自定义监听器
     * @param skipIoMappings       是否跳过 IO 映射
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "processDefinitions", allEntries = true)
    public void deleteDeployment(String deploymentId, boolean cascade, boolean skipCustomListeners, boolean skipIoMappings) {
        try {
            log.info("Deleting deployment [deploymentId={}, cascade={}, skipCustomListeners={}, skipIoMappings={}]",
                    deploymentId, cascade, skipCustomListeners, skipIoMappings);

            repositoryService.deleteDeployment(deploymentId, cascade, skipCustomListeners, skipIoMappings);

            log.info("Deployment deleted successfully [deploymentId={}]", deploymentId);
        } catch (Exception ex) {
            log.error("Failed to delete deployment [deploymentId={}]", deploymentId, ex);
            throw new CamundaServiceException("删除部署失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 下载 BPMN XML 资源
     * <p>委托给 {@link #download(String)} 方法实现</p>
     *
     * @param processDefinitionId 流程定义 ID
     * @return 二进制载荷
     */
    @Override
    @Transactional(readOnly = true)
    public BinaryPayload downloadBpmn(String processDefinitionId) {
        return download(processDefinitionId);
    }

    /**
     * 下载流程图资源
     * <p>委托给 {@link #diagram(String)} 方法实现</p>
     *
     * @param processDefinitionId 流程定义 ID
     * @return 二进制载荷
     */
    @Override
    @Transactional(readOnly = true)
    public BinaryPayload downloadDiagram(String processDefinitionId) {
        return diagram(processDefinitionId);
    }

    /**
     * 启动流程实例
     * <p>
     * 支持通过流程定义 ID 或流程定义 Key 启动流程实例。
     * 如果只提供 Key，则自动查找最新版本的流程定义。
     * 支持多租户环境，会根据请求中的租户 ID 进行过滤。
     * </p>
     *
     * @param request 启动请求参数
     * @return 流程实例信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceDTO startInstance(ProcessDefinitionStartRequest request) {
        String definitionId = request.getProcessDefinitionId();

        // 如果未提供 definitionId 但提供了 key，则查找最新版本
        if (!StringUtils.hasText(definitionId) && StringUtils.hasText(request.getProcessDefinitionKey())) {
            ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(request.getProcessDefinitionKey())
                    .latestVersion();

            // 支持多租户过滤
            if (StringUtils.hasText(request.getTenantId())) {
                query.tenantIdIn(request.getTenantId());
            }

            ProcessDefinition definition = query.singleResult();

            if (definition == null) {
                String errorDetail = StringUtils.hasText(request.getTenantId())
                        ? "流程定义不存在: key=" + request.getProcessDefinitionKey() + ", tenantId=" + request.getTenantId()
                        : "流程定义不存在: key=" + request.getProcessDefinitionKey();
                throw new CamundaServiceException("DEFINITION_NOT_FOUND", errorDetail);
            }
            definitionId = definition.getId();
        }

        // 验证必须提供有效的流程定义标识
        if (!StringUtils.hasText(definitionId)) {
            throw new CamundaServiceException("INVALID_REQUEST", "必须提供流程定义 ID 或 Key");
        }

        return start(definitionId, request);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "processDefinitions", allEntries = true)
    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        try {
            log.info("Deleting deployment, deploymentId={}, cascade={}", deploymentId, cascade);

            repositoryService.deleteDeployment(deploymentId, cascade);

            log.info("Deployment deleted successfully, deploymentId={}", deploymentId);
        } catch (Exception ex) {
            log.error("Failed to delete deployment, deploymentId={}", deploymentId, ex);
            throw new CamundaServiceException("删除部署失败: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public BinaryPayload download(String definitionId) {
        try {
            log.info("Downloading BPMN resource, definitionId={}", definitionId);

            ProcessDefinition definition = repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionId(definitionId)
                    .singleResult();

            if (definition == null) {
                throw new CamundaServiceException("流程定义不存在: " + definitionId);
            }

            byte[] content;
            try (InputStream inputStream = repositoryService.getResourceAsStream(
                    definition.getDeploymentId(), definition.getResourceName())) {
                content = inputStream.readAllBytes();
            }

            String fileName = definition.getResourceName();

            return new BinaryPayload(content, fileName, "application/xml");
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to download BPMN resource, definitionId={}", definitionId, ex);
            throw new CamundaServiceException("下载BPMN资源失败: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public BinaryPayload diagram(String definitionId) {
        try {
            log.info("Getting process diagram, definitionId={}", definitionId);

            ProcessDefinition definition = repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionId(definitionId)
                    .singleResult();

            if (definition == null) {
                throw new CamundaServiceException("流程定义不存在: " + definitionId);
            }

            if (definition.getDiagramResourceName() == null) {
                throw new CamundaServiceException("流程定义没有关联的图表资源");
            }

            byte[] content;
            try (InputStream inputStream = repositoryService.getResourceAsStream(
                    definition.getDeploymentId(), definition.getDiagramResourceName())) {
                content = inputStream.readAllBytes();
            }

            String fileName = definition.getDiagramResourceName();
            String contentType = fileName.endsWith(".svg") ? "image/svg+xml" : "image/png";

            return new BinaryPayload(content, fileName, contentType);
        } catch (CamundaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to get process diagram, definitionId={}", definitionId, ex);
            throw new CamundaServiceException("获取流程图失败: " + ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ProcessInstanceDTO start(String definitionId, ProcessDefinitionStartRequest request) {
        try {
            log.info("Starting process instance, definitionId={}, businessKey={}",
                    definitionId, request.getBusinessKey());

            ProcessInstance instance = runtimeService.startProcessInstanceById(
                    definitionId,
                    request.getBusinessKey(),
                    request.getVariables()
            );

            log.info("Process instance started successfully, instanceId={}", instance.getId());

            return convertInstanceToDTO(instance);
        } catch (Exception ex) {
            log.error("Failed to start process instance, definitionId={}", definitionId, ex);
            throw new CamundaServiceException("启动流程实例失败: " + ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "processDefinitions", allEntries = true)
    @Override
    public void suspend(String definitionId, boolean includeInstances) {
        try {
            log.info("Suspending process definition, definitionId={}, includeInstances={}",
                    definitionId, includeInstances);

            repositoryService.suspendProcessDefinitionById(definitionId, includeInstances, null);

            log.info("Process definition suspended successfully, definitionId={}", definitionId);
        } catch (Exception ex) {
            log.error("Failed to suspend process definition, definitionId={}", definitionId, ex);
            throw new CamundaServiceException("挂起流程定义失败: " + ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "processDefinitions", allEntries = true)
    @Override
    public void activate(String definitionId, boolean includeInstances) {
        try {
            log.info("Activating process definition, definitionId={}, includeInstances={}",
                    definitionId, includeInstances);

            repositoryService.activateProcessDefinitionById(definitionId, includeInstances, null);

            log.info("Process definition activated successfully, definitionId={}", definitionId);
        } catch (Exception ex) {
            log.error("Failed to activate process definition, definitionId={}", definitionId, ex);
            throw new CamundaServiceException("激活流程定义失败: " + ex.getMessage(), ex);
        }
    }

    // ========== 私有辅助方法 ==========

    private void applyQueryFilters(ProcessDefinitionQuery query, ProcessDefinitionPageQuery pageQuery) {
        if (StringUtils.hasText(pageQuery.getKey())) {
            query.processDefinitionKeyLike("%" + pageQuery.getKey() + "%");
        }
        if (StringUtils.hasText(pageQuery.getName())) {
            query.processDefinitionNameLike("%" + pageQuery.getName() + "%");
        }
        if (StringUtils.hasText(pageQuery.getTenantId())) {
            query.tenantIdIn(pageQuery.getTenantId());
        }
        if (pageQuery.isLatestOnly()) {
            query.latestVersion();
        }
        if (pageQuery.isSuspended() != null) {
            if (pageQuery.isSuspended()) {
                query.suspended();
            } else {
                query.active();
            }
        }
    }

    private ProcessDefinitionDTO convertToDTO(ProcessDefinition definition) {
        ProcessDefinitionDTO dto = new ProcessDefinitionDTO();
        dto.setId(definition.getId());
        dto.setKey(definition.getKey());
        dto.setName(definition.getName());
        dto.setVersion(definition.getVersion());
        dto.setDescription(definition.getDescription());
        dto.setCategory(definition.getCategory());
        dto.setDeploymentId(definition.getDeploymentId());
        dto.setResourceName(definition.getResourceName());
        dto.setDiagramResourceName(definition.getDiagramResourceName());
        dto.setSuspended(definition.isSuspended());
        dto.setTenantId(definition.getTenantId());
        dto.setVersionTag(definition.getVersionTag());
        dto.setHistoryTimeToLive(definition.getHistoryTimeToLive());
        dto.setStartableInTasklist(definition.isStartableInTasklist());
        return dto;
    }

    private ProcessDefinitionDetailDTO convertToDetailDTO(ProcessDefinition definition) {
        ProcessDefinitionDetailDTO dto = new ProcessDefinitionDetailDTO();
        dto.setId(definition.getId());
        dto.setKey(definition.getKey());
        dto.setName(definition.getName());
        dto.setVersion(definition.getVersion());
        dto.setDescription(definition.getDescription());
        dto.setCategory(definition.getCategory());
        dto.setDeploymentId(definition.getDeploymentId());
        dto.setResourceName(definition.getResourceName());
        dto.setDiagramResourceName(definition.getDiagramResourceName());
        dto.setSuspended(definition.isSuspended());
        dto.setTenantId(definition.getTenantId());
        dto.setVersionTag(definition.getVersionTag());
        dto.setHistoryTimeToLive(definition.getHistoryTimeToLive());
        dto.setStartableInTasklist(definition.isStartableInTasklist());
        dto.setHasStartFormKey(definition.hasStartFormKey());

        // 获取部署信息
        Deployment deployment = repositoryService.createDeploymentQuery()
                .deploymentId(definition.getDeploymentId())
                .singleResult();
        if (deployment != null) {
            dto.setDeploymentTime(com.basebackend.scheduler.util.DateTimeUtil.toLocalDateTime(deployment.getDeploymentTime()));
            dto.setDeploymentSource(deployment.getSource());
        }

        return dto;
    }

    private ProcessInstanceDTO convertInstanceToDTO(ProcessInstance instance) {
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

    @Override
    public void updateSuspensionState(String processDefinitionId, com.basebackend.scheduler.camunda.dto.ProcessDefinitionStateRequest request, boolean suspend) {
        try {
            if (suspend) {
                repositoryService.suspendProcessDefinitionById(processDefinitionId);
                log.info("Process definition suspended: {}", processDefinitionId);
            } else {
                repositoryService.activateProcessDefinitionById(processDefinitionId);
                log.info("Process definition activated: {}", processDefinitionId);
            }
        } catch (Exception ex) {
            log.error("Failed to update suspension state for process definition: {}", processDefinitionId, ex);
            throw new CamundaServiceException("更新流程定义状态失败: " + ex.getMessage(), ex);
        }
    }
}
