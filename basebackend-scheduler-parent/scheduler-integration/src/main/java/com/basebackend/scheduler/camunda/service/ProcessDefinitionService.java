package com.basebackend.scheduler.camunda.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * 流程定义业务逻辑接口
 *
 * <p>提供流程定义相关的业务逻辑封装，包括：
 * <ul>
 *   <li>流程定义部署</li>
 *   <li>流程定义查询（分页、详情）</li>
 *   <li>流程定义删除</li>
 *   <li>流程资源下载（BPMN、流程图）</li>
 *   <li>流程实例启动</li>
 *   <li>流程定义挂起/激活</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface ProcessDefinitionService {

    /**
     * 部署流程定义
     *
     * @param request 部署请求参数
     * @return 部署结果
     */
    String deploy(ProcessDefinitionDeployRequest request);

    /**
     * 分页查询流程定义
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<ProcessDefinitionDTO> page(ProcessDefinitionPageQuery query);

    /**
     * 获取流程定义详情
     *
     * @param processDefinitionId 流程定义 ID
     * @return 流程定义详情
     */
    ProcessDefinitionDetailDTO detail(String processDefinitionId);

    /**
     * 删除流程部署
     *
     * @param deploymentId 部署 ID
     * @param cascade 是否级联删除
     * @param skipCustomListeners 是否跳过自定义监听器
     * @param skipIoMappings 是否跳过 IO 映射
     */
    void deleteDeployment(String deploymentId, boolean cascade, boolean skipCustomListeners, boolean skipIoMappings);

    /**
     * 下载 BPMN XML
     *
     * @param processDefinitionId 流程定义 ID
     * @return 二进制载荷
     */
    BinaryPayload downloadBpmn(String processDefinitionId);

    /**
     * 下载流程图
     *
     * @param processDefinitionId 流程定义 ID
     * @return 二进制载荷
     */
    BinaryPayload downloadDiagram(String processDefinitionId);

    /**
     * 启动流程实例
     *
     * @param request 启动请求参数
     * @return 流程实例信息
     */
    ProcessInstanceDTO startInstance(ProcessDefinitionStartRequest request);

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "processDefinitions", allEntries = true)
    void deleteDeployment(String deploymentId, boolean cascade);

    @Transactional(readOnly = true)
    com.basebackend.scheduler.camunda.dto.BinaryPayload download(String definitionId);

    @Transactional(readOnly = true)
    com.basebackend.scheduler.camunda.dto.BinaryPayload diagram(String definitionId);

    @Transactional(rollbackFor = Exception.class)
    ProcessInstanceDTO start(String definitionId, ProcessDefinitionStartRequest request);

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "processDefinitions", allEntries = true)
    void suspend(String definitionId, boolean includeInstances);

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "processDefinitions", allEntries = true)
    void activate(String definitionId, boolean includeInstances);

    /**
     * 更新流程定义状态（挂起/激活）
     *
     * @param processDefinitionId 流程定义 ID
     * @param request 状态请求参数
     * @param suspend 是否挂起
     */
    void updateSuspensionState(String processDefinitionId, ProcessDefinitionStateRequest request, boolean suspend);
}
