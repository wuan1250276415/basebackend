package com.basebackend.scheduler.camunda.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.HistoricActivityInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDetailDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceStatusDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceHistoryQuery;
import com.basebackend.scheduler.camunda.dto.ProcessTrackingDTO;
import com.basebackend.scheduler.camunda.dto.UserOperationLogDTO;

/**
 * 历史流程实例业务逻辑接口
 *
 * <p>
 * 提供历史流程实例相关的业务逻辑封装，包括：
 * <ul>
 * <li>历史流程实例查询（分页、详情、状态）</li>
 * <li>历史活动实例查询</li>
 * <li>用户操作审计日志查询</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface HistoricProcessInstanceService {

    /**
     * 分页查询历史流程实例
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<HistoricProcessInstanceDTO> page(ProcessInstanceHistoryQuery query);

    /**
     * 获取历史流程实例详情
     *
     * @param instanceId 流程实例 ID
     * @return 历史流程实例详情
     */
    HistoricProcessInstanceDetailDTO detail(String instanceId);

    /**
     * 获取历史流程实例状态
     *
     * @param instanceId 流程实例 ID
     * @return 历史流程实例状态
     */
    HistoricProcessInstanceStatusDTO status(String instanceId);

    /**
     * 查询历史活动实例
     *
     * @param instanceId 流程实例 ID
     * @param query      分页查询参数
     * @return 活动实例分页结果
     */
    PageResult<HistoricActivityInstanceDTO> activities(String instanceId, ProcessInstanceHistoryQuery query);

    /**
     * 查询用户操作审计日志
     *
     * @param instanceId 流程实例 ID
     * @param query      分页查询参数
     * @return 审计日志分页结果
     */
    PageResult<UserOperationLogDTO> auditLogs(String instanceId, ProcessInstanceHistoryQuery query);

    /**
     * 获取流程跟踪信息（用于可视化）
     *
     * <p>
     * 返回流程实例的完整跟踪信息，包括：
     * <ul>
     * <li>BPMN XML 内容</li>
     * <li>当前活动节点列表</li>
     * <li>已完成活动节点列表</li>
     * <li>失败活动节点列表</li>
     * <li>活动历史详情</li>
     * </ul>
     *
     * @param instanceId 流程实例 ID
     * @return 流程跟踪信息
     */
    ProcessTrackingDTO getProcessTracking(String instanceId);
}
