package com.basebackend.scheduler.camunda.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CamundaAdapter 使用示例
 *
 * <p>演示如何使用 CamundaAdapter 简化 Camunda API 的使用。
 * 包括常见的业务流程操作场景。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaAdapterExample {

    private final CamundaAdapter camundaAdapter;

    /**
     * 示例：启动审批流程
     *
     * <p>模拟一个订单审批流程的启动，包括业务键和流程变量。
     */
    public String startApprovalProcess() {
        String definitionKey = "order_approval";
        String businessKey = "ORDER_" + System.currentTimeMillis();

        // 准备流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", "12345");
        variables.put("orderAmount", 1000.0);
        variables.put("approver", "manager@example.com");

        // 启动流程实例
        String instanceId = camundaAdapter.runtime()
                .startProcessByKey(definitionKey, businessKey, variables);

        log.info("审批流程已启动 [instanceId={}, businessKey={}]", instanceId, businessKey);
        return instanceId;
    }

    /**
     * 示例：查询待办任务
     *
     * <p>查询当前用户的所有待办任务。
     */
    public void queryUserTasks() {
        String assignee = "alice@example.com";

        List<org.camunda.bpm.engine.task.Task> tasks = camundaAdapter.task()
                .findByAssignee(assignee);

        log.info("用户 {} 有 {} 个待办任务", assignee, tasks.size());

        for (org.camunda.bpm.engine.task.Task task : tasks) {
            log.info("任务: ID={}, Name={}, DefinitionKey={}",
                    task.getId(), task.getName(), task.getTaskDefinitionKey());
        }
    }

    /**
     * 示例：完成任务
     *
     * <p>处理一个审批任务并添加审批意见。
     */
    public void completeApprovalTask() {
        String taskId = "TASK_ID_12345";

        // 准备审批结果变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("comment", "审批通过");
        variables.put("approvedBy", "manager@example.com");

        // 完成任务
        camundaAdapter.task().complete(taskId, variables);

        log.info("任务已处理完成 [taskId={}]", taskId);
    }

    /**
     * 示例：处理审批拒绝
     *
     * <p>处理审批拒绝场景，设置拒绝原因并终止流程。
     */
    public void rejectApproval() {
        String taskId = "TASK_ID_67890";
        String instanceId = "PROCESS_INSTANCE_12345";

        // 设置拒绝变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("rejectReason", "订单金额超出预算");

        // 完成任务（拒绝）
        camundaAdapter.task().complete(taskId, variables);

        // 如果有后续网关判断，可能需要终止流程
        // camundaAdapter.runtime().terminateProcess(instanceId, "审批拒绝");

        log.info("审批已拒绝 [taskId={}, instanceId={}]", taskId, instanceId);
    }

    /**
     * 示例：任务委托
     *
     * <p>将任务委托给其他用户处理。
     */
    public void delegateTask() {
        String taskId = "TASK_ID_11111";
        String newAssignee = "bob@example.com";

        // 委托任务
        camundaAdapter.task().delegate(taskId, newAssignee);

        log.info("任务已委托 [taskId={}, newAssignee={}]", taskId, newAssignee);
    }

    /**
     * 示例：查询历史数据
     *
     * <p>查询某个业务键对应的历史流程实例和任务。
     */
    public void queryHistoryData() {
        String businessKey = "ORDER_12345";

        // 查询历史流程实例
        List<org.camunda.bpm.engine.history.HistoricProcessInstance> processInstances =
                camundaAdapter.history().findHistoricProcessInstancesByBusinessKey(businessKey);

        log.info("找到 {} 个历史流程实例", processInstances.size());

        // 查询已完成的任务
        List<org.camunda.bpm.engine.history.HistoricTaskInstance> completedTasks =
                camundaAdapter.history().findCompletedTasksByUser("manager@example.com");

        log.info("用户已完成 {} 个任务", completedTasks.size());
    }

    /**
     * 示例：设置流程变量
     *
     * <p>在流程执行过程中动态设置变量。
     */
    public void setProcessVariables() {
        String instanceId = "PROCESS_INSTANCE_12345";

        Map<String, Object> variables = new HashMap<>();
        variables.put("status", "IN_PROGRESS");
        variables.put("updateTime", System.currentTimeMillis());
        variables.put("updatedBy", "system");

        // 批量设置变量
        camundaAdapter.runtime().setVariables(instanceId, variables);

        log.info("流程变量已更新 [instanceId={}]", instanceId);
    }

    /**
     * 示例：获取流程变量
     *
     * <p>查询流程实例中的变量值。
     */
    public void getProcessVariables() {
        String instanceId = "PROCESS_INSTANCE_12345";

        // 获取单个变量
        Boolean approved = camundaAdapter.runtime()
                .getVariable(instanceId, "approved", Boolean.class);

        // 获取所有变量（需要通过查询）
        // List<HistoricVariableInstance> variables = ...;

        log.info("流程变量 approved = {}", approved);
    }

    /**
     * 示例：流程实例查询
     *
     * <p>根据不同条件查询流程实例。
     */
    public void queryProcessInstances() {
        String definitionKey = "order_approval";

        // 查询特定流程定义的所有实例
        List<org.camunda.bpm.engine.runtime.ProcessInstance> instances =
                camundaAdapter.runtime().findByDefinitionKey(definitionKey);

        log.info("找到 {} 个流程实例", instances.size());

        // 使用查询对象进行更复杂的查询
        long runningCount = camundaAdapter.runtime()
                .createProcessInstanceQuery()
                .processDefinitionKey(definitionKey)
                .active()
                .count();

        log.info("运行中的流程实例数量: {}", runningCount);
    }

    /**
     * 示例：任务查询
     *
     * <p>根据不同条件查询任务。
     */
    public void queryTasks() {
        // 查询候选用户的任务
        List<org.camunda.bpm.engine.task.Task> candidateTasks =
                camundaAdapter.task().findByCandidateUser("alice@example.com");

        log.info("候选任务数量: {}", candidateTasks.size());

        // 查询候选组的任务
        List<org.camunda.bpm.engine.task.Task> groupTasks =
                camundaAdapter.task().findByCandidateGroup("managers");

        log.info("组任务数量: {}", groupTasks.size());

        // 使用查询对象进行更复杂的查询
        long myTasks = camundaAdapter.task()
                .createTaskQuery()
                .taskAssignee("bob@example.com")
                .active()
                .count();

        log.info("Bob 的待办任务数量: {}", myTasks);
    }

    /**
     * 示例：流程定义查询
     *
     * <p>查询流程定义信息。
     */
    public void queryProcessDefinitions() {
        String definitionKey = "order_approval";

        // 查询最新版本
        var latestDefinition = camundaAdapter.definition()
                .findLatestVersionByKey(definitionKey);

        if (latestDefinition.isPresent()) {
            var def = latestDefinition.get();
            log.info("最新版本流程定义: ID={}, Version={}",
                    def.getId(), def.getVersion());
        }

        // 查询所有流程定义（最新版本）
        List<org.camunda.bpm.engine.repository.ProcessDefinition> allDefinitions =
                camundaAdapter.definition().findAllLatestVersion();

        log.info("系统中的流程定义数量: {}", allDefinitions.size());

        // 检查流程是否已部署
        boolean isDeployed = camundaAdapter.definition().isDeployed(definitionKey);
        log.info("流程 {} 是否已部署: {}", definitionKey, isDeployed);
    }

    /**
     * 示例：流程终止
     *
     * <p>终止一个正在运行的流程实例。
     */
    public void terminateProcessInstance() {
        String instanceId = "PROCESS_INSTANCE_12345";
        String reason = "用户主动取消订单";

        camundaAdapter.runtime().terminateProcess(instanceId, reason);

        log.info("流程实例已终止 [instanceId={}, reason={}]", instanceId, reason);
    }
}
