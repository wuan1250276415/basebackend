package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.FormTemplateCreateRequest;
import com.basebackend.scheduler.camunda.dto.FormTemplateDTO;
import com.basebackend.scheduler.camunda.dto.FormTemplatePageQuery;
import com.basebackend.scheduler.camunda.dto.FormTemplateUpdateRequest;
import com.basebackend.scheduler.camunda.service.FormTemplateService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FormTemplateController 集成测试
 *
 * <p>测试表单模板管理相关的 API 端点，包括：
 * <ul>
 *   <li>表单模板分页查询</li>
 *   <li>表单模板详情查看</li>
 *   <li>表单模板创建</li>
 *   <li>表单模板更新</li>
 *   <li>表单模板删除</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@WebMvcTest(FormTemplateController.class)
public class FormTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FormTemplateService formTemplateService;

    // ========== 分页查询测试 ==========

    @Test
    public void testPageQuery_Success() throws Exception {
        // 准备测试数据
        FormTemplatePageQuery query = new FormTemplatePageQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setTenantId("tenant_001");
        query.setFormType("APPROVAL");

        List<FormTemplateDTO> templates = createTestFormTemplates();
        PageResult<FormTemplateDTO> pageResult = PageResult.of(templates, (long)50, 1L, 10L);

        when(formTemplateService.page(any(FormTemplatePageQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/form-templates")
                .param("current", "1")
                .param("size", "10")
                .param("tenantId", "tenant_001")
                .param("templateType", "APPROVAL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(50))
                .andExpect(jsonPath("$.data.data.length()").value(3));
    }

    @Test
    public void testPageQuery_WithKeyword() throws Exception {
        // 准备测试数据
        FormTemplatePageQuery query = new FormTemplatePageQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setKeyword("审批");

        List<FormTemplateDTO> templates = createTestFormTemplates();
        PageResult<FormTemplateDTO> pageResult = PageResult.of(templates,  (long)20, 1L, 10L);

        when(formTemplateService.page(any(FormTemplatePageQuery.class)))
                .thenReturn(pageResult);

        // 执行测试
        mockMvc.perform(get("/api/camunda/form-templates")
                .param("current", "1")
                .param("size", "10")
                .param("keyword", "审批")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(20));
    }

    @Test
    public void testPageQuery_InvalidPagination() throws Exception {
        // 测试负数页码
        mockMvc.perform(get("/api/camunda/form-templates")
                .param("current", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 测试超过上限的页大小
        mockMvc.perform(get("/api/camunda/form-templates")
                .param("current", "1")
                .param("size", "1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ========== 详情查询测试 ==========

    @Test
    public void testDetail_Success() throws Exception {
        // 准备测试数据
        FormTemplateDTO template = createTestFormTemplate();
        when(formTemplateService.detail(anyLong()))
                .thenReturn(template);

        // 执行测试
        mockMvc.perform(get("/api/camunda/form-templates/12345")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(12345L))
                .andExpect(jsonPath("$.data.name").value("订单审批表单"))
                .andExpect(jsonPath("$.data.templateType").value("APPROVAL"))
                .andExpect(jsonPath("$.data.tenantId").value("tenant_001"));
    }

    @Test
    public void testDetail_NotFound() throws Exception {
        when(formTemplateService.detail(anyLong()))
                .thenReturn(null);

        mockMvc.perform(get("/api/camunda/form-templates/99999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== 创建测试 ==========

    @Test
    public void testCreate_Success() throws Exception {
        // 准备测试数据
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("新表单模板");
        request.setTemplateType("APPROVAL");
        request.setTenantId("tenant_001");
        request.setDescription("测试表单模板");
        request.setFormContent("{\"fields\": []}");

        FormTemplateDTO created = createTestFormTemplate();
        when(formTemplateService.create(any(FormTemplateCreateRequest.class)))
                .thenReturn(created);

        // 执行测试
        mockMvc.perform(post("/api/camunda/form-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.name").value("订单审批表单"));
    }

    @Test
    public void testCreate_InvalidRequest() throws Exception {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName(""); // 空名称

        mockMvc.perform(post("/api/camunda/form-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 更新测试 ==========

    @Test
    public void testUpdate_Success() throws Exception {
        // 准备测试数据
        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName("更新后的表单模板");
        request.setDescription("更新描述");
        request.setFormContent("{\"fields\": [{\"type\": \"text\"}]}");

        FormTemplateDTO updated = createTestFormTemplate();
        updated.setName("更新后的表单模板");
        when(formTemplateService.update(anyLong(), any(FormTemplateUpdateRequest.class)))
                .thenReturn(updated);

        // 执行测试
        mockMvc.perform(put("/api/camunda/form-templates/12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.name").value("更新后的表单模板"));
    }

    @Test
    public void testUpdate_InvalidRequest() throws Exception {
        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName(""); // 空名称

        mockMvc.perform(put("/api/camunda/form-templates/12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 删除测试 ==========

    @Test
    public void testDelete_Success() throws Exception {
        doNothing().when(formTemplateService).delete(anyLong());

        // 执行测试
        mockMvc.perform(delete("/api/camunda/form-templates/12345")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    // ========== 辅助方法 ==========

    private List<FormTemplateDTO> createTestFormTemplates() {
        List<FormTemplateDTO> templates = new ArrayList<>();

        FormTemplateDTO template1 = new FormTemplateDTO();
        template1.setId(12345L);
        template1.setName("订单审批表单");
        template1.setTemplateType("APPROVAL");
        template1.setTenantId("tenant_001");
        template1.setVersion(1L);
        template1.setStatus("ACTIVE");
        template1.setCreated(Instant.now());
        template1.setCreatedBy("alice@example.com");
        templates.add(template1);

        FormTemplateDTO template2 = new FormTemplateDTO();
        template2.setId(67890L);
        template2.setName("报销申请表单");
        template2.setTemplateType("APPLICATION");
        template2.setTenantId("tenant_001");
        template2.setVersion(1L);
        template2.setStatus("ACTIVE");
        template2.setCreated(Instant.now());
        template2.setCreatedBy("bob@example.com");
        templates.add(template2);

        FormTemplateDTO template3 = new FormTemplateDTO();
        template3.setId(11111L);
        template3.setName("请假申请表单");
        template3.setTemplateType("APPLICATION");
        template3.setTenantId("tenant_001");
        template3.setVersion(2L);
        template3.setStatus("ACTIVE");
        template3.setCreated(Instant.now());
        template3.setCreatedBy("alice@example.com");
        templates.add(template3);

        return templates;
    }

    private FormTemplateDTO createTestFormTemplate() {
        FormTemplateDTO template = new FormTemplateDTO();
        template.setId(12345L);
        template.setName("订单审批表单");
        template.setTemplateType("APPROVAL");
        template.setTenantId("tenant_001");
        template.setVersion(1L);
        template.setStatus("ACTIVE");
        template.setDescription("用于订单审批的表单模板");
        template.setFormContent("{\"fields\": [{\"type\": \"text\", \"name\": \"orderId\"}]}");
        template.setCreated(Instant.now());
        template.setUpdated(Instant.now());
        template.setCreatedBy("alice@example.com");
        template.setUpdatedBy("alice@example.com");
        return template;
    }
}
