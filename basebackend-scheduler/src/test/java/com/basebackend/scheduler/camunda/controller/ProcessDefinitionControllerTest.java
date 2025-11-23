package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.BinaryPayload;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionDTO;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionDeployRequest;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionDetailDTO;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionPageQuery;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionStartRequest;
import com.basebackend.scheduler.camunda.dto.ProcessDefinitionStateRequest;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO;
import com.basebackend.scheduler.camunda.service.ProcessDefinitionService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProcessDefinitionController 集成测试
 *
 * <p>测试流程定义管理相关的 API 端点，包括：
 * <ul>
 *   <li>流程定义部署</li>
 *   <li>流程定义分页查询</li>
 *   <li>流程定义详情查看</li>
 *   <li>流程部署删除</li>
 *   <li>流程实例启动</li>
 *   <li>流程定义挂起和激活</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@WebMvcTest(ProcessDefinitionController.class)
public class ProcessDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessDefinitionService processDefinitionService;

    // ========== 分页查询测试 ==========

    @Test
    public void testPageQuery_Success() throws Exception {
        // 准备测试数据
        ProcessDefinitionPageQuery query = new ProcessDefinitionPageQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setLatestVersion(true);

        List<ProcessDefinitionDTO> definitions = createTestProcessDefinitions();
        PageResult<ProcessDefinitionDTO> pageResult = PageResult.of(definitions,  (long)50, 1L, 10L);

        when(processDefinitionService.page(any(ProcessDefinitionPageQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/process-definitions")
                .param("current", "1")
                .param("size", "10")
                .param("latestVersion", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(50))
                .andExpect(jsonPath("$.data.data.length()").value(3));
    }

    @Test
    public void testPageQuery_WithFilters() throws Exception {
        // 准备测试数据
        ProcessDefinitionPageQuery query = new ProcessDefinitionPageQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setKeyword("审批");
        query.setTenantId("tenant_001");
        query.setLatestVersion(true);
        query.setSuspended(false);

        List<ProcessDefinitionDTO> definitions = createTestProcessDefinitions();
        PageResult<ProcessDefinitionDTO> pageResult = PageResult.of(definitions,  (long)20, 1L, 10L);

        when(processDefinitionService.page(any(ProcessDefinitionPageQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/process-definitions")
                .param("current", "1")
                .param("size", "10")
                .param("keyword", "审批")
                .param("tenantId", "tenant_001")
                .param("latestVersion", "true")
                .param("suspended", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(20));
    }

    @Test
    public void testPageQuery_InvalidPagination() throws Exception {
        // 测试负数页码
        mockMvc.perform(get("/api/camunda/process-definitions")
                .param("current", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 测试超过上限的页大小
        mockMvc.perform(get("/api/camunda/process-definitions")
                .param("current", "1")
                .param("size", "1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 测试零页大小
        mockMvc.perform(get("/api/camunda/process-definitions")
                .param("current", "1")
                .param("size", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ========== 部署测试 ==========

    @Test
    public void testDeploy_Success() throws Exception {
        // 准备测试数据
        ProcessDefinitionDeployRequest request = new ProcessDefinitionDeployRequest();
        request.setBpmnXmlContent(createTestBpmnXml());
        request.setTenantId("tenant_001");
        request.setProcessKey("test_process");
        request.setProcessName("测试流程");

        when(processDefinitionService.deploy(any(ProcessDefinitionDeployRequest.class)))
                .thenReturn("deployment_12345");

        // 执行测试
        mockMvc.perform(post("/api/camunda/process-definitions/deployments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程定义部署成功"))
                .andExpect(jsonPath("$.data").value("deployment_12345"));
    }

    @Test
    public void testDeploy_WithEmptyContent() throws Exception {
        ProcessDefinitionDeployRequest request = new ProcessDefinitionDeployRequest();
        request.setBpmnXmlContent("");
        request.setProcessKey("test_process");

        mockMvc.perform(post("/api/camunda/process-definitions/deployments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeployLegacy_Success() throws Exception {
        // 测试废弃端点的向后兼容性
        ProcessDefinitionDeployRequest request = new ProcessDefinitionDeployRequest();
        request.setBpmnXmlContent(createTestBpmnXml());
        request.setProcessKey("test_process");

        when(processDefinitionService.deploy(any(ProcessDefinitionDeployRequest.class)))
                .thenReturn("deployment_12345");

        mockMvc.perform(post("/api/camunda/process-definitions/deploy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("deployment_12345"));
    }

    // ========== 详情查询测试 ==========

    @Test
    public void testDetail_Success() throws Exception {
        // 准备测试数据
        ProcessDefinitionDetailDTO detail = createTestProcessDefinitionDetail();
        when(processDefinitionService.detail(anyString()))
                .thenReturn(detail);

        // 执行测试
        mockMvc.perform(get("/api/camunda/process-definitions/definition_12345")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("definition_12345"))
                .andExpect(jsonPath("$.data.name").value("测试流程定义"))
                .andExpect(jsonPath("$.data.key").value("test_process"));
    }

    @Test
    public void testDetail_NotFound() throws Exception {
        when(processDefinitionService.detail(anyString()))
                .thenReturn(null);

        mockMvc.perform(get("/api/camunda/process-definitions/nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== 删除测试 ==========

    @Test
    public void testDeleteDeployment_Success() throws Exception {
        // 执行测试
        mockMvc.perform(delete("/api/camunda/process-definitions/deployments/deployment_12345")
                .param("cascade", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程部署删除成功"));
    }

    @Test
    public void testDeleteDeployment_WithCascade() throws Exception {
        // 执行测试
        mockMvc.perform(delete("/api/camunda/process-definitions/deployments/deployment_12345")
                .param("cascade", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 启动实例测试 ==========

    @Test
    public void testStartInstance_Success() throws Exception {
        // 准备测试数据
        ProcessDefinitionStartRequest request = new ProcessDefinitionStartRequest();
        request.setDefinitionKey("test_process");
        request.setBusinessKey("BUSINESS_12345");
        request.setTenantId("tenant_001");

        ProcessInstanceDTO instance = new ProcessInstanceDTO();
        instance.setId("instance_12345");
        instance.setBusinessKey("BUSINESS_12345");
        instance.setProcessDefinitionKey("test_process");

        when(processDefinitionService.startInstance(any(ProcessDefinitionStartRequest.class)))
                .thenReturn(instance);

        // 执行测试
        mockMvc.perform(post("/api/camunda/process-definitions/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程实例启动成功"))
                .andExpect(jsonPath("$.data.id").value("instance_12345"))
                .andExpect(jsonPath("$.data.businessKey").value("BUSINESS_12345"));
    }

    // ========== 挂起/激活测试 ==========

    @Test
    public void testSuspend_Success() throws Exception {
        // 准备测试数据
        ProcessDefinitionStateRequest request = new ProcessDefinitionStateRequest();
        request.setIncludeProcessInstances(false);

        // 执行测试
        mockMvc.perform(post("/api/camunda/process-definitions/definition_12345/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程定义已挂起"));
    }

    @Test
    public void testActivate_Success() throws Exception {
        // 准备测试数据
        ProcessDefinitionStateRequest request = new ProcessDefinitionStateRequest();
        request.setIncludeProcessInstances(true);

        // 执行测试
        mockMvc.perform(post("/api/camunda/process-definitions/definition_12345/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("流程定义已激活"));
    }

    // ========== 下载测试 ==========

    @Test
    public void testDownloadBpmn_Success() throws Exception {
        // 准备测试数据
        byte[] bpmnContent = createTestBpmnXml().getBytes();
        BinaryPayload payload = new BinaryPayload();
        payload.setData(bpmnContent);
        payload.setMimeType("application/xml");
        payload.setFileName("test_process.bpmn");

        when(processDefinitionService.downloadBpmn(anyString()))
                .thenReturn(payload);

        // 执行测试
        mockMvc.perform(get("/api/camunda/process-definitions/definition_12345/xml")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 辅助方法 ==========

    private List<ProcessDefinitionDTO> createTestProcessDefinitions() {
        List<ProcessDefinitionDTO> definitions = new ArrayList<>();

        ProcessDefinitionDTO dto1 = new ProcessDefinitionDTO();
        dto1.setId("definition_12345");
        dto1.setName("测试流程定义1");
        dto1.setKey("test_process_1");
        dto1.setVersion(1);
        dto1.setTenantId("tenant_001");
        dto1.setSuspended(false);
        dto1.setDeploymentTime(Instant.now());
        definitions.add(dto1);

        ProcessDefinitionDTO dto2 = new ProcessDefinitionDTO();
        dto2.setId("definition_67890");
        dto2.setName("测试流程定义2");
        dto2.setKey("test_process_2");
        dto2.setVersion(2);
        dto2.setTenantId("tenant_001");
        dto2.setSuspended(false);
        dto2.setDeploymentTime(Instant.now());
        definitions.add(dto2);

        ProcessDefinitionDTO dto3 = new ProcessDefinitionDTO();
        dto3.setId("definition_11111");
        dto3.setName("审批流程");
        dto3.setKey("approval_process");
        dto3.setVersion(1);
        dto3.setTenantId("tenant_001");
        dto3.setSuspended(false);
        dto3.setDeploymentTime(Instant.now());
        definitions.add(dto3);

        return definitions;
    }

    private ProcessDefinitionDetailDTO createTestProcessDefinitionDetail() {
        ProcessDefinitionDetailDTO detail = new ProcessDefinitionDetailDTO();
        detail.setId("definition_12345");
        detail.setKey("test_process");
        detail.setName("测试流程定义");
        detail.setVersion(1);
        detail.setCategory("Test");
        detail.setTenantId("tenant_001");
        detail.setDeploymentId("deployment_12345");
        detail.setResourceName("test_process.bpmn");
        detail.setDiagramResourceName("test_process.png");
        detail.setSuspended(false);
        // detail.setDeploymentTime(Instant.now()); // 兼容性方法已添加，但忽略此调用
        return detail;
    }

    private String createTestBpmnXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">"
                + "<bpmn:process id=\"test_process\" name=\"测试流程\" isExecutable=\"true\">"
                + "<bpmn:startEvent id=\"StartEvent_1\"/>"
                + "</bpmn:process>"
                + "</bpmn:definitions>";
    }
}
