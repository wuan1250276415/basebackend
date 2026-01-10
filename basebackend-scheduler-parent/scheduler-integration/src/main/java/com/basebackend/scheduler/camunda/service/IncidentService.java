package com.basebackend.scheduler.camunda.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.IncidentDTO;
import com.basebackend.scheduler.camunda.dto.IncidentPageQuery;

import java.util.List;
import java.util.Map;

/**
 * 异常事件管理业务逻辑接口
 *
 * <p>
 * 提供 Camunda 异常事件（Incident）相关的业务逻辑封装，包括：
 * <ul>
 * <li>异常事件查询（分页、详情）</li>
 * <li>异常事件操作（解决、添加注解）</li>
 * <li>异常事件统计</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface IncidentService {

    /**
     * 分页查询异常事件
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<IncidentDTO> page(IncidentPageQuery query);

    /**
     * 获取异常事件详情
     *
     * @param incidentId 异常事件 ID
     * @return 异常事件详情
     */
    IncidentDTO detail(String incidentId);

    /**
     * 获取流程实例的异常事件列表
     *
     * @param processInstanceId 流程实例 ID
     * @return 异常事件列表
     */
    List<IncidentDTO> listByProcessInstance(String processInstanceId);

    /**
     * 获取执行实例的异常事件列表
     *
     * @param executionId 执行实例 ID
     * @return 异常事件列表
     */
    List<IncidentDTO> listByExecution(String executionId);

    /**
     * 解决异常事件（通过重试关联的作业）
     *
     * <p>
     * 此方法会尝试重试与异常事件关联的失败作业，
     * 如果作业重试成功，异常事件将自动解决。
     *
     * @param incidentId 异常事件 ID
     */
    void resolve(String incidentId);

    /**
     * 为异常事件添加注解
     *
     * @param incidentId 异常事件 ID
     * @param annotation 注解内容
     */
    void setAnnotation(String incidentId, String annotation);

    /**
     * 统计异常事件数量
     *
     * @return 异常事件总数
     */
    long countIncidents();

    /**
     * 按异常类型统计
     *
     * @return 按异常类型分组的数量
     */
    Map<String, Long> countByType();

    /**
     * 按流程定义统计异常事件
     *
     * @return 按流程定义 ID 分组的异常事件数量
     */
    Map<String, Long> countByProcessDefinition();

    /**
     * 获取最近的异常事件列表
     *
     * @param maxResults 最大结果数
     * @return 异常事件列表
     */
    List<IncidentDTO> listRecentIncidents(int maxResults);
}
