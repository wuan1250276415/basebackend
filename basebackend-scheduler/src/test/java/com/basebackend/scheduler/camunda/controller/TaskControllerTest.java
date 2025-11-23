package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.ClaimTaskRequest;
import com.basebackend.scheduler.camunda.dto.CommentDTO;
import com.basebackend.scheduler.camunda.dto.CommentRequest;
import com.basebackend.scheduler.camunda.dto.CompleteTaskRequest;
import com.basebackend.scheduler.camunda.dto.DelegateTaskRequest;
import com.basebackend.scheduler.camunda.dto.TaskDetailDTO;
import com.basebackend.scheduler.camunda.dto.TaskPageQuery;
import com.basebackend.scheduler.camunda.dto.TaskSummaryDTO;
import com.basebackend.scheduler.camunda.dto.VariableUpsertRequest;
import com.basebackend.scheduler.camunda.service.TaskManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TaskController 集成测试
 *
 * <p>测试任务管理相关的 API 端点，包括：
 * <ul>
 *   <li>任务分页查询</li>
 *   <li>任务详情查看</li>
 *   <li>任务认领</li>
 *   <li>任务释放</li>
 *   <li>完成任务</li>
 *   <li>任务委托</li>
 *   <li>任务变量管理</li>
 *   <li>任务评论管理</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskManagementService taskManagementService;

    // ========== 分页查询测试 ==========

    @Test
    public void testPageQuery_Success() throws Exception {
        // 准备测试数据
        TaskPageQuery query = new TaskPageQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setAssignee("alice@example.com");

        List<TaskSummaryDTO> tasks = createTestTasks();
        PageResult<TaskSummaryDTO> pageResult = PageResult.of(tasks, 50L, 1L, 10L);

        when(taskManagementService.page(any(TaskPageQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/tasks")
                .param("current", "1")
                .param("size", "10")
                .param("assignee", "alice@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(50))
                .andExpect(jsonPath("$.data.data.length()").value(3));
    }

    @Test
    public void testPageQuery_WithMultipleFilters() throws Exception {
        // 准备测试数据
        TaskPageQuery query = new TaskPageQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setAssignee("alice@example.com");
        query.setProcessDefinitionKey("approval_process");
        query.setTenantId("tenant_001");
        query.setCreatedAfter(Instant.now().minusSeconds(86400)); // 24小时前

        List<TaskSummaryDTO> tasks = createTestTasks();
        PageResult<TaskSummaryDTO> pageResult = PageResult.of(tasks, 25L, 1L, 10L);

        when(taskManagementService.page(any(TaskPageQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/tasks")
                .param("current", "1")
                .param("size", "10")
                .param("assignee", "alice@example.com")
                .param("processDefinitionKey", "approval_process")
                .param("tenantId", "tenant_001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(25));
    }

    @Test
    public void testPageQuery_InvalidPagination() throws Exception {
        // 测试负数页码
        mockMvc.perform(get("/api/camunda/tasks")
                .param("current", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 测试超过上限的页大小
        mockMvc.perform(get("/api/camunda/tasks")
                .param("current", "1")
                .param("size", "1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ========== 任务详情测试 ==========

    @Test
    public void testDetail_Success() throws Exception {
        // 准备测试数据
        TaskDetailDTO detail = createTestTaskDetail();
        when(taskManagementService.detail(anyString(), anyBoolean()))
                .thenReturn(detail);

        // 执行测试
        mockMvc.perform(get("/api/camunda/tasks/task_12345")
                .param("withVariables", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("task_12345"))
                .andExpect(jsonPath("$.data.name").value("测试任务"))
                .andExpect(jsonPath("$.data.assignee").value("alice@example.com"));
    }

    @Test
    public void testDetail_WithoutVariables() throws Exception {
        // 准备测试数据
        TaskDetailDTO detail = createTestTaskDetail();
        detail.setVariables(null);
        when(taskManagementService.detail(anyString(), anyBoolean()))
                .thenReturn(detail);

        // 执行测试
        mockMvc.perform(get("/api/camunda/tasks/task_12345")
                .param("withVariables", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.variables").isEmpty());
    }

    @Test
    public void testDetail_NotFound() throws Exception {
        when(taskManagementService.detail(anyString(), anyBoolean()))
                .thenReturn(null);

        mockMvc.perform(get("/api/camunda/tasks/nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== 任务认领测试 ==========

    @Test
    public void testClaim_Success() throws Exception {
        // 准备测试数据
        ClaimTaskRequest request = new ClaimTaskRequest();
        request.setUserId("bob@example.com");

        doNothing().when(taskManagementService)
                .claim(anyString(), any(ClaimTaskRequest.class));

        // 执行测试
        mockMvc.perform(post("/api/camunda/tasks/task_12345/claim")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务认领成功"));
    }

    @Test
    public void testClaim_InvalidRequest() throws Exception {
        ClaimTaskRequest request = new ClaimTaskRequest();
        request.setUserId(""); // 空用户ID

        mockMvc.perform(post("/api/camunda/tasks/task_12345/claim")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 任务释放测试 ==========

    @Test
    public void testUnclaim_Success() throws Exception {
        doNothing().when(taskManagementService)
                .unclaim(anyString());

        // 执行测试
        mockMvc.perform(post("/api/camunda/tasks/task_12345/unclaim")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务已释放"));
    }

    // ========== 完成任务测试 ==========

    @Test
    public void testComplete_Success() throws Exception {
        // 准备测试数据
        CompleteTaskRequest request = new CompleteTaskRequest();
        request.setComment("任务已完成");
        request.setApproved(true);

        doNothing().when(taskManagementService)
                .complete(anyString(), any(CompleteTaskRequest.class));

        // 执行测试
        mockMvc.perform(post("/api/camunda/tasks/task_12345/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务已完成"));
    }

    @Test
    public void testComplete_WithVariables() throws Exception {
        // 准备测试数据
        CompleteTaskRequest request = new CompleteTaskRequest();
        request.setComment("任务已完成");
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("approvalAmount", 1000.0);
        request.setVariables(variables);

        doNothing().when(taskManagementService)
                .complete(anyString(), any(CompleteTaskRequest.class));

        // 执行测试
        mockMvc.perform(post("/api/camunda/tasks/task_12345/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testComplete_InvalidRequest() throws Exception {
        CompleteTaskRequest request = new CompleteTaskRequest();

        mockMvc.perform(post("/api/camunda/tasks/task_12345/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 任务委托测试 ==========

    @Test
    public void testDelegate_Success() throws Exception {
        // 准备测试数据
        DelegateTaskRequest request = new DelegateTaskRequest();
        request.setDelegateTo("bob@example.com");
        request.setReason("工作转移");

        doNothing().when(taskManagementService)
                .delegate(anyString(), any(DelegateTaskRequest.class));

        // 执行测试
        mockMvc.perform(post("/api/camunda/tasks/task_12345/delegate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务已委托"));
    }

    @Test
    public void testDelegate_InvalidRequest() throws Exception {
        DelegateTaskRequest request = new DelegateTaskRequest();
        request.setDelegateTo(""); // 空委托对象

        mockMvc.perform(post("/api/camunda/tasks/task_12345/delegate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 任务变量测试 ==========

    @Test
    public void testGetVariables_Success() throws Exception {
        // 准备测试数据
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 1000.0);
        variables.put("approved", true);
        variables.put("comment", "审批通过");

        when(taskManagementService.variables(anyString(), anyBoolean()))
                .thenReturn(variables);

        // 执行测试
        mockMvc.perform(get("/api/camunda/tasks/task_12345/variables")
                .param("local", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.amount").value(1000.0))
                .andExpect(jsonPath("$.data.approved").value(true));
    }

    @Test
    public void testSetVariable_Success() throws Exception {
        // 准备测试数据
        VariableUpsertRequest request = new VariableUpsertRequest();
        request.setVariableName("approvalAmount");
        request.setVariableValue(2000.0);
        request.setLocal(false);

        doNothing().when(taskManagementService)
                .upsertVariable(anyString(), any(VariableUpsertRequest.class));

        // 执行测试
        mockMvc.perform(post("/api/camunda/tasks/task_12345/variables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("变量设置成功"));
    }

    @Test
    public void testSetVariable_InvalidVariable() throws Exception {
        VariableUpsertRequest request = new VariableUpsertRequest();
        request.setVariableName(""); // 空变量名

        mockMvc.perform(post("/api/camunda/tasks/task_12345/variables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteVariable_Success() throws Exception {
        // 执行测试
        mockMvc.perform(delete("/api/camunda/tasks/task_12345/variables/variableName")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("变量删除成功"));
    }

    // ========== 任务评论测试 ==========

    @Test
    public void testGetComments_Success() throws Exception {
        // 准备测试数据
        List<CommentDTO> comments = createTestComments();
        when(taskManagementService.getComments(anyString()))
                .thenReturn(comments);

        // 执行测试
        mockMvc.perform(get("/api/camunda/tasks/task_12345/comments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    public void testAddComment_Success() throws Exception {
        // 准备测试数据
        CommentRequest request = new CommentRequest();
        request.setMessage("任务已完成审批");
        request.setUserId("alice@example.com");

        CommentDTO comment = new CommentDTO();
        comment.setId("comment_12345");
        comment.setTaskId("task_12345");
        comment.setMessage("任务已完成审批");
        comment.setUserId("alice@example.com");
        comment.setTime(java.util.Date.from(Instant.now()));

        when(taskManagementService.addComment(anyString(), any(CommentRequest.class)))
                .thenReturn(comment);

        // 执行测试
        mockMvc.perform(post("/api/camunda/tasks/task_12345/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("任务已完成审批"));
    }

    // ========== 辅助方法 ==========

    private List<TaskSummaryDTO> createTestTasks() {
        List<TaskSummaryDTO> tasks = new ArrayList<>();

        TaskSummaryDTO task1 = new TaskSummaryDTO();
        task1.setId("task_12345");
        task1.setName("审批订单");
        task1.setAssignee("alice@example.com");
        task1.setProcessInstanceId("instance_12345");
        task1.setProcessDefinitionKey("approval_process");
        task1.setCreated(Instant.now());
        tasks.add(task1);

        TaskSummaryDTO task2 = new TaskSummaryDTO();
        task2.setId("task_67890");
        task2.setName("审核报告");
        task2.setAssignee("bob@example.com");
        task2.setProcessInstanceId("instance_67890");
        task2.setProcessDefinitionKey("report_process");
        task2.setCreated(Instant.now());
        tasks.add(task2);

        TaskSummaryDTO task3 = new TaskSummaryDTO();
        task3.setId("task_11111");
        task3.setName("确认付款");
        task3.setAssignee("alice@example.com");
        task3.setProcessInstanceId("instance_11111");
        task3.setProcessDefinitionKey("payment_process");
        task3.setCreated(Instant.now());
        tasks.add(task3);

        return tasks;
    }

    private TaskDetailDTO createTestTaskDetail() {
        TaskDetailDTO detail = new TaskDetailDTO();
        detail.setId("task_12345");
        detail.setName("审批订单");
        detail.setDescription("审批订单金额");
        detail.setAssignee("alice@example.com");
        detail.setOwner("manager@example.com");
        detail.setProcessInstanceId("instance_12345");
        detail.setProcessDefinitionKey("approval_process");
        detail.setCreated(Instant.now());
        detail.setDue(Instant.now().plusSeconds(86400));
        detail.setPriority(50);
        detail.setSuspended(false);

        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 1000.0);
        variables.put("approved", false);
        detail.setVariables(variables);

        return detail;
    }

    private List<CommentDTO> createTestComments() {
        List<CommentDTO> comments = new ArrayList<>();

        CommentDTO comment1 = new CommentDTO();
        comment1.setId("comment_12345");
        comment1.setTaskId("task_12345");
        comment1.setMessage("任务开始处理");
        comment1.setUserId("alice@example.com");
        comment1.setTime(java.util.Date.from(Instant.now().minusSeconds(3600)));
        comments.add(comment1);

        CommentDTO comment2 = new CommentDTO();
        comment2.setId("comment_67890");
        comment2.setTaskId("task_12345");
        comment2.setMessage("已完成审核");
        comment2.setUserId("bob@example.com");
        comment2.setTime(java.util.Date.from(Instant.now().minusSeconds(1800)));
        comments.add(comment2);

        return comments;
    }
}
