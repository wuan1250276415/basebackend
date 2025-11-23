package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDeleteRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDetailDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceMigrationRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceVariablesRequest;
import com.basebackend.scheduler.camunda.dto.ProcessVariableDTO;
import com.basebackend.scheduler.camunda.service.ProcessInstanceService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProcessInstanceController 集成测试
 *
 * <p>测试流程实例管理相关的 API 端点，包括：
 * <ul>
 *   <li>流程实例详情查看</li>
 *   <li>流程实例挂起/激活</li>
 *   <li>流程实例删除</li>
 *   <li>流程变量查询/设置/删除</li>
 *   <li>流程实例迁移</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@WebMvcTest(ProcessInstanceController.class)
public class ProcessInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessInstanceService processInstanceService;

    // ========== 详情查询测试 ==========

    @Test
    public void testDetail_Success() throws Exception {
        // 准备测试数据
        ProcessInstanceDetailDTO detail = createTestProcessInstanceDetail();
        when(processInstanceService.detail(anyString(), anyBoolean()))
                .thenReturn(detail);

        // 执行测试
        mockMvc.perform(get("/api/camunda/process-instances/instance_12345")
                .param("withVariables", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("instance_12345"))
                .andExpect(jsonPath("$.data.processDefinitionKey").value("order_approval"))
                .andExpect(jsonPath("$.data.businessKey").value("ORDER_12345"))
                .andExpect(jsonPath("$.data.variables.amount").value(1000.0));
    }

    @Test
    public void testDetail_WithoutVariables() throws Exception {
        // 准备测试数据
        ProcessInstanceDetailDTO detail = createTestProcessInstanceDetail();
        detail.setVariables(null);
        when(processInstanceService.detail(anyString(), anyBoolean()))
                .thenReturn(detail);

        // 执行测试
        mockMvc.perform(get("/api/camunda/process-instances/instance_12345")
                .param("withVariables", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.variables").isEmpty());
    }

    // ========== 挂起/激活测试 ==========

    @Test
    public void testSuspend_Success() throws Exception {
        doNothing().when(processInstanceService).suspend(anyString());

        // 执行测试
        mockMvc.perform(post("/api/camunda/process-instances/instance_12345/suspend")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程实例挂起成功"));
    }

    @Test
    public void testActivate_Success() throws Exception {
        doNothing().when(processInstanceService).activate(anyString());

        // 执行测试
        mockMvc.perform(post("/api/camunda/process-instances/instance_12345/activate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程实例激活成功"));
    }

    // ========== 删除测试 ==========

    @Test
    public void testDelete_WithQueryParams() throws Exception {
        doNothing().when(processInstanceService).delete(anyString(), any(ProcessInstanceDeleteRequest.class));

        // 执行测试（使用 Query Parameters）
        mockMvc.perform(delete("/api/camunda/process-instances/instance_12345")
                .param("deleteReason", "业务取消")
                .param("skipCustomListeners", "true")
                .param("skipIoMappings", "true")
                .param("externallyTerminated", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程实例删除成功"));
    }

    @Test
    public void testDelete_WithRequestBody() throws Exception {
        // 准备测试数据
        ProcessInstanceDeleteRequest request = new ProcessInstanceDeleteRequest();
        request.setDeleteReason("业务取消");
        request.setSkipCustomListeners(true);
        request.setSkipIoMappings(true);
        request.setExternallyTerminated(true);

        doNothing().when(processInstanceService).delete(anyString(), any(ProcessInstanceDeleteRequest.class));

        // 执行测试（使用 RequestBody）
        mockMvc.perform(delete("/api/camunda/process-instances/instance_12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程实例删除成功"));
    }

    @Test
    public void testDelete_WithDefaultParams() throws Exception {
        doNothing().when(processInstanceService).delete(anyString(), any(ProcessInstanceDeleteRequest.class));

        // 执行测试（使用默认参数）
        mockMvc.perform(delete("/api/camunda/process-instances/instance_12345")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 变量查询测试 ==========

    @Test
    public void testVariables_Success() throws Exception {
        // 准备测试数据
        List<ProcessVariableDTO> variables = createTestVariables();
        when(processInstanceService.variables(anyString(), anyBoolean()))
                .thenReturn(variables);

        // 执行测试
        mockMvc.perform(get("/api/camunda/process-instances/instance_12345/variables")
                .param("local", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("amount"))
                .andExpect(jsonPath("$.data[0].value").value(1000.0));
    }

    @Test
    public void testVariable_Success() throws Exception {
        // 准备测试数据
        ProcessVariableDTO variable = createTestVariables().get(0);
        when(processInstanceService.variable(anyString(), anyString(), anyBoolean()))
                .thenReturn(variable);

        // 执行测试
        mockMvc.perform(get("/api/camunda/process-instances/instance_12345/variables/amount")
                .param("local", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("amount"))
                .andExpect(jsonPath("$.data.value").value(1000.0))
                .andExpect(jsonPath("$.data.type").value("double"));
    }

    // ========== 变量设置测试 ==========

    @Test
    public void testSetVariables_Success() throws Exception {
        // 准备测试数据
        ProcessInstanceVariablesRequest request = new ProcessInstanceVariablesRequest();
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("comment", "审批通过");
        request.setVariables(variables);
        request.setLocal(false);

        doNothing().when(processInstanceService).setVariables(anyString(), any(ProcessInstanceVariablesRequest.class));

        // 执行测试
        mockMvc.perform(put("/api/camunda/process-instances/instance_12345/variables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("变量设置成功"));
    }

    @Test
    public void testSetVariables_EmptyVariables() throws Exception {
        ProcessInstanceVariablesRequest request = new ProcessInstanceVariablesRequest();
        request.setVariables(new HashMap<>());
        request.setLocal(false);

        mockMvc.perform(put("/api/camunda/process-instances/instance_12345/variables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteVariable_Success() throws Exception {
        doNothing().when(processInstanceService).deleteVariable(anyString(), anyString(), anyBoolean());

        // 执行测试
        mockMvc.perform(delete("/api/camunda/process-instances/instance_12345/variables/amount")
                .param("local", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("变量删除成功"));
    }

    // ========== 迁移测试 ==========

    @Test
    public void testMigrate_Success() throws Exception {
        // 准备测试数据
        ProcessInstanceMigrationRequest request = new ProcessInstanceMigrationRequest();
        request.setTargetProcessDefinitionId("definition_67890");
        request.setSkipCustomListeners(true);
        request.setSkipIoMappings(false);

        doNothing().when(processInstanceService).migrate(anyString(), any(ProcessInstanceMigrationRequest.class));

        // 执行测试
        mockMvc.perform(post("/api/camunda/process-instances/instance_12345/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程实例迁移完成"));
    }

    @Test
    public void testMigrate_InvalidRequest() throws Exception {
        ProcessInstanceMigrationRequest request = new ProcessInstanceMigrationRequest();
        request.setTargetProcessDefinitionId(""); // 空目标定义ID

        mockMvc.perform(post("/api/camunda/process-instances/instance_12345/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 辅助方法 ==========

    private ProcessInstanceDetailDTO createTestProcessInstanceDetail() {
        ProcessInstanceDetailDTO detail = new ProcessInstanceDetailDTO();
        detail.setId("instance_12345");
        detail.setProcessDefinitionKey("order_approval");
        detail.setProcessDefinitionId("definition_12345");
        detail.setBusinessKey("ORDER_12345");
        detail.setTenantId("tenant_001");
//        detail.setStarted(Instant.now());
//        detail.setStartUserId("alice@example.com");
//        detail.setSuperProcessInstanceId(null);
//        detail.setRootProcessInstanceId("instance_12345");
//        detail.setSuperExecutionId("execution_12345");
//        detail.setActive(true);
        detail.setSuspended(false);

        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 1000.0);
        variables.put("approved", false);
        variables.put("comment", "待审批");
        detail.setVariables(variables);

        return detail;
    }

    private List<ProcessVariableDTO> createTestVariables() {
        List<ProcessVariableDTO> variables = new ArrayList<>();

        ProcessVariableDTO var1 = new ProcessVariableDTO();
        var1.setName("amount");
        var1.setValue(1000.0);
        var1.setType("double");
//        var1.setCreateTime(Instant.now());
//        var1.setLastUpdatedTime(Instant.now());
        variables.add(var1);

        ProcessVariableDTO var2 = new ProcessVariableDTO();
        var2.setName("approved");
        var2.setValue(false);
        var2.setType("boolean");
//        var2.setCreateTime(Instant.now());
//        var2.setLastUpdatedTime(Instant.now());
        variables.add(var2);

        ProcessVariableDTO var3 = new ProcessVariableDTO();
        var3.setName("comment");
        var3.setValue("待审批");
        var3.setType("string");
//        var3.setCreateTime(Instant.now());
//        var3.setLastUpdatedTime(Instant.now());
        variables.add(var3);

        return variables;
    }
}
