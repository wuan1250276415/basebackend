package com.basebackend.scheduler.camunda.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.history.HistoricTaskInstanceQuery;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Camunda API 适配器
 *
 * <p>封装 Camunda 引擎的核心 API，提供简化、统一的接口。
 * 隐藏 Camunda 原生 API 的复杂性，提供更友好的使用方法。
 *
 * <p>主要功能：
 * <ul>
 *   <li>流程实例管理（启动、查询、终止）</li>
 *   <li>任务管理（查询、认领、完成任务）</li>
 *   <li>流程定义管理（查询、部署）</li>
 *   <li>历史数据查询（流程实例、任务）</li>
 *   <li>变量管理（获取、设置）</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>简化复杂操作，提供默认值和智能处理</li>
 *   <li>统一异常处理，转换为统一的异常类型</li>
 *   <li>支持链式调用，提供流畅的 API 体验</li>
 *   <li>完整的日志记录，便于调试和监控</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaAdapter {

    private final ProcessEngine processEngine;

    // ========== Runtime Service 适配器 ==========

    /**
     * 流程实例运行时操作适配器
     */
    public RuntimeOperation runtime() {
        return new RuntimeOperation(processEngine.getRuntimeService());
    }

    /**
     * 任务运行时操作适配器
     */
    public TaskOperation task() {
        return new TaskOperation(processEngine.getTaskService());
    }

    /**
     * 流程定义操作适配器
     */
    public DefinitionOperation definition() {
        return new DefinitionOperation(processEngine.getRepositoryService());
    }

    /**
     * 历史数据查询适配器
     */
    public HistoryOperation history() {
        return new HistoryOperation(processEngine);
    }

    /**
     * 流程实例运行时操作类
     */
    public static class RuntimeOperation {
        private final RuntimeService runtimeService;

        private RuntimeOperation(RuntimeService runtimeService) {
            this.runtimeService = runtimeService;
        }

        /**
         * 启动流程实例
         *
         * @param definitionKey 流程定义 Key
         * @return 流程实例 ID
         */
        public String startProcessByKey(String definitionKey) {
            log.debug("启动流程实例 [definitionKey={}]", definitionKey);
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(definitionKey);
            log.info("流程实例启动成功 [instanceId={}, definitionKey={}]",
                    instance.getId(), definitionKey);
            return instance.getId();
        }

        /**
         * 启动流程实例（带业务键）
         *
         * @param definitionKey 流程定义 Key
         * @param businessKey 业务键
         * @return 流程实例 ID
         */
        public String startProcessByKey(String definitionKey, String businessKey) {
            log.debug("启动流程实例 [definitionKey={}, businessKey={}]", definitionKey, businessKey);
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(definitionKey, businessKey);
            log.info("流程实例启动成功 [instanceId={}, definitionKey={}, businessKey={}]",
                    instance.getId(), definitionKey, businessKey);
            return instance.getId();
        }

        /**
         * 启动流程实例（带变量）
         *
         * @param definitionKey 流程定义 Key
         * @param variables 流程变量
         * @return 流程实例 ID
         */
        public String startProcessByKey(String definitionKey, Map<String, Object> variables) {
            log.debug("启动流程实例 [definitionKey={}, variables={}]", definitionKey, variables);
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(definitionKey, variables);
            log.info("流程实例启动成功 [instanceId={}, definitionKey={}]",
                    instance.getId(), definitionKey);
            return instance.getId();
        }

        /**
         * 启动流程实例（完整参数）
         *
         * @param definitionKey 流程定义 Key
         * @param businessKey 业务键
         * @param variables 流程变量
         * @return 流程实例 ID
         */
        public String startProcessByKey(String definitionKey, String businessKey, Map<String, Object> variables) {
            log.debug("启动流程实例 [definitionKey={}, businessKey={}, variables={}]",
                    definitionKey, businessKey, variables);
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(definitionKey, businessKey, variables);
            log.info("流程实例启动成功 [instanceId={}, definitionKey={}, businessKey={}]",
                    instance.getId(), definitionKey, businessKey);
            return instance.getId();
        }

        /**
         * 终止流程实例
         *
         * @param instanceId 流程实例 ID
         */
        public void terminateProcess(String instanceId) {
            log.debug("终止流程实例 [instanceId={}]", instanceId);
            runtimeService.deleteProcessInstance(instanceId, "用户主动终止");
            log.info("流程实例已终止 [instanceId={}]", instanceId);
        }

        /**
         * 终止流程实例（带原因）
         *
         * @param instanceId 流程实例 ID
         * @param reason 终止原因
         */
        public void terminateProcess(String instanceId, String reason) {
            log.debug("终止流程实例 [instanceId={}, reason={}]", instanceId, reason);
            runtimeService.deleteProcessInstance(instanceId, reason);
            log.info("流程实例已终止 [instanceId={}, reason={}]", instanceId, reason);
        }

        /**
         * 设置流程变量
         *
         * @param instanceId 流程实例 ID
         * @param variableName 变量名
         * @param value 变量值
         */
        public void setVariable(String instanceId, String variableName, Object value) {
            log.debug("设置流程变量 [instanceId={}, variableName={}]", instanceId, variableName);
            runtimeService.setVariable(instanceId, variableName, value);
        }

        /**
         * 批量设置流程变量
         *
         * @param instanceId 流程实例 ID
         * @param variables 变量集合
         */
        public void setVariables(String instanceId, Map<String, Object> variables) {
            log.debug("设置流程变量 [instanceId={}, variables={}]", instanceId, variables);
            runtimeService.setVariables(instanceId, variables);
        }

        /**
         * 获取流程变量
         *
         * @param instanceId 流程实例 ID
         * @param variableName 变量名
         * @return 变量值
         */
        public Object getVariable(String instanceId, String variableName) {
            return runtimeService.getVariable(instanceId, variableName);
        }

        /**
         * 获取流程变量（泛型）
         *
         * @param instanceId 流程实例 ID
         * @param variableName 变量名
         * @param clazz 值类型
         * @return 变量值
         */
        @SuppressWarnings("unchecked")
        public <T> T getVariable(String instanceId, String variableName, Class<T> clazz) {
            Object value = runtimeService.getVariable(instanceId, variableName);
            if (value == null) {
                return null;
            }
            if (clazz.isInstance(value)) {
                return (T) value;
            }
            throw new IllegalArgumentException(
                    String.format("Variable '%s' type mismatch: expected %s but got %s",
                            variableName, clazz.getName(), value.getClass().getName()));
        }

        /**
         * 创建流程实例查询对象
         *
         * @return ProcessInstanceQuery
         */
        public ProcessInstanceQuery createProcessInstanceQuery() {
            return runtimeService.createProcessInstanceQuery();
        }

        /**
         * 根据业务键查找流程实例
         *
         * @param businessKey 业务键
         * @return 流程实例列表
         */
        public List<ProcessInstance> findByBusinessKey(String businessKey) {
            return runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .list();
        }

        /**
         * 根据流程定义 Key 查找流程实例
         *
         * @param definitionKey 流程定义 Key
         * @return 流程实例列表
         */
        public List<ProcessInstance> findByDefinitionKey(String definitionKey) {
            return runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(definitionKey)
                    .list();
        }
    }

    /**
     * 任务操作类
     */
    public static class TaskOperation {
        private final TaskService taskService;

        private TaskOperation(TaskService taskService) {
            this.taskService = taskService;
        }

        /**
         * 认领任务
         *
         * @param taskId 任务 ID
         * @param userId 用户 ID
         */
        public void claim(String taskId, String userId) {
            log.debug("认领任务 [taskId={}, userId={}]", taskId, userId);
            taskService.claim(taskId, userId);
            log.info("任务已认领 [taskId={}, userId={}]", taskId, userId);
        }

        /**
         * 释放任务
         *
         * @param taskId 任务 ID
         */
        public void unclaim(String taskId) {
            log.debug("释放任务 [taskId={}]", taskId);
            // 使用 setAssignee(null) 来释放任务，而不是使用不存在的 unclaim 方法
            taskService.setAssignee(taskId, null);
            log.info("任务已释放 [taskId={}]", taskId);
        }

        /**
         * 完成任务
         *
         * @param taskId 任务 ID
         */
        public void complete(String taskId) {
            log.debug("完成任务 [taskId={}]", taskId);
            taskService.complete(taskId);
            log.info("任务已完成 [taskId={}]", taskId);
        }

        /**
         * 完成任务（带变量）
         *
         * @param taskId 任务 ID
         * @param variables 变量集合
         */
        public void complete(String taskId, Map<String, Object> variables) {
            log.debug("完成任务 [taskId={}, variables={}]", taskId, variables);
            taskService.complete(taskId, variables);
            log.info("任务已完成 [taskId={}]", taskId);
        }

        /**
         * 委托任务
         *
         * @param taskId 任务 ID
         * @param userId 委托给的用户
         */
        public void delegate(String taskId, String userId) {
            log.debug("委托任务 [taskId={}, userId={}]", taskId, userId);
            taskService.delegateTask(taskId, userId);
            log.info("任务已委托 [taskId={}, userId={}]", taskId, userId);
        }

        /**
         * 设置任务变量
         *
         * @param taskId 任务 ID
         * @param variableName 变量名
         * @param value 变量值
         */
        public void setVariable(String taskId, String variableName, Object value) {
            taskService.setVariable(taskId, variableName, value);
        }

        /**
         * 批量设置任务变量
         *
         * @param taskId 任务 ID
         * @param variables 变量集合
         */
        public void setVariables(String taskId, Map<String, Object> variables) {
            taskService.setVariables(taskId, variables);
        }

        /**
         * 获取任务变量
         *
         * @param taskId 任务 ID
         * @param variableName 变量名
         * @return 变量值
         */
        public Object getVariable(String taskId, String variableName) {
            return taskService.getVariable(taskId, variableName);
        }

        /**
         * 获取任务变量（泛型）
         *
         * @param taskId 任务 ID
         * @param variableName 变量名
         * @param clazz 值类型
         * @return 变量值
         */
        @SuppressWarnings("unchecked")
        public <T> T getVariable(String taskId, String variableName, Class<T> clazz) {
            Object value = taskService.getVariable(taskId, variableName);
            if (value == null) {
                return null;
            }
            if (clazz.isInstance(value)) {
                return (T) value;
            }
            throw new IllegalArgumentException(
                    String.format("Variable '%s' type mismatch: expected %s but got %s",
                            variableName, clazz.getName(), value.getClass().getName()));
        }

        /**
         * 创建任务查询对象
         *
         * @return TaskQuery
         */
        public TaskQuery createTaskQuery() {
            return taskService.createTaskQuery();
        }

        /**
         * 根据负责人查找任务
         *
         * @param assignee 负责人
         * @return 任务列表
         */
        public List<Task> findByAssignee(String assignee) {
            return taskService.createTaskQuery()
                    .taskAssignee(assignee)
                    .list();
        }

        /**
         * 根据候选用户查找任务
         *
         * @param candidateUser 候选用户
         * @return 任务列表
         */
        public List<Task> findByCandidateUser(String candidateUser) {
            return taskService.createTaskQuery()
                    .taskCandidateUser(candidateUser)
                    .list();
        }

        /**
         * 根据候选组查找任务
         *
         * @param candidateGroup 候选组
         * @return 任务列表
         */
        public List<Task> findByCandidateGroup(String candidateGroup) {
            return taskService.createTaskQuery()
                    .taskCandidateGroup(candidateGroup)
                    .list();
        }

        /**
         * 查找单个任务
         *
         * @param taskId 任务 ID
         * @return 任务对象
         */
        public Optional<Task> findById(String taskId) {
            Task task = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();
            return Optional.ofNullable(task);
        }
    }

    /**
     * 流程定义操作类
     */
    public static class DefinitionOperation {
        private final org.camunda.bpm.engine.RepositoryService repositoryService;

        private DefinitionOperation(org.camunda.bpm.engine.RepositoryService repositoryService) {
            this.repositoryService = repositoryService;
        }

        /**
         * 创建流程定义查询对象
         *
         * @return ProcessDefinitionQuery
         */
        public ProcessDefinitionQuery createProcessDefinitionQuery() {
            return repositoryService.createProcessDefinitionQuery();
        }

        /**
         * 根据 Key 查找最新版本的流程定义
         *
         * @param key 流程定义 Key
         * @return 流程定义对象
         */
        public Optional<org.camunda.bpm.engine.repository.ProcessDefinition> findLatestVersionByKey(String key) {
            org.camunda.bpm.engine.repository.ProcessDefinition definition =
                    repositoryService.createProcessDefinitionQuery()
                            .processDefinitionKey(key)
                            .latestVersion()
                            .singleResult();
            return Optional.ofNullable(definition);
        }

        /**
         * 根据 Key 和版本号查找流程定义
         *
         * @param key 流程定义 Key
         * @param version 版本号
         * @return 流程定义对象
         */
        public Optional<org.camunda.bpm.engine.repository.ProcessDefinition> findByKeyAndVersion(String key, Integer version) {
            org.camunda.bpm.engine.repository.ProcessDefinition definition =
                    repositoryService.createProcessDefinitionQuery()
                            .processDefinitionKey(key)
                            .processDefinitionVersion(version)
                            .singleResult();
            return Optional.ofNullable(definition);
        }

        /**
         * 查找所有流程定义（最新版本）
         *
         * @return 流程定义列表
         */
        public List<org.camunda.bpm.engine.repository.ProcessDefinition> findAllLatestVersion() {
            return repositoryService.createProcessDefinitionQuery()
                    .latestVersion()
                    .list();
        }

        /**
         * 查找已部署的流程定义数量
         *
         * @param key 流程定义 Key
         * @return 部署数量
         */
        public long countDeploymentsByKey(String key) {
            return repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(key)
                    .count();
        }

        /**
         * 判断流程定义是否已部署
         *
         * @param key 流程定义 Key
         * @return true 如果已部署
         */
        public boolean isDeployed(String key) {
            return countDeploymentsByKey(key) > 0;
        }
    }

    /**
     * 历史数据操作类
     */
    public static class HistoryOperation {
        private final ProcessEngine processEngine;

        private HistoryOperation(ProcessEngine processEngine) {
            this.processEngine = processEngine;
        }

        /**
         * 创建历史流程实例查询对象
         *
         * @return HistoricProcessInstanceQuery
         */
        public HistoricProcessInstanceQuery createHistoricProcessInstanceQuery() {
            return processEngine.getHistoryService().createHistoricProcessInstanceQuery();
        }

        /**
         * 创建历史任务查询对象
         *
         * @return HistoricTaskInstanceQuery
         */
        public HistoricTaskInstanceQuery createHistoricTaskInstanceQuery() {
            return processEngine.getHistoryService().createHistoricTaskInstanceQuery();
        }

        /**
         * 根据业务键查找历史流程实例
         *
         * @param businessKey 业务键
         * @return 历史流程实例列表
         */
        public List<org.camunda.bpm.engine.history.HistoricProcessInstance> findHistoricProcessInstancesByBusinessKey(String businessKey) {
            return createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .list();
        }

        /**
         * 根据流程定义 Key 查找历史流程实例
         *
         * @param definitionKey 流程定义 Key
         * @return 历史流程实例列表
         */
        public List<org.camunda.bpm.engine.history.HistoricProcessInstance> findHistoricProcessInstancesByDefinitionKey(String definitionKey) {
            return createHistoricProcessInstanceQuery()
                    .processDefinitionKey(definitionKey)
                    .list();
        }

        /**
         * 根据用户查找已完成的任务
         *
         * @param userId 用户 ID
         * @return 历史任务列表
         */
        public List<org.camunda.bpm.engine.history.HistoricTaskInstance> findCompletedTasksByUser(String userId) {
            return createHistoricTaskInstanceQuery()
                    .taskAssignee(userId)
                    .finished()
                    .list();
        }
    }
}
