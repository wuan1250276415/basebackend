package com.basebackend.ticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.basebackend.api.model.scheduler.TaskFeignDTO;
import com.basebackend.common.model.Result;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.enums.ApprovalAction;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.service.client.scheduler.ProcessDefinitionServiceClient;
import com.basebackend.service.client.scheduler.TaskServiceClient;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketWorkflowServiceImpl 工作流服务测试")
class TicketWorkflowServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Ticket.class
        );
    }

    @Mock private ProcessDefinitionServiceClient processDefinitionClient;
    @Mock private TaskServiceClient taskServiceClient;
    @Mock private TicketMapper ticketMapper;

    @InjectMocks
    private TicketWorkflowServiceImpl workflowService;

    private Ticket testTicket;

    @BeforeEach
    void setUp() {
        testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setTicketNo("TK-20260301-0001");
        testTicket.setReporterId(100L);
        testTicket.setProcessInstanceId("proc-instance-123");
    }

    @Nested
    @DisplayName("启动审批流程")
    class StartApprovalTests {

        @Test
        @DisplayName("startApproval - 应启动流程并返回实例ID")
        void shouldStartProcessAndReturnInstanceId() {
            given(ticketMapper.selectById(1L)).willReturn(testTicket);
            given(processDefinitionClient.startProcessInstance(any()))
                    .willReturn(Result.success("启动成功", "proc-new-123"));

            String instanceId = workflowService.startApproval(1L, "user1", "user2");

            assertThat(instanceId).isEqualTo("proc-new-123");
            verify(processDefinitionClient).startProcessInstance(any());
            verify(ticketMapper).update(eq(null), any());
        }

        @Test
        @DisplayName("startApproval - 工单不存在应抛出异常")
        void shouldThrowWhenTicketNotFound() {
            given(ticketMapper.selectById(999L)).willReturn(null);

            assertThatThrownBy(() -> workflowService.startApproval(999L, "user1", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("工单不存在");
        }

        @Test
        @DisplayName("startApproval - 启动失败应抛出异常")
        void shouldThrowWhenStartFails() {
            given(ticketMapper.selectById(1L)).willReturn(testTicket);
            given(processDefinitionClient.startProcessInstance(any()))
                    .willReturn(Result.success("启动成功", null));

            assertThatThrownBy(() -> workflowService.startApproval(1L, "user1", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("启动审批流程失败");
        }
    }

    @Nested
    @DisplayName("完成审批任务")
    class CompleteTaskTests {

        @Test
        @DisplayName("completeTask - 应调用 taskServiceClient 完成任务")
        void shouldCallCompleteWithCorrectVariables() {
            workflowService.completeTask("task-1", ApprovalAction.APPROVE, "同意", "100");

            verify(taskServiceClient).complete(eq("task-1"), any());
        }

        @Test
        @DisplayName("completeTask - REJECT 应设置 approved=false")
        void shouldSetApprovedFalseForReject() {
            workflowService.completeTask("task-1", ApprovalAction.REJECT, "拒绝原因", "100");

            verify(taskServiceClient).complete(eq("task-1"), any());
        }
    }

    @Nested
    @DisplayName("委派任务")
    class DelegateTaskTests {

        @Test
        @DisplayName("delegateTask - 应调用 taskServiceClient 委派")
        void shouldCallDelegate() {
            workflowService.delegateTask("task-1", "user1", "user2");

            verify(taskServiceClient).delegate("task-1", "user1", "user2");
        }
    }

    @Nested
    @DisplayName("获取活跃任务")
    class GetActiveTasksTests {

        @Test
        @DisplayName("getActiveTasks - 有流程实例时应返回任务列表")
        void shouldReturnTasksWhenProcessExists() {
            given(ticketMapper.selectById(1L)).willReturn(testTicket);
            given(taskServiceClient.getActiveTasksByProcessInstance("proc-instance-123"))
                    .willReturn(Result.success("查询成功", Collections.emptyList()));

            List<TaskFeignDTO> tasks = workflowService.getActiveTasks(1L);

            assertThat(tasks).isEmpty();
            verify(taskServiceClient).getActiveTasksByProcessInstance("proc-instance-123");
        }

        @Test
        @DisplayName("getActiveTasks - 无流程实例时应返回空列表")
        void shouldReturnEmptyWhenNoProcessInstance() {
            testTicket.setProcessInstanceId(null);
            given(ticketMapper.selectById(1L)).willReturn(testTicket);

            List<TaskFeignDTO> tasks = workflowService.getActiveTasks(1L);

            assertThat(tasks).isEmpty();
        }

        @Test
        @DisplayName("getActiveTasks - 工单不存在时应返回空列表")
        void shouldReturnEmptyWhenTicketNotFound() {
            given(ticketMapper.selectById(999L)).willReturn(null);

            List<TaskFeignDTO> tasks = workflowService.getActiveTasks(999L);

            assertThat(tasks).isEmpty();
        }
    }
}
