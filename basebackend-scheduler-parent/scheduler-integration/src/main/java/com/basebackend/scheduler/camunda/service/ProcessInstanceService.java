package com.basebackend.scheduler.camunda.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDeleteRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDetailDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceHistoryQuery;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceMigrationRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstancePageQuery;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceVariablesRequest;
import com.basebackend.scheduler.camunda.dto.ProcessVariableDTO;

import com.basebackend.scheduler.camunda.dto.ProcessDefinitionStartRequest;
import java.util.List;

/**
 * 流程实例业务逻辑接口
 *
 * <p>
 * 提供流程实例相关的业务逻辑封装，包括：
 * <ul>
 * <li>流程实例查询（分页、详情）</li>
 * <li>流程实例操作（挂起、激活、删除）</li>
 * <li>流程实例变量管理（获取、设置、删除）</li>
 * <li>流程实例迁移</li>
 * <li>历史流程实例查询</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface ProcessInstanceService {

    /**
     * 分页查询流程实例
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    /**
     * 启动流程实例
     *
     * @param request 启动请求参数
     * @return 流程实例信息
     */
    ProcessInstanceDTO start(com.basebackend.scheduler.camunda.dto.ProcessDefinitionStartRequest request);

    /**
     * 分页查询流程实例
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<ProcessInstanceDTO> page(ProcessInstancePageQuery query);

    /**
     * 获取流程实例详情
     *
     * @param instanceId    流程实例 ID
     * @param withVariables 是否包含变量
     * @return 流程实例详情
     */
    ProcessInstanceDetailDTO detail(String instanceId, boolean withVariables);

    /**
     * 挂起流程实例
     *
     * @param instanceId 流程实例 ID
     */
    void suspend(String instanceId);

    /**
     * 激活流程实例
     *
     * @param instanceId 流程实例 ID
     */
    void activate(String instanceId);

    /**
     * 删除流程实例
     *
     * @param instanceId 流程实例 ID
     * @param request    删除请求参数
     */
    void delete(String instanceId, ProcessInstanceDeleteRequest request);

    /**
     * 终止流程实例（强制结束）
     * 其实是 delete 的语义别名，但通常带有 "cancelled" 状态语义
     */
    void terminate(String instanceId, String reason);

    /**
     * 保存草稿
     * 不启动流程，仅保存流程相关业务数据
     *
     * @param request 启动请求作为草稿数据
     * @return 草稿ID (businessKey)
     */
    String draft(ProcessDefinitionStartRequest request);

    /**
     * 获取流程变量
     *
     * @param instanceId 流程实例 ID
     * @param local      是否本地变量
     * @return 变量列表
     */
    List<ProcessVariableDTO> variables(String instanceId, boolean local);

    /**
     * 获取单个流程变量
     *
     * @param instanceId   流程实例 ID
     * @param variableName 变量名
     * @param local        是否本地变量
     * @return 流程变量
     */
    ProcessVariableDTO variable(String instanceId, String variableName, boolean local);

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例 ID
     * @param request    变量设置请求
     */
    void setVariables(String instanceId, ProcessInstanceVariablesRequest request);

    /**
     * 删除流程变量
     *
     * @param instanceId   流程实例 ID
     * @param variableName 变量名
     * @param local        是否本地变量
     */
    void deleteVariable(String instanceId, String variableName, boolean local);

    /**
     * 迁移流程实例
     *
     * @param instanceId 流程实例 ID
     * @param request    迁移请求参数
     */
    void migrate(String instanceId, ProcessInstanceMigrationRequest request);

    /**
     * 查询历史流程实例
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<HistoricProcessInstanceDTO> history(ProcessInstanceHistoryQuery query);
}
