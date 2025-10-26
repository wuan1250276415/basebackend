package com.basebackend.scheduler.camunda.service;

import com.basebackend.scheduler.camunda.dto.ProcessDefinitionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程定义管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDefinitionService {

    private final RepositoryService repositoryService;

    /**
     * 部署流程定义
     */
    public String deployProcessDefinition(String name, MultipartFile file) {
        try {
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
                    .name(name)
                    .addInputStream(file.getOriginalFilename(), file.getInputStream());

            Deployment deployment = deploymentBuilder.deploy();
            log.info("流程部署成功: deploymentId={}, name={}", deployment.getId(), name);
            return deployment.getId();
        } catch (Exception e) {
            log.error("流程部署失败: name={}", name, e);
            throw new RuntimeException("流程部署失败: " + e.getMessage(), e);
        }
    }

    /**
     * 部署流程定义（从classpath）
     */
    public String deployProcessDefinition(String name, String resourcePath) {
        try {
            Deployment deployment = repositoryService.createDeployment()
                    .name(name)
                    .addClasspathResource(resourcePath)
                    .deploy();

            log.info("流程部署成功: deploymentId={}, name={}", deployment.getId(), name);
            return deployment.getId();
        } catch (Exception e) {
            log.error("流程部署失败: name={}", name, e);
            throw new RuntimeException("流程部署失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询所有流程定义
     */
    public List<ProcessDefinitionDTO> listProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionName()
                .asc()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据Key查询流程定义
     */
    public ProcessDefinitionDTO getProcessDefinitionByKey(String key) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .latestVersion()
                .singleResult();

        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在: key=" + key);
        }

        return convertToDTO(processDefinition);
    }

    /**
     * 根据ID查询流程定义
     */
    public ProcessDefinitionDTO getProcessDefinitionById(String id) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(id)
                .singleResult();

        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在: id=" + id);
        }

        return convertToDTO(processDefinition);
    }

    /**
     * 挂起流程定义
     */
    public void suspendProcessDefinition(String processDefinitionId) {
        repositoryService.suspendProcessDefinitionById(processDefinitionId);
        log.info("流程定义已挂起: id={}", processDefinitionId);
    }

    /**
     * 激活流程定义
     */
    public void activateProcessDefinition(String processDefinitionId) {
        repositoryService.activateProcessDefinitionById(processDefinitionId);
        log.info("流程定义已激活: id={}", processDefinitionId);
    }

    /**
     * 删除部署（会级联删除流程定义和流程实例）
     */
    public void deleteDeployment(String deploymentId, boolean cascade) {
        repositoryService.deleteDeployment(deploymentId, cascade);
        log.info("部署已删除: deploymentId={}, cascade={}", deploymentId, cascade);
    }

    /**
     * 获取流程定义XML
     */
    public String getProcessDefinitionXml(String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);
            InputStream inputStream = repositoryService.getResourceAsStream(
                    processDefinition.getDeploymentId(),
                    processDefinition.getResourceName()
            );

            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            log.error("获取流程定义XML失败: id={}", processDefinitionId, e);
            throw new RuntimeException("获取流程定义XML失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取流程图（PNG）
     */
    public InputStream getProcessDefinitionDiagram(String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);
            return repositoryService.getResourceAsStream(
                    processDefinition.getDeploymentId(),
                    processDefinition.getDiagramResourceName()
            );
        } catch (Exception e) {
            log.error("获取流程图失败: id={}", processDefinitionId, e);
            throw new RuntimeException("获取流程图失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换为DTO
     */
    private ProcessDefinitionDTO convertToDTO(ProcessDefinition pd) {
        return ProcessDefinitionDTO.builder()
                .id(pd.getId())
                .key(pd.getKey())
                .name(pd.getName())
                .version(pd.getVersion())
                .deploymentId(pd.getDeploymentId())
                .resourceName(pd.getResourceName())
                .diagramResourceName(pd.getDiagramResourceName())
                .suspended(pd.isSuspended())
                .tenantId(pd.getTenantId())
                .versionTag(pd.getVersionTag())
                .description(pd.getDescription())
                .build();
    }
}
