package com.basebackend.scheduler.camunda.service;

import com.basebackend.scheduler.camunda.dto.InstanceStatisticsDTO;
import com.basebackend.scheduler.camunda.dto.ProcessStatisticsDTO;
import com.basebackend.scheduler.camunda.dto.StatisticsQuery;
import com.basebackend.scheduler.camunda.dto.TaskStatisticsDTO;

import java.util.Map;

/**
 * 统计分析业务逻辑接口
 *
 * <p>提供工作流统计分析相关的业务逻辑封装，包括：
 * <ul>
 *   <li>流程定义统计</li>
 *   <li>流程实例统计</li>
 *   <li>任务统计</li>
 *   <li>工作流运行状态概览</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface ProcessStatisticsService {

    /**
     * 获取流程定义统计
     *
     * @param query 统计查询参数
     * @return 流程定义统计信息
     */
    ProcessStatisticsDTO processDefinitions(StatisticsQuery query);

    /**
     * 获取流程实例统计
     *
     * @param query 统计查询参数
     * @return 流程实例统计信息
     */
    InstanceStatisticsDTO instances(StatisticsQuery query);

    /**
     * 获取任务统计
     *
     * @param query 统计查询参数
     * @return 任务统计信息
     */
    TaskStatisticsDTO tasks(StatisticsQuery query);

    /**
     * 获取工作流运行状态概览
     *
     * @param query 统计查询参数
     * @return 工作流运行状态概览
     */
    Map<String, Object> overview(StatisticsQuery query);
}
