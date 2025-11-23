package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.InstanceStatisticsDTO;
import com.basebackend.scheduler.camunda.dto.ProcessStatisticsDTO;
import com.basebackend.scheduler.camunda.dto.StatisticsQuery;
import com.basebackend.scheduler.camunda.dto.TaskStatisticsDTO;
import com.basebackend.scheduler.camunda.service.ProcessStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProcessStatisticsController 集成测试
 *
 * <p>测试流程统计分析相关的 API 端点，包括：
 * <ul>
 *   <li>流程定义统计查询</li>
 *   <li>流程实例统计查询</li>
 *   <li>任务统计查询</li>
 *   <li>工作流运行状态概览查询</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@WebMvcTest(ProcessStatisticsController.class)
public class ProcessStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessStatisticsService processStatisticsService;

    // ========== 流程定义统计测试 ==========

    @Test
    public void testProcessDefinitions_Success() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setTenantId("tenant_001");
        query.setStartTime(Instant.now().minusSeconds(86400));
        query.setEndTime(Instant.now());

        ProcessStatisticsDTO statistics = createTestProcessStatistics();
        when(processStatisticsService.processDefinitions(any(StatisticsQuery.class)))
                .thenReturn(statistics);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/process-definitions")
                .param("tenantId", "tenant_001")
                .param("startTime", String.valueOf(Instant.now().minusSeconds(86400).toEpochMilli()))
                .param("endTime", String.valueOf(Instant.now().toEpochMilli()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalDefinitions").value(5))
                .andExpect(jsonPath("$.data.activeDefinitions").value(4))
                .andExpect(jsonPath("$.data.suspendedDefinitions").value(1));
    }

    @Test
    public void testProcessDefinitions_AllTenants() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        // 不设置租户ID，查询所有租户
        ProcessStatisticsDTO statistics = createTestProcessStatistics();
        statistics.setTotalDefinitions(10L);
        statistics.setActiveDefinitions(8L);
        statistics.setSuspendedDefinitions(2L);

        when(processStatisticsService.processDefinitions(any(StatisticsQuery.class)))
                .thenReturn(statistics);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/process-definitions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalDefinitions").value(10));
    }

    @Test
    public void testProcessDefinitions_TimeRangeOnly() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setStartTime(Instant.now().minusSeconds(86400));
        query.setEndTime(Instant.now());

        ProcessStatisticsDTO statistics = createTestProcessStatistics();
        when(processStatisticsService.processDefinitions(any(StatisticsQuery.class)))
                .thenReturn(statistics);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/process-definitions")
                .param("startTime", String.valueOf(Instant.now().minusSeconds(86400).toEpochMilli()))
                .param("endTime", String.valueOf(Instant.now().toEpochMilli()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    // ========== 流程实例统计测试 ==========

    @Test
    public void testInstances_Success() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setTenantId("tenant_001");

        InstanceStatisticsDTO statistics = createTestInstanceStatistics();
        when(processStatisticsService.instances(any(StatisticsQuery.class)))
                .thenReturn(statistics);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/instances")
                .param("tenantId", "tenant_001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalInstances").value(100))
                .andExpect(jsonPath("$.data.runningInstances").value(30))
                .andExpect(jsonPath("$.data.completedInstances").value(65))
                .andExpect(jsonPath("$.data.terminatedInstances").value(5));
    }

    @Test
    public void testInstances_WithTimeRange() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setTenantId("tenant_001");
        query.setStartTime(Instant.now().minusSeconds(86400));
        query.setEndTime(Instant.now());

        InstanceStatisticsDTO statistics = createTestInstanceStatistics();
        statistics.setRunningInstances(20L);
        statistics.setCompletedInstances(40L);

        when(processStatisticsService.instances(any(StatisticsQuery.class)))
                .thenReturn(statistics);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/instances")
                .param("tenantId", "tenant_001")
                .param("startTime", String.valueOf(Instant.now().minusSeconds(86400).toEpochMilli()))
                .param("endTime", String.valueOf(Instant.now().toEpochMilli()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.runningInstances").value(20))
                .andExpect(jsonPath("$.data.completedInstances").value(40));
    }

    // ========== 任务统计测试 ==========

    @Test
    public void testTasks_Success() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setTenantId("tenant_001");

        TaskStatisticsDTO statistics = createTestTaskStatistics();
        when(processStatisticsService.tasks(any(StatisticsQuery.class)))
                .thenReturn(statistics);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/tasks")
                .param("tenantId", "tenant_001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalTasks").value(200))
                .andExpect(jsonPath("$.data.openTasks").value(50))
                .andExpect(jsonPath("$.data.completedTasks").value(140))
                .andExpect(jsonPath("$.data.overdueTasks").value(10));
    }

    @Test
    public void testTasks_ByAssignee() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setAssignee("alice@example.com");

        TaskStatisticsDTO statistics = createTestTaskStatistics();
        statistics.setTotalTasks(80L);
        statistics.setOpenTasks(20L);
        statistics.setCompletedTasks(55L);
        statistics.setOverdueTasks(5L);

        when(processStatisticsService.tasks(any(StatisticsQuery.class)))
                .thenReturn(statistics);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/tasks")
                .param("assignee", "alice@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalTasks").value(80));
    }

    @Test
    public void testTasks_ByProcessDefinitionKey() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setProcessDefinitionKey("order_approval");

        TaskStatisticsDTO statistics = createTestTaskStatistics();
        statistics.setTotalTasks(120L);

        when(processStatisticsService.tasks(any(StatisticsQuery.class)))
                .thenReturn(statistics);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/tasks")
                .param("processDefinitionKey", "order_approval")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalTasks").value(120));
    }

    // ========== 工作流概览测试 ==========

    @Test
    public void testOverview_Success() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setTenantId("tenant_001");

        Map<String, Object> overview = createTestOverview();
        when(processStatisticsService.overview(any(StatisticsQuery.class)))
                .thenReturn(overview);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/overview")
                .param("tenantId", "tenant_001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalDefinitions").value(5))
                .andExpect(jsonPath("$.data.totalInstances").value(100))
                .andExpect(jsonPath("$.data.totalTasks").value(200))
                .andExpect(jsonPath("$.data.runningInstances").value(30))
                .andExpect(jsonPath("$.data.openTasks").value(50));
    }

    @Test
    public void testOverview_AllTenants() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        // 不设置租户ID，查询所有租户
        Map<String, Object> overview = createTestOverview();
        overview.put("totalDefinitions", 10L);
        overview.put("totalInstances", 200L);
        overview.put("totalTasks", 400L);

        when(processStatisticsService.overview(any(StatisticsQuery.class)))
                .thenReturn(overview);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/overview")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalDefinitions").value(10))
                .andExpect(jsonPath("$.data.totalInstances").value(200))
                .andExpect(jsonPath("$.data.totalTasks").value(400));
    }

    @Test
    public void testOverview_WithTimeRange() throws Exception {
        // 准备测试数据
        StatisticsQuery query = new StatisticsQuery();
        query.setStartTime(Instant.now().minusSeconds(86400));
        query.setEndTime(Instant.now());

        Map<String, Object> overview = createTestOverview();
        // 时间范围内的统计值会不同
        overview.put("totalInstances", 50L);
        overview.put("totalTasks", 100L);

        when(processStatisticsService.overview(any(StatisticsQuery.class)))
                .thenReturn(overview);

        // 执行测试
        mockMvc.perform(get("/api/camunda/statistics/overview")
                .param("startTime", String.valueOf(Instant.now().minusSeconds(86400).toEpochMilli()))
                .param("endTime", String.valueOf(Instant.now().toEpochMilli()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalInstances").value(50))
                .andExpect(jsonPath("$.data.totalTasks").value(100));
    }

    // ========== 辅助方法 ==========

    private ProcessStatisticsDTO createTestProcessStatistics() {
        ProcessStatisticsDTO statistics = new ProcessStatisticsDTO();
        statistics.setTotalDefinitions(5L);
        statistics.setActiveDefinitions(4L);
        statistics.setSuspendedDefinitions(1L);
        statistics.setLatestVersionCount(3L);
        statistics.setOlderVersionCount(2L);
        return statistics;
    }

    private InstanceStatisticsDTO createTestInstanceStatistics() {
        InstanceStatisticsDTO statistics = new InstanceStatisticsDTO();
        statistics.setTotalInstances(100L);
        statistics.setRunningInstances(30L);
        statistics.setCompletedInstances(65L);
        statistics.setTerminatedInstances(5L);
        statistics.setAverageDuration(3600000L); // 1小时
        statistics.setInstancesPerDefinition(createTestInstancesPerDefinition());
        return statistics;
    }

    private TaskStatisticsDTO createTestTaskStatistics() {
        TaskStatisticsDTO statistics = new TaskStatisticsDTO();
        statistics.setTotalTasks(200L);
        statistics.setOpenTasks(50L);
        statistics.setCompletedTasks(140L);
        statistics.setOverdueTasks(10L);
        statistics.setAverageDuration(1800000L); // 30分钟
        statistics.setTasksPerAssignee(createTestTasksPerAssignee());
        statistics.setTasksPerProcessDefinition(createTestTasksPerProcessDefinition());
        return statistics;
    }

    private Map<String, Object> createTestOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalDefinitions", 5L);
        overview.put("activeDefinitions", 4L);
        overview.put("totalInstances", 100L);
        overview.put("runningInstances", 30L);
        overview.put("completedInstances", 65L);
        overview.put("terminatedInstances", 5L);
        overview.put("totalTasks", 200L);
        overview.put("openTasks", 50L);
        overview.put("completedTasks", 140L);
        overview.put("overdueTasks", 10L);
        return overview;
    }

    private Map<String, Long> createTestInstancesPerDefinition() {
        Map<String, Long> instances = new HashMap<>();
        instances.put("order_approval", 40L);
        instances.put("report_process", 35L);
        instances.put("payment_process", 25L);
        return instances;
    }

    private Map<String, Long> createTestTasksPerAssignee() {
        Map<String, Long> tasks = new HashMap<>();
        tasks.put("alice@example.com", 80L);
        tasks.put("bob@example.com", 75L);
        tasks.put("manager@example.com", 45L);
        return tasks;
    }

    private Map<String, Long> createTestTasksPerProcessDefinition() {
        Map<String, Long> tasks = new HashMap<>();
        tasks.put("order_approval", 120L);
        tasks.put("report_process", 50L);
        tasks.put("payment_process", 30L);
        return tasks;
    }
}
