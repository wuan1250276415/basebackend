package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.HistoricActivityInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDetailDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceStatusDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceHistoryQuery;
import com.basebackend.scheduler.camunda.dto.SimplePageQuery;
import com.basebackend.scheduler.camunda.dto.UserOperationLogDTO;
import com.basebackend.scheduler.camunda.service.HistoricProcessInstanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HistoricProcessInstanceController 集成测试
 *
 * <p>测试历史流程实例管理相关的 API 端点，包括：
 * <ul>
 *   <li>历史流程实例分页查询</li>
 *   <li>历史流程实例详情查看</li>
 *   <li>历史流程实例状态查询</li>
 *   <li>历史流程实例活动历史查询</li>
 *   <li>历史流程实例审计日志查询</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@WebMvcTest(HistoricProcessInstanceController.class)
public class HistoricProcessInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HistoricProcessInstanceService historicProcessInstanceService;

    // ========== 分页查询测试 ==========

    @Test
    public void testPageQuery_Success() throws Exception {
        // 准备测试数据
        ProcessInstanceHistoryQuery query = new ProcessInstanceHistoryQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setTenantId("tenant_001");

        List<HistoricProcessInstanceDTO> instances = createTestHistoricProcessInstances();
        PageResult<HistoricProcessInstanceDTO> pageResult = PageResult.of(instances, 50L, 1L, 10L);

        when(historicProcessInstanceService.page(any(ProcessInstanceHistoryQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/history/process-instances")
                .param("current", "1")
                .param("size", "10")
                .param("tenantId", "tenant_001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(50))
                .andExpect(jsonPath("$.data.data.length()").value(3));
    }

    @Test
    public void testPageQuery_WithFilters() throws Exception {
        // 准备测试数据
        ProcessInstanceHistoryQuery query = new ProcessInstanceHistoryQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setBusinessKey("ORDER_12345");
        query.setProcessDefinitionKey("order_approval");
        query.setStartedBy("alice@example.com");
        query.setFinished(false);

        List<HistoricProcessInstanceDTO> instances = createTestHistoricProcessInstances();
        PageResult<HistoricProcessInstanceDTO> pageResult = PageResult.of(instances, 20L, 1L, 10L);

        when(historicProcessInstanceService.page(any(ProcessInstanceHistoryQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/history/process-instances")
                .param("current", "1")
                .param("size", "10")
                .param("businessKey", "ORDER_12345")
                .param("processDefinitionKey", "order_approval")
                .param("startedBy", "alice@example.com")
                .param("finished", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(20));
    }

    @Test
    public void testPageQuery_InvalidPagination() throws Exception {
        // 测试负数页码
        mockMvc.perform(get("/api/camunda/history/process-instances")
                .param("current", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 测试超过上限的页大小
        mockMvc.perform(get("/api/camunda/history/process-instances")
                .param("current", "1")
                .param("size", "1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ========== 详情查询测试 ==========

    @Test
    public void testDetail_Success() throws Exception {
        // 准备测试数据
        HistoricProcessInstanceDetailDTO detail = createTestHistoricProcessInstanceDetail();
        when(historicProcessInstanceService.detail(anyString()))
                .thenReturn(detail);

        // 执行测试
        mockMvc.perform(get("/api/camunda/history/process-instances/instance_12345")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("instance_12345"))
                .andExpect(jsonPath("$.data.processDefinitionKey").value("order_approval"))
                .andExpect(jsonPath("$.data.businessKey").value("ORDER_12345"))
                .andExpect(jsonPath("$.data.startUserId").value("alice@example.com"))
                .andExpect(jsonPath("$.data.state").value("COMPLETED"));
    }

    @Test
    public void testDetail_NotFound() throws Exception {
        when(historicProcessInstanceService.detail(anyString()))
                .thenReturn(null);

        mockMvc.perform(get("/api/camunda/history/process-instances/nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== 状态查询测试 ==========

    @Test
    public void testStatus_Success() throws Exception {
        // 准备测试数据
        HistoricProcessInstanceStatusDTO status = new HistoricProcessInstanceStatusDTO();
        status.setInstanceId("instance_12345");
        status.setState("COMPLETED");
        status.setCompleted(true);
        status.setEnded(Instant.now());
        status.setDurationInMillis(3600000L);

        when(historicProcessInstanceService.status(anyString()))
                .thenReturn(status);

        // 执行测试
        mockMvc.perform(get("/api/camunda/history/process-instances/instance_12345/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.instanceId").value("instance_12345"))
                .andExpect(jsonPath("$.data.state").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.durationInMillis").value(3600000L));
    }

    // ========== 活动历史测试 ==========

    @Test
    public void testActivities_Success() throws Exception {
        // 准备测试数据
        SimplePageQuery pageQuery = new SimplePageQuery();
        pageQuery.setCurrent(1);
        pageQuery.setSize(10);

        List<HistoricActivityInstanceDTO> activities = createTestHistoricActivityInstances();
        PageResult<HistoricActivityInstanceDTO> pageResult = PageResult.of(activities, 30L, 1L, 10L);

        when(historicProcessInstanceService.activities(anyString(), any(ProcessInstanceHistoryQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/history/process-instances/instance_12345/activities")
                .param("current", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(30))
                .andExpect(jsonPath("$.data.data.length()").value(3));
    }

    @Test
    public void testActivities_InvalidPagination() throws Exception {
        // 测试负数页码
        mockMvc.perform(get("/api/camunda/history/process-instances/instance_12345/activities")
                .param("current", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ========== 审计日志测试 ==========

    @Test
    public void testAuditLogs_Success() throws Exception {
        // 准备测试数据
        SimplePageQuery pageQuery = new SimplePageQuery();
        pageQuery.setCurrent(1);
        pageQuery.setSize(10);

        List<UserOperationLogDTO> logs = createTestUserOperationLogs();
        PageResult<UserOperationLogDTO> pageResult = PageResult.of(logs, 20L, 1L, 10L);

        when(historicProcessInstanceService.auditLogs(anyString(), any(ProcessInstanceHistoryQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/history/process-instances/instance_12345/audit-logs")
                .param("current", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(20))
                .andExpect(jsonPath("$.data.data.length()").value(2));
    }

    @Test
    public void testAuditLogs_InvalidPagination() throws Exception {
        // 测试零页大小
        mockMvc.perform(get("/api/camunda/history/process-instances/instance_12345/audit-logs")
                .param("current", "1")
                .param("size", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ========== 辅助方法 ==========

    private List<HistoricProcessInstanceDTO> createTestHistoricProcessInstances() {
        List<HistoricProcessInstanceDTO> instances = new ArrayList<>();

        HistoricProcessInstanceDTO instance1 = new HistoricProcessInstanceDTO();
        instance1.setId("instance_12345");
        instance1.setProcessDefinitionKey("order_approval");
        instance1.setProcessDefinitionName("订单审批流程");
        instance1.setBusinessKey("ORDER_12345");
        instance1.setTenantId("tenant_001");
        instance1.setStartUserId("alice@example.com");
        instance1.setStartTime(Instant.now().minusSeconds(3600));
        instance1.setEndTime(Instant.now());
        instance1.setState("COMPLETED");
        instance1.setDurationInMillis(3600000L);
        instances.add(instance1);

        HistoricProcessInstanceDTO instance2 = new HistoricProcessInstanceDTO();
        instance2.setId("instance_67890");
        instance2.setProcessDefinitionKey("report_process");
        instance2.setProcessDefinitionName("报告流程");
        instance2.setBusinessKey("REPORT_67890");
        instance2.setTenantId("tenant_001");
        instance2.setStartUserId("bob@example.com");
        instance2.setStartTime(Instant.now().minusSeconds(7200));
        instance2.setEndTime(Instant.now().minusSeconds(3600));
        instance2.setState("COMPLETED");
        instance2.setDurationInMillis(3600000L);
        instances.add(instance2);

        HistoricProcessInstanceDTO instance3 = new HistoricProcessInstanceDTO();
        instance3.setId("instance_11111");
        instance3.setProcessDefinitionKey("payment_process");
        instance3.setProcessDefinitionName("付款流程");
        instance3.setBusinessKey("PAYMENT_11111");
        instance3.setTenantId("tenant_001");
        instance3.setStartUserId("alice@example.com");
        instance3.setStartTime(Instant.now().minusSeconds(1800));
        instance3.setEndTime(null);
        instance3.setState("RUNNING");
        instance3.setDurationInMillis(null);
        instances.add(instance3);

        return instances;
    }

    private HistoricProcessInstanceDetailDTO createTestHistoricProcessInstanceDetail() {
        HistoricProcessInstanceDetailDTO detail = new HistoricProcessInstanceDetailDTO();
        detail.setId("instance_12345");
        detail.setProcessDefinitionKey("order_approval");
        detail.setProcessDefinitionName("订单审批流程");
        detail.setBusinessKey("ORDER_12345");
        detail.setTenantId("tenant_001");
        detail.setStartUserId("alice@example.com");
        detail.setStartTime(Instant.now().minusSeconds(3600));
        detail.setEndTime(Instant.now());
        detail.setState("COMPLETED");
        detail.setDurationInMillis(3600000L);

        return detail;
    }

    private List<HistoricActivityInstanceDTO> createTestHistoricActivityInstances() {
        List<HistoricActivityInstanceDTO> activities = new ArrayList<>();

        HistoricActivityInstanceDTO activity1 = new HistoricActivityInstanceDTO();
        activity1.setId("activity_12345");
        activity1.setActivityId("StartEvent_1");
        activity1.setActivityName("开始");
        activity1.setActivityType("startEvent");
        activity1.setProcessInstanceId("instance_12345");
        activity1.setStartTime(Instant.now().minusSeconds(3600));
        activity1.setEndTime(Instant.now().minusSeconds(3500));
        activity1.setDurationInMillis(100L);
        activities.add(activity1);

        HistoricActivityInstanceDTO activity2 = new HistoricActivityInstanceDTO();
        activity2.setId("activity_67890");
        activity2.setActivityId("Task_1");
        activity2.setActivityName("审批任务");
        activity2.setActivityId("userTask");
        activity2.setProcessInstanceId("instance_12345");
        activity2.setStartTime(Instant.now().minusSeconds(3500));
        activity2.setEndTime(Instant.now().minusSeconds(100));
        activity2.setDurationInMillis(3400000L);
        activities.add(activity2);

        HistoricActivityInstanceDTO activity3 = new HistoricActivityInstanceDTO();
        activity3.setId("activity_11111");
        activity3.setActivityId("EndEvent_1");
        activity3.setActivityName("结束");
        activity3.setActivityType("endEvent");
        activity3.setProcessInstanceId("instance_12345");
        activity3.setStartTime(Instant.now().minusSeconds(100));
        activity3.setEndTime(Instant.now());
        activity3.setDurationInMillis(100L);
        activities.add(activity3);

        return activities;
    }

    private List<UserOperationLogDTO> createTestUserOperationLogs() {
        List<UserOperationLogDTO> logs = new ArrayList<>();

        UserOperationLogDTO log1 = new UserOperationLogDTO();
        log1.setId("log_12345");
        log1.setProcessInstanceId("instance_12345");
        log1.setUserId("alice@example.com");
        log1.setOperation("Start Process Instance");
        log1.setTime(Instant.now().minusSeconds(3600));
        log1.setDetails("启动流程实例");
        logs.add(log1);

        UserOperationLogDTO log2 = new UserOperationLogDTO();
        log2.setId("log_67890");
        log2.setProcessInstanceId("instance_12345");
        log2.setUserId("bob@example.com");
        log2.setOperation("Complete Task");
        log2.setTime(Instant.now().minusSeconds(100));
        log2.setDetails("完成任务");
        logs.add(log2);

        return logs;
    }
}
