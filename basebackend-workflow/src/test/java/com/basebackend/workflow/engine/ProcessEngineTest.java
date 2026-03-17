package com.basebackend.workflow.engine;

import com.basebackend.workflow.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("工作流引擎测试")
class ProcessEngineTest {

    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ProcessEngine();
    }

    private ProcessDefinition buildSimpleApproval() {
        return ProcessDefinition.builder("leave", "请假审批")
                .startNode("start", "提交申请")
                .approvalNode("leader", "主管审批", "ROLE_LEADER")
                .endNode("end", "审批完成")
                .transition("start", "leader")
                .transition("leader", "end")
                .build();
    }

    private ProcessDefinition buildMultiStepApproval() {
        return ProcessDefinition.builder("expense", "报销审批")
                .startNode("start", "提交报销")
                .approvalNode("leader", "主管审批", "ROLE_LEADER")
                .approvalNode("finance", "财务审批", "ROLE_FINANCE")
                .endNode("end", "审批完成")
                .transition("start", "leader")
                .transition("leader", "finance")
                .transition("finance", "end")
                .build();
    }

    private ProcessDefinition buildConditionApproval() {
        return ProcessDefinition.builder("purchase", "采购审批")
                .startNode("start", "提交采购")
                .conditionNode("amount_check", "金额判断", List.of(
                        ConditionBranch.of("amount <= 5000", "leader"),
                        ConditionBranch.of("amount > 5000", "director")
                ))
                .approvalNode("leader", "主管审批", "ROLE_LEADER")
                .approvalNode("director", "总监审批", "ROLE_DIRECTOR")
                .endNode("end", "审批完成")
                .transition("start", "amount_check")
                .transition("leader", "end")
                .transition("director", "end")
                .build();
    }

    // ==================== 流程定义 ====================

    @Nested
    @DisplayName("流程定义测试")
    class DefinitionTest {

        @Test
        @DisplayName("部署流程定义")
        void deploy() {
            engine.deploy(buildSimpleApproval());
            assertThat(engine.getDefinition("leave")).isNotNull();
            assertThat(engine.getDefinition("leave").getName()).isEqualTo("请假审批");
        }

        @Test
        @DisplayName("获取所有流程定义")
        void getAllDefinitions() {
            engine.deploy(buildSimpleApproval());
            engine.deploy(buildMultiStepApproval());
            assertThat(engine.getAllDefinitions()).hasSize(2);
        }

        @Test
        @DisplayName("流程定义包含正确节点数")
        void nodeCount() {
            ProcessDefinition def = buildSimpleApproval();
            assertThat(def.getNodeCount()).isEqualTo(3); // start + leader + end
        }

        @Test
        @DisplayName("无开始节点抛异常")
        void noStartNodeThrows() {
            assertThatThrownBy(() -> ProcessDefinition.builder("bad", "错误流程").build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("开始节点");
        }
    }

    // ==================== 流程实例 - 简单审批 ====================

    @Nested
    @DisplayName("简单审批流程测试")
    class SimpleApprovalTest {

        @BeforeEach
        void deploy() {
            engine.deploy(buildSimpleApproval());
        }

        @Test
        @DisplayName("启动流程实例")
        void startProcess() {
            ProcessInstance inst = engine.startProcess("leave", "张三", Map.of("days", 3));
            assertThat(inst.getInstanceId()).isNotNull();
            assertThat(inst.getProcessKey()).isEqualTo("leave");
            assertThat(inst.getInitiator()).isEqualTo("张三");
            assertThat(inst.isRunning()).isTrue();
            assertThat(inst.getCurrentNodeId()).isEqualTo("leader"); // 自动推进到审批节点
        }

        @Test
        @DisplayName("启动不存在的流程抛异常")
        void startNonexistentProcess() {
            assertThatThrownBy(() -> engine.startProcess("unknown", "user", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不存在");
        }

        @Test
        @DisplayName("审批通过 → 流程完成")
        void approveAndComplete() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.approve(inst.getInstanceId(), "李经理", "同意");

            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
            assertThat(inst.isFinished()).isTrue();
            assertThat(inst.getEndTime()).isNotNull();
            assertThat(inst.getApprovalHistory()).hasSize(1);
            assertThat(inst.getApprovalHistory().getFirst().action()).isEqualTo(ApprovalRecord.ApprovalAction.APPROVE);
        }

        @Test
        @DisplayName("审批驳回")
        void reject() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.reject(inst.getInstanceId(), "李经理", "事由不充分");

            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.REJECTED);
            assertThat(inst.isFinished()).isTrue();
            assertThat(inst.getApprovalHistory().getFirst().action()).isEqualTo(ApprovalRecord.ApprovalAction.REJECT);
        }

        @Test
        @DisplayName("取消流程")
        void cancel() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.cancel(inst.getInstanceId(), "张三");

            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.CANCELLED);
        }

        @Test
        @DisplayName("对已结束的流程操作抛异常")
        void operateOnFinished() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.approve(inst.getInstanceId(), "李经理", "ok");

            assertThatThrownBy(() -> engine.approve(inst.getInstanceId(), "王总", "ok"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已结束");
        }
    }

    // ==================== 多步审批 ====================

    @Nested
    @DisplayName("多步审批流程测试")
    class MultiStepApprovalTest {

        @BeforeEach
        void deploy() {
            engine.deploy(buildMultiStepApproval());
        }

        @Test
        @DisplayName("两级审批 → 通过")
        void twoStepApproval() {
            ProcessInstance inst = engine.startProcess("expense", "张三", null);
            assertThat(inst.getCurrentNodeId()).isEqualTo("leader");

            engine.approve(inst.getInstanceId(), "李经理", "同意");
            assertThat(inst.getCurrentNodeId()).isEqualTo("finance");
            assertThat(inst.isRunning()).isTrue();

            engine.approve(inst.getInstanceId(), "王财务", "报销已审核");
            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
            assertThat(inst.getApprovalHistory()).hasSize(2);
        }

        @Test
        @DisplayName("第二步驳回")
        void rejectAtSecondStep() {
            ProcessInstance inst = engine.startProcess("expense", "张三", null);
            engine.approve(inst.getInstanceId(), "李经理", "同意");
            engine.reject(inst.getInstanceId(), "王财务", "发票不合规");

            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.REJECTED);
            assertThat(inst.getApprovalHistory()).hasSize(2);
        }
    }

    // ==================== 条件分支 ====================

    @Nested
    @DisplayName("条件分支流程测试")
    class ConditionBranchTest {

        @BeforeEach
        void deploy() {
            engine.deploy(buildConditionApproval());
        }

        @Test
        @DisplayName("小额采购走主管审批")
        void smallAmountGoesToLeader() {
            ProcessInstance inst = engine.startProcess("purchase", "张三", Map.of("amount", 3000));
            assertThat(inst.getCurrentNodeId()).isEqualTo("leader");
        }

        @Test
        @DisplayName("大额采购走总监审批")
        void largeAmountGoesToDirector() {
            ProcessInstance inst = engine.startProcess("purchase", "张三", Map.of("amount", 10000));
            assertThat(inst.getCurrentNodeId()).isEqualTo("director");
        }

        @Test
        @DisplayName("条件分支 + 审批通过 → 完成")
        void conditionThenApprove() {
            ProcessInstance inst = engine.startProcess("purchase", "张三", Map.of("amount", 3000));
            engine.approve(inst.getInstanceId(), "李经理", "同意");
            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
        }
    }

    // ==================== 任务查询 ====================

    @Nested
    @DisplayName("任务查询测试")
    class TaskQueryTest {

        @BeforeEach
        void deploy() {
            engine.deploy(buildSimpleApproval());
        }

        @Test
        @DisplayName("按角色查询待办")
        void getTasksByRole() {
            engine.startProcess("leave", "张三", null);
            engine.startProcess("leave", "李四", null);

            List<ProcessInstance> tasks = engine.getTasksByRole("ROLE_LEADER");
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("审批后待办减少")
        void tasksDecreaseAfterApproval() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.startProcess("leave", "李四", null);

            engine.approve(inst.getInstanceId(), "经理", "ok");
            assertThat(engine.getTasksByRole("ROLE_LEADER")).hasSize(1);
        }

        @Test
        @DisplayName("查询我发起的流程")
        void getMyProcesses() {
            engine.startProcess("leave", "张三", null);
            engine.startProcess("leave", "张三", null);
            engine.startProcess("leave", "李四", null);

            assertThat(engine.getMyProcesses("张三")).hasSize(2);
            assertThat(engine.getMyProcesses("李四")).hasSize(1);
        }
    }

    // ==================== 流程变量 ====================

    @Nested
    @DisplayName("流程变量测试")
    class VariableTest {

        @BeforeEach
        void deploy() {
            engine.deploy(buildSimpleApproval());
        }

        @Test
        @DisplayName("设置和获取变量")
        void setAndGetVariable() {
            ProcessInstance inst = engine.startProcess("leave", "张三", Map.of("days", 5, "reason", "年假"));
            assertThat(inst.getVariable("days")).isEqualTo(5);
            assertThat(inst.getVariable("reason")).isEqualTo("年假");
        }

        @Test
        @DisplayName("类型安全获取变量")
        void getVariableTypeSafe() {
            ProcessInstance inst = engine.startProcess("leave", "张三", Map.of("days", 5));
            Integer days = inst.getVariable("days", Integer.class);
            assertThat(days).isEqualTo(5);
            assertThat(inst.getVariable("days", String.class)).isNull(); // 类型不匹配
        }

        @Test
        @DisplayName("运行时修改变量")
        void setVariableAtRuntime() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            inst.setVariable("urgent", true);
            assertThat(inst.getVariable("urgent")).isEqualTo(true);
        }
    }

    // ==================== 流程实例状态 ====================

    @Nested
    @DisplayName("流程实例状态测试")
    class StatusTest {

        @BeforeEach
        void deploy() {
            engine.deploy(buildSimpleApproval());
        }

        @Test
        @DisplayName("挂起和恢复")
        void suspendAndResume() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            inst.suspend();
            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.SUSPENDED);
            assertThat(inst.isRunning()).isFalse();
            assertThat(inst.isFinished()).isFalse();

            inst.resume();
            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.RUNNING);
            assertThat(inst.isRunning()).isTrue();
        }

        @Test
        @DisplayName("审批历史不可变")
        void approvalHistoryImmutable() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.approve(inst.getInstanceId(), "经理", "ok");

            assertThatThrownBy(() -> inst.getApprovalHistory().add(
                    ApprovalRecord.approve("fake", "fake", "hacker", "inject")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("流程变量视图不可变")
        void variablesImmutable() {
            ProcessInstance inst = engine.startProcess("leave", "张三", Map.of("days", 3));
            assertThatThrownBy(() -> inst.getVariables().put("hack", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== 权限校验 ====================

    @Nested
    @DisplayName("审批权限校验测试")
    class PermissionTest {

        /** ROLE_LEADER 只有 "李经理" 一人 */
        private final RoleChecker checker = (user, role) ->
                "ROLE_LEADER".equals(role) && "李经理".equals(user);

        @BeforeEach
        void deploy() {
            // 使用注入了 RoleChecker 的引擎
            engine = new ProcessEngine(checker);
            engine.deploy(buildSimpleApproval());
        }

        @Test
        @DisplayName("有权限的用户可以正常审批")
        void authorizedUserCanApprove() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.approve(inst.getInstanceId(), "李经理", "同意");
            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
        }

        @Test
        @DisplayName("无权限的用户审批时抛出 SecurityException")
        void unauthorizedUserThrows() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            assertThatThrownBy(() -> engine.approve(inst.getInstanceId(), "陌生人", "随便"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("无权操作");
        }

        @Test
        @DisplayName("未配置 RoleChecker 时任何人都可审批")
        void noCheckerAllowsAll() {
            ProcessEngine noCheckEngine = new ProcessEngine();
            noCheckEngine.deploy(buildSimpleApproval());
            ProcessInstance inst = noCheckEngine.startProcess("leave", "张三", null);
            noCheckEngine.approve(inst.getInstanceId(), "任意人", "ok");
            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
        }

        @Test
        @DisplayName("转交后被转交人可审批，原审批人不可审批")
        void transfereeCanApproveWithChecker() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.transfer(inst.getInstanceId(), "李经理", "王副理", "出差代理");

            // 原审批人不可操作（任务已转交）
            assertThatThrownBy(() -> engine.approve(inst.getInstanceId(), "李经理", "ok"))
                    .isInstanceOf(SecurityException.class);

            // 被转交人可操作
            engine.approve(inst.getInstanceId(), "王副理", "同意");
            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
        }
    }

    // ==================== 流程定义版本保护 ====================

    @Nested
    @DisplayName("流程定义版本保护测试")
    class VersionControlTest {

        @Test
        @DisplayName("无运行中实例时可重复部署同版本")
        void redeployWithoutRunningInstances() {
            engine.deploy(buildSimpleApproval());
            // 无运行中实例，允许重复部署
            assertThatCode(() -> engine.deploy(buildSimpleApproval())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("有运行中实例时部署同版本抛出异常")
        void redeployWithRunningInstancesThrows() {
            engine.deploy(buildSimpleApproval());
            engine.startProcess("leave", "张三", null); // 产生运行中实例

            assertThatThrownBy(() -> engine.deploy(buildSimpleApproval()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("版本号必须递增");
        }

        @Test
        @DisplayName("有运行中实例时部署更高版本成功")
        void redeployHigherVersionSucceeds() {
            engine.deploy(buildSimpleApproval());
            engine.startProcess("leave", "张三", null);

            ProcessDefinition v2 = ProcessDefinition.builder("leave", "请假审批")
                    .version(2)
                    .startNode("start", "提交申请")
                    .approvalNode("leader", "主管审批", "ROLE_LEADER")
                    .endNode("end", "审批完成")
                    .transition("start", "leader")
                    .transition("leader", "end")
                    .build();

            assertThatCode(() -> engine.deploy(v2)).doesNotThrowAnyException();
            assertThat(engine.getDefinition("leave").getVersion()).isEqualTo(2);
        }
    }

    // ==================== 转交 ====================

    @Nested
    @DisplayName("转交任务测试")
    class TransferTest {

        @BeforeEach
        void deploy() {
            engine.deploy(buildSimpleApproval());
        }

        @Test
        @DisplayName("转交后原角色不再有待办")
        void transferRemovesFromRole() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.transfer(inst.getInstanceId(), "李经理", "王副理", "出差代理");

            assertThat(engine.getTasksByRole("ROLE_LEADER")).isEmpty();
        }

        @Test
        @DisplayName("转交后被转交人可查询到待办")
        void transfereeCanQueryTask() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.transfer(inst.getInstanceId(), "李经理", "王副理", "出差代理");

            assertThat(engine.getTasksByUser("王副理")).hasSize(1);
            assertThat(engine.getTasksByUser("李经理")).isEmpty();
        }

        @Test
        @DisplayName("转交后被转交人可正常审批")
        void transfereeCanApprove() {
            ProcessInstance inst = engine.startProcess("leave", "张三", null);
            engine.transfer(inst.getInstanceId(), "李经理", "王副理", "出差代理");
            engine.approve(inst.getInstanceId(), "王副理", "同意");

            assertThat(inst.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
            assertThat(inst.getApprovalHistory()).hasSize(2); // transfer + approve
        }
    }

    // ==================== 条件分支异常 ====================

    @Nested
    @DisplayName("条件分支异常测试")
    class ConditionErrorTest {

        @Test
        @DisplayName("条件无匹配时 startProcess 抛出异常")
        void noMatchThrowsOnStart() {
            ProcessDefinition def = ProcessDefinition.builder("order", "订单审批")
                    .startNode("start", "提交")
                    .conditionNode("check", "金额检查", List.of(
                            ConditionBranch.of("amount > 10000", "ceo")
                            // 故意不添加 default 分支
                    ))
                    .approvalNode("ceo", "CEO审批", "ROLE_CEO")
                    .endNode("end", "完成")
                    .transition("start", "check")
                    .transition("ceo", "end")
                    .build();
            engine.deploy(def);

            assertThatThrownBy(() -> engine.startProcess("order", "张三", Map.of("amount", 100)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("条件分支无匹配");
        }
    }

    // ==================== 实例清理 ====================

    @Nested
    @DisplayName("实例清理测试")
    class CleanupTest {

        @BeforeEach
        void deploy() {
            engine.deploy(buildSimpleApproval());
        }

        @Test
        @DisplayName("cleanupFinished 清理已结束实例")
        void cleanupFinishedInstances() {
            ProcessInstance inst1 = engine.startProcess("leave", "张三", null);
            ProcessInstance inst2 = engine.startProcess("leave", "李四", null);
            engine.startProcess("leave", "王五", null); // 运行中不清理

            engine.approve(inst1.getInstanceId(), "经理", "ok");
            engine.reject(inst2.getInstanceId(), "经理", "不批");

            int cleaned = engine.cleanupFinished();
            assertThat(cleaned).isEqualTo(2);
            assertThat(engine.getTasksByRole("ROLE_LEADER")).hasSize(1); // 只剩王五的任务
        }

        @Test
        @DisplayName("cleanupFinished 不影响运行中的实例")
        void cleanupDoesNotAffectRunning() {
            engine.startProcess("leave", "张三", null);
            int cleaned = engine.cleanupFinished();
            assertThat(cleaned).isEqualTo(0);
            assertThat(engine.getTasksByRole("ROLE_LEADER")).hasSize(1);
        }
    }
}
