package com.basebackend.scheduler.camunda.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO 验证测试
 *
 * <p>测试所有关键 DTO 类的验证规则，包括：
 * <ul>
 *   <li>BasePageQuery 分页基类的验证规则</li>
 *   <li>分页 DTO 子类的继承关系</li>
 *   <li>请求 DTO 的字段约束验证</li>
 *   <li>分页参数边界值测试</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DtoValidationTest {

    private Validator validator;

    @BeforeAll
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========== BasePageQuery 分页基类测试 ==========

    @Test
    void testBasePageQuery_ValidDefaultValues() {
        // 测试默认值是否有效
        BasePageQuery query = new BasePageQuery();

        Set<ConstraintViolation<BasePageQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "默认的分页参数应该有效");
        assertEquals(1, query.getCurrent(), "默认当前页码应该是 1");
        assertEquals(10, query.getSize(), "默认每页大小应该是 10");
    }

    @Test
    void testBasePageQuery_ValidCustomValues() {
        // 测试自定义有效值
        BasePageQuery query = new BasePageQuery();
        query.setCurrent(5);
        query.setSize(50);

        Set<ConstraintViolation<BasePageQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "有效的自定义分页参数应该通过验证");
        assertEquals(5, query.getCurrent());
        assertEquals(50, query.getSize());
    }

    @Test
    void testBasePageQuery_InvalidZeroPageNum() {
        // 测试零页码
        BasePageQuery query = new BasePageQuery();
        query.setCurrent(0);
        query.setSize(10);

        Set<ConstraintViolation<BasePageQuery>> violations = validator.validate(query);

        assertFalse(violations.isEmpty(), "零页码应该触发验证错误");
        assertEquals(1, violations.size());
        ConstraintViolation<BasePageQuery> violation = violations.iterator().next();
        assertEquals("current", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("必须大于等于 1"));
    }

    @Test
    void testBasePageQuery_InvalidNegativePageNum() {
        // 测试负数页码
        BasePageQuery query = new BasePageQuery();
        query.setCurrent(-1);
        query.setSize(10);

        Set<ConstraintViolation<BasePageQuery>> violations = validator.validate(query);

        assertFalse(violations.isEmpty(), "负数页码应该触发验证错误");
        assertEquals(1, violations.size());
    }

    @Test
    void testBasePageQuery_InvalidZeroPageSize() {
        // 测试零页大小
        BasePageQuery query = new BasePageQuery();
        query.setCurrent(1);
        query.setSize(0);

        Set<ConstraintViolation<BasePageQuery>> violations = validator.validate(query);

        assertFalse(violations.isEmpty(), "零页大小应该触发验证错误");
        assertEquals(1, violations.size());
        ConstraintViolation<BasePageQuery> violation = violations.iterator().next();
        assertEquals("size", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("必须大于等于 1"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100, 200, 500})
    void testBasePageQuery_ValidPageSizes(int size) {
        // 测试有效的页大小值
        BasePageQuery query = new BasePageQuery();
        query.setCurrent(1);
        query.setSize(size);

        Set<ConstraintViolation<BasePageQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "页大小 " + size + " 应该有效");
    }

    @Test
    void testBasePageQuery_ExceedMaxPageSize() {
        // 测试超过最大页大小
        BasePageQuery query = new BasePageQuery();
        query.setCurrent(1);
        query.setSize(1001);

        Set<ConstraintViolation<BasePageQuery>> violations = validator.validate(query);

        assertFalse(violations.isEmpty(), "超过最大页大小应该触发验证错误");
        assertEquals(1, violations.size());
        ConstraintViolation<BasePageQuery> violation = violations.iterator().next();
        assertEquals("size", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("不能超过"));
    }

    // ========== 分页 DTO 子类继承测试 ==========

    @Test
    void testTaskPageQuery_ExtendsBasePageQuery() {
        // 测试 TaskPageQuery 继承关系
        TaskPageQuery query = new TaskPageQuery();
        query.setCurrent(2);
        query.setSize(20);
        query.setAssignee("alice@example.com");

        Set<ConstraintViolation<TaskPageQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "继承自 BasePageQuery 的有效值应该通过验证");
        assertEquals(2, query.getCurrent());
        assertEquals(20, query.getSize());
        assertEquals("alice@example.com", query.getAssignee());
    }

    @Test
    void testProcessInstancePageQuery_ExtendsBasePageQuery() {
        // 测试 ProcessInstancePageQuery 继承关系
        ProcessInstancePageQuery query = new ProcessInstancePageQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setBusinessKey("ORDER_12345");

        Set<ConstraintViolation<ProcessInstancePageQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "继承自 BasePageQuery 的有效值应该通过验证");
        assertEquals(1, query.getCurrent());
        assertEquals(10, query.getSize());
        assertEquals("ORDER_12345", query.getBusinessKey());
    }

    @Test
    void testProcessDefinitionPageQuery_ExtendsBasePageQuery() {
        // 测试 ProcessDefinitionPageQuery 继承关系
        ProcessDefinitionPageQuery query = new ProcessDefinitionPageQuery();
        query.setCurrent(3);
        query.setSize(30);
        query.setKeyword("approval");

        Set<ConstraintViolation<ProcessDefinitionPageQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "继承自 BasePageQuery 的有效值应该通过验证");
        assertEquals(3, query.getCurrent());
        assertEquals(30, query.getSize());
        assertEquals("approval", query.getKeyword());
    }

    @Test
    void testFormTemplatePageQuery_ExtendsBasePageQuery() {
        // 测试 FormTemplatePageQuery 继承关系
        FormTemplatePageQuery query = new FormTemplatePageQuery();
        query.setCurrent(1);
        query.setSize(15);
        query.setTemplateType("APPROVAL");

        Set<ConstraintViolation<FormTemplatePageQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "继承自 BasePageQuery 的有效值应该通过验证");
        assertEquals(1, query.getCurrent());
        assertEquals(15, query.getSize());
        assertEquals("APPROVAL", query.getTemplateType());
    }

    @Test
    void testHistoricProcessInstancePageQuery_ExtendsBasePageQuery() {
        // 测试 HistoricProcessInstancePageQuery 继承关系
        ProcessInstanceHistoryQuery query = new ProcessInstanceHistoryQuery();
        query.setCurrent(1);
        query.setSize(10);
        query.setProcessDefinitionKey("order_approval");

        Set<ConstraintViolation<ProcessInstanceHistoryQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "继承自 BasePageQuery 的有效值应该通过验证");
        assertEquals(1, query.getCurrent());
        assertEquals(10, query.getSize());
        assertEquals("order_approval", query.getProcessDefinitionKey());
    }

    @Test
    void testSimplePageQuery_ExtendsBasePageQuery() {
        // 测试 SimplePageQuery 继承关系（仅分页字段）
        SimplePageQuery query = new SimplePageQuery();
        query.setCurrent(5);
        query.setSize(25);

        Set<ConstraintViolation<SimplePageQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "SimplePageQuery 的有效分页值应该通过验证");
        assertEquals(5, query.getCurrent());
        assertEquals(25, query.getSize());
    }

    // ========== 任务相关 Request DTO 测试 ==========

    @Test
    void testClaimTaskRequest_ValidData() {
        // 测试有效的认领任务请求
        ClaimTaskRequest request = new ClaimTaskRequest();
        request.setUserId("alice@example.com");

        Set<ConstraintViolation<ClaimTaskRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的认领任务请求应该通过验证");
        assertEquals("alice@example.com", request.getUserId());
    }

    @Test
    void testClaimTaskRequest_EmptyUserId() {
        // 测试空用户ID
        ClaimTaskRequest request = new ClaimTaskRequest();
        request.setUserId("");

        Set<ConstraintViolation<ClaimTaskRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "空用户ID应该触发验证错误");
        assertEquals(1, violations.size());
        ConstraintViolation<ClaimTaskRequest> violation = violations.iterator().next();
        assertEquals("userId", violation.getPropertyPath().toString());
    }

    @Test
    void testCompleteTaskRequest_ValidData() {
        // 测试有效的完成任务请求
        CompleteTaskRequest request = new CompleteTaskRequest();
        request.setComment("任务已完成");
        request.setApproved(true);

        Set<ConstraintViolation<CompleteTaskRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的完成任务请求应该通过验证");
        assertEquals("任务已完成", request.getComment());
        assertTrue(request.getApproved());
    }

    @Test
    void testDelegateTaskRequest_ValidData() {
        // 测试有效的委托任务请求
        DelegateTaskRequest request = new DelegateTaskRequest();
        request.setDelegateTo("bob@example.com");
        request.setReason("工作转移");

        Set<ConstraintViolation<DelegateTaskRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的委托任务请求应该通过验证");
        assertEquals("bob@example.com", request.getDelegateTo());
        assertEquals("工作转移", request.getReason());
    }

    @Test
    void testDelegateTaskRequest_EmptyDelegateTo() {
        // 测试空委托对象
        DelegateTaskRequest request = new DelegateTaskRequest();
        request.setDelegateTo("");
        request.setReason("工作转移");

        Set<ConstraintViolation<DelegateTaskRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "空委托对象应该触发验证错误");
        assertEquals(1, violations.size());
    }

    @Test
    void testCommentRequest_ValidData() {
        // 测试有效的评论请求
        CommentRequest request = new CommentRequest();
        request.setMessage("任务已完成审批");
        request.setUserId("alice@example.com");

        Set<ConstraintViolation<CommentRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的评论请求应该通过验证");
        assertEquals("任务已完成审批", request.getMessage());
        assertEquals("alice@example.com", request.getUserId());
    }

    @Test
    void testVariableUpsertRequest_ValidData() {
        // 测试有效的变量设置请求
        VariableUpsertRequest request = new VariableUpsertRequest();
        request.setVariableName("amount");
        request.setVariableValue(1000.0);
        request.setLocal(false);

        Set<ConstraintViolation<VariableUpsertRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的变量设置请求应该通过验证");
        assertEquals("amount", request.getVariableName());
        assertEquals(1000.0, request.getVariableValue());
        assertFalse(request.getLocal());
    }

    @Test
    void testVariableUpsertRequest_EmptyVariableName() {
        // 测试空变量名
        VariableUpsertRequest request = new VariableUpsertRequest();
        request.setVariableName("");
        request.setVariableValue(1000.0);

        Set<ConstraintViolation<VariableUpsertRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "空变量名应该触发验证错误");
        assertEquals(1, violations.size());
    }

    // ========== 流程定义相关 Request DTO 测试 ==========

    @Test
    void testProcessDefinitionDeployRequest_ValidData() {
        // 测试有效的流程部署请求
        ProcessDefinitionDeployRequest request = new ProcessDefinitionDeployRequest();
        request.setBpmnXmlContent("<bpmn>test</bpmn>");
        request.setProcessKey("test_process");
        request.setProcessName("测试流程");

        Set<ConstraintViolation<ProcessDefinitionDeployRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的流程部署请求应该通过验证");
        assertEquals("test_process", request.getProcessKey());
        assertEquals("测试流程", request.getProcessName());
    }

    @Test
    void testProcessDefinitionStartRequest_ValidData() {
        // 测试有效的流程启动请求
        ProcessDefinitionStartRequest request = new ProcessDefinitionStartRequest();
        request.setDefinitionKey("order_approval");
        request.setBusinessKey("ORDER_12345");
        request.setTenantId("tenant_001");

        Set<ConstraintViolation<ProcessDefinitionStartRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的流程启动请求应该通过验证");
        assertEquals("order_approval", request.getDefinitionKey());
        assertEquals("ORDER_12345", request.getBusinessKey());
    }

    @Test
    void testProcessDefinitionStateRequest_ValidData() {
        // 测试有效的流程状态变更请求
        ProcessDefinitionStateRequest request = new ProcessDefinitionStateRequest();
        request.setIncludeProcessInstances(true);

        Set<ConstraintViolation<ProcessDefinitionStateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的流程状态变更请求应该通过验证");
        assertTrue(request.getIncludeProcessInstances());
    }

    // ========== 流程实例相关 Request DTO 测试 ==========

    @Test
    void testProcessInstanceDeleteRequest_ValidData() {
        // 测试有效的流程实例删除请求
        ProcessInstanceDeleteRequest request = new ProcessInstanceDeleteRequest();
        request.setDeleteReason("业务取消");
        request.setSkipCustomListeners(true);
        request.setSkipIoMappings(true);
        request.setExternallyTerminated(true);

        Set<ConstraintViolation<ProcessInstanceDeleteRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的流程实例删除请求应该通过验证");
        assertEquals("业务取消", request.getDeleteReason());
        assertTrue(request.getSkipCustomListeners());
        assertTrue(request.getSkipIoMappings());
        assertTrue(request.getExternallyTerminated());
    }

    @Test
    void testProcessInstanceVariablesRequest_ValidData() {
        // 测试有效的流程变量设置请求
        ProcessInstanceVariablesRequest request = new ProcessInstanceVariablesRequest();
        request.setLocal(false);
        // variables 字段可能为 null 或空 Map

        Set<ConstraintViolation<ProcessInstanceVariablesRequest>> violations = validator.validate(request);

        // variables 字段可以为 null 或空，所以应该通过验证
        assertTrue(violations.isEmpty(), "有效的流程变量设置请求应该通过验证");
        assertFalse(request.getLocal());
    }

    @Test
    void testProcessInstanceMigrationRequest_ValidData() {
        // 测试有效的流程实例迁移请求
        ProcessInstanceMigrationRequest request = new ProcessInstanceMigrationRequest();
        request.setTargetProcessDefinitionId("definition_67890");
        request.setSkipCustomListeners(true);
        request.setSkipIoMappings(false);

        Set<ConstraintViolation<ProcessInstanceMigrationRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的流程实例迁移请求应该通过验证");
        assertEquals("definition_67890", request.getTargetProcessDefinitionId());
        assertTrue(request.getSkipCustomListeners());
        assertFalse(request.getSkipIoMappings());
    }

    @Test
    void testProcessInstanceMigrationRequest_EmptyTargetDefinition() {
        // 测试空目标定义ID
        ProcessInstanceMigrationRequest request = new ProcessInstanceMigrationRequest();
        request.setTargetProcessDefinitionId("");
        request.setSkipCustomListeners(true);

        Set<ConstraintViolation<ProcessInstanceMigrationRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "空目标定义ID应该触发验证错误");
        assertEquals(1, violations.size());
    }

    // ========== 表单模板相关 Request DTO 测试 ==========

    @Test
    void testFormTemplateCreateRequest_ValidData() {
        // 测试有效的表单模板创建请求
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("订单审批表单");
        request.setTemplateType("APPROVAL");
        request.setTenantId("tenant_001");
        request.setDescription("测试表单模板");
        request.setFormContent("{\"fields\": []}");

        Set<ConstraintViolation<FormTemplateCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的表单模板创建请求应该通过验证");
        assertEquals("订单审批表单", request.getName());
        assertEquals("APPROVAL", request.getTemplateType());
    }

    @Test
    void testFormTemplateCreateRequest_EmptyName() {
        // 测试空名称
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("");
        request.setTemplateType("APPROVAL");
        request.setTenantId("tenant_001");

        Set<ConstraintViolation<FormTemplateCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "空名称应该触发验证错误");
        assertEquals(1, violations.size());
    }

    @Test
    void testFormTemplateUpdateRequest_ValidData() {
        // 测试有效的表单模板更新请求
        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName("更新后的表单模板");
        request.setDescription("更新描述");
        request.setFormContent("{\"fields\": [{\"type\": \"text\"}]}");

        Set<ConstraintViolation<FormTemplateUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "有效的表单模板更新请求应该通过验证");
        assertEquals("更新后的表单模板", request.getName());
        assertEquals("更新描述", request.getDescription());
    }

    // ========== 统计查询 DTO 测试 ==========

    @Test
    void testStatisticsQuery_ValidData() {
        // 测试有效的统计查询请求
        StatisticsQuery query = new StatisticsQuery();
        query.setTenantId("tenant_001");
        // 时间字段可选

        Set<ConstraintViolation<StatisticsQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "有效的统计查询请求应该通过验证");
        assertEquals("tenant_001", query.getTenantId());
    }

    @Test
    void testStatisticsQuery_AllFieldsNull() {
        // 测试所有字段都为 null（可选字段）
        StatisticsQuery query = new StatisticsQuery();
        // 所有字段都是可选的

        Set<ConstraintViolation<StatisticsQuery>> violations = validator.validate(query);

        assertTrue(violations.isEmpty(), "所有字段为 null 的统计查询应该通过验证");
    }
}
