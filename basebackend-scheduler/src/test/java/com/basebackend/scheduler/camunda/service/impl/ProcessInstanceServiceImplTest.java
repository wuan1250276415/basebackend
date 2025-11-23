package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.*;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.engine.migration.MigrationPlanBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.runtime.VariableInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProcessInstanceServiceImpl 单元测试
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class ProcessInstanceServiceImplTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private HistoryService historyService;

    @Mock
    private ProcessInstanceQuery processInstanceQuery;

    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private HistoricProcessInstance historicProcessInstance;

    @Mock
    private VariableInstance variableInstance;

    @Mock
    private MigrationPlan migrationPlan;

    @Mock
    private VariableInstanceQuery variableInstanceQuery;

    @Mock
    private MigrationPlanBuilder migrationPlanBuilder;

    @InjectMocks
    private ProcessInstanceServiceImpl processInstanceService;

    private ProcessInstancePageQuery pageQuery;
    private ProcessInstanceHistoryQuery historyQuery;
    private ProcessInstanceDeleteRequest deleteRequest;
    private ProcessInstanceVariablesRequest variablesRequest;
    private ProcessInstanceMigrationRequest migrationRequest;

    @BeforeEach
    void setUp() {
        // 初始化分页查询参数
        pageQuery = new ProcessInstancePageQuery();
        pageQuery.setCurrent(1);
        pageQuery.setSize(10);
        pageQuery.setProcessDefinitionKey("test-process");
        pageQuery.setBusinessKey("test-key");
        pageQuery.setTenantId("tenant1");
        pageQuery.setSuspended(false);

        // 初始化历史查询参数
        historyQuery = new ProcessInstanceHistoryQuery();
        historyQuery.setCurrent(1);
        historyQuery.setSize(10);
        historyQuery.setProcessDefinitionKey("test-process");
        historyQuery.setFinished(true);

        // 初始化删除请求
        deleteRequest = new ProcessInstanceDeleteRequest();
        deleteRequest.setDeleteReason("测试删除");
        deleteRequest.setSkipCustomListeners(false);
        deleteRequest.setExternallyTerminated(false);
        deleteRequest.setSkipIoMappings(false);

        // 初始化变量请求
        variablesRequest = new ProcessInstanceVariablesRequest();
        Map<String, Object> variables = new HashMap<>();
        variables.put("key1", "value1");
        variables.put("key2", 123);
        variablesRequest.setVariables(variables);
        variablesRequest.setLocal(false);

        // 初始化迁移请求
        migrationRequest = new ProcessInstanceMigrationRequest();
        migrationRequest.setTargetProcessDefinitionId("new-definition-id");

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processDefinitionKey(anyString())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceBusinessKeyLike(anyString())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.tenantIdIn(anyString())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.suspended()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.active()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(anyString())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.orderByProcessInstanceId()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.asc()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.desc()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        when(processInstanceQuery.count()).thenReturn(0L);

        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processDefinitionKey(anyString())).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceBusinessKeyLike(anyString())).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.finished()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.unfinished()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.startedAfter(any(Date.class))).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.startedBefore(any(Date.class))).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.orderByProcessInstanceStartTime()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.asc()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.desc()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        when(historicProcessInstanceQuery.count()).thenReturn(0L);

        when(runtimeService.createVariableInstanceQuery()).thenReturn(variableInstanceQuery);
        when(variableInstanceQuery.processInstanceIdIn(anyString())).thenReturn(variableInstanceQuery);
        when(variableInstanceQuery.variableScopeIdIn(anyString())).thenReturn(variableInstanceQuery);
        when(variableInstanceQuery.variableName(anyString())).thenReturn(variableInstanceQuery);
        when(variableInstanceQuery.list()).thenReturn(Collections.emptyList());
    }

    @Test
    void testPage_Success() {
        // 准备测试数据
        ProcessInstance instance1 = mock(ProcessInstance.class);
        ProcessInstance instance2 = mock(ProcessInstance.class);

        when(instance1.getId()).thenReturn("id1");
        when(instance1.getProcessDefinitionId()).thenReturn("def1");
        when(instance1.getProcessInstanceId()).thenReturn("proc1");
        when(instance1.getBusinessKey()).thenReturn("key1");
        when(instance1.getCaseInstanceId()).thenReturn("case1");
        when(instance1.isSuspended()).thenReturn(false);
        when(instance1.isEnded()).thenReturn(false);
        when(instance1.getTenantId()).thenReturn("tenant1");

        when(instance2.getId()).thenReturn("id2");
        when(instance2.getProcessDefinitionId()).thenReturn("def2");
        when(instance2.getProcessInstanceId()).thenReturn("proc2");
        when(instance2.getBusinessKey()).thenReturn("key2");
        when(instance2.getCaseInstanceId()).thenReturn("case2");
        when(instance2.isSuspended()).thenReturn(true);
        when(instance2.isEnded()).thenReturn(true);
        when(instance2.getTenantId()).thenReturn("tenant2");

        List<ProcessInstance> instances = Arrays.asList(instance1, instance2);
        long total = instances.size();

        // 模拟查询
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.listPage(anyInt(), anyInt())).thenReturn(instances);
        when(processInstanceQuery.count()).thenReturn(total);

        // 执行测试
        PageResult<ProcessInstanceDTO> result = processInstanceService.page(pageQuery);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getRecords().size());
        assertEquals(total, result.getTotal());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());

        // 验证转换结果
        ProcessInstanceDTO dto1 = result.getRecords().get(0);
        assertEquals("id1", dto1.getId());
        assertEquals("proc1", dto1.getProcessInstanceId());
        assertEquals("key1", dto1.getBusinessKey());
    }

    @Test
    void testPage_EmptyResult() {
        // 准备测试数据（空结果）
        List<ProcessInstance> instances = Arrays.asList();
        long total = 0;

        // 模拟查询
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.listPage(anyInt(), anyInt())).thenReturn(instances);
        when(processInstanceQuery.count()).thenReturn(total);

        // 执行测试
        PageResult<ProcessInstanceDTO> result = processInstanceService.page(pageQuery);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0, result.getTotal());
    }

    @Test
    void testPage_Exception() {
        // 模拟异常
        when(runtimeService.createProcessInstanceQuery())
                .thenThrow(new RuntimeException("查询失败"));

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.page(pageQuery);
        });
    }

    @Test
    void testDetail_Success() {
        // 准备测试数据
        String instanceId = "test-id";
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", "value1");

        when(runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult())
                .thenReturn(processInstance);

        when(processInstance.getId()).thenReturn(instanceId);
        when(processInstance.getProcessDefinitionId()).thenReturn("def1");
        when(processInstance.getProcessInstanceId()).thenReturn("proc1");
        when(processInstance.getBusinessKey()).thenReturn("key1");
        when(processInstance.getCaseInstanceId()).thenReturn("case1");
        when(processInstance.isSuspended()).thenReturn(false);
        when(processInstance.isEnded()).thenReturn(false);
        when(processInstance.getTenantId()).thenReturn("tenant1");

        when(runtimeService.getVariables(instanceId)).thenReturn(variables);

        // 执行测试
        ProcessInstanceDetailDTO result = processInstanceService.detail(instanceId, true);

        // 验证结果
        assertNotNull(result);
        assertEquals(instanceId, result.getId());
        assertEquals("proc1", result.getProcessInstanceId());
        assertEquals("key1", result.getBusinessKey());
        assertNotNull(result.getVariables());
        assertEquals("value1", result.getVariables().get("var1"));
    }

    @Test
    void testDetail_NotFound() {
        // 准备测试数据（不存在）
        String instanceId = "non-existent";
        when(runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult())
                .thenReturn(null);

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.detail(instanceId, false);
        });
    }

    @Test
    void testDetail_Exception() {
        // 准备测试数据
        String instanceId = "test-id";
        when(runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult())
                .thenThrow(new RuntimeException("系统异常"));

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.detail(instanceId, false);
        });
    }

    @Test
    void testSuspend_Success() {
        // 准备测试数据
        String instanceId = "test-id";

        // 执行测试
        processInstanceService.suspend(instanceId);

        // 验证结果
        verify(runtimeService).suspendProcessInstanceById(instanceId);
    }

    @Test
    void testSuspend_Exception() {
        // 准备测试数据
        String instanceId = "test-id";
        doThrow(new RuntimeException("挂起失败")).when(runtimeService)
                .suspendProcessInstanceById(instanceId);

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.suspend(instanceId);
        });
    }

    @Test
    void testActivate_Success() {
        // 准备测试数据
        String instanceId = "test-id";

        // 执行测试
        processInstanceService.activate(instanceId);

        // 验证结果
        verify(runtimeService).activateProcessInstanceById(instanceId);
    }

    @Test
    void testActivate_Exception() {
        // 准备测试数据
        String instanceId = "test-id";
        doThrow(new RuntimeException("激活失败")).when(runtimeService)
                .activateProcessInstanceById(instanceId);

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.activate(instanceId);
        });
    }

    @Test
    void testDelete_Success() {
        // 准备测试数据
        String instanceId = "test-id";

        // 执行测试
        processInstanceService.delete(instanceId, deleteRequest);

        // 验证结果
        verify(runtimeService).deleteProcessInstance(
                eq(instanceId),
                eq("测试删除"),
                eq(false),
                eq(false),
                eq(false)
        );
    }

    @Test
    void testDelete_Exception() {
        // 准备测试数据
        String instanceId = "test-id";
        doThrow(new RuntimeException("删除失败")).when(runtimeService)
                .deleteProcessInstance(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean());

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.delete(instanceId, deleteRequest);
        });
    }

    @Test
    void testVariables_Success() {
        // 准备测试数据
        String instanceId = "test-id";
        List<VariableInstance> variables = Arrays.asList(variableInstance);

        when(runtimeService.createVariableInstanceQuery()
                .processInstanceIdIn(instanceId).list()).thenReturn(variables);

        when(variableInstance.getName()).thenReturn("var1");
        when(variableInstance.getValue()).thenReturn("value1");
        when(variableInstance.getTypeName()).thenReturn("String");
        when(variableInstance.getProcessInstanceId()).thenReturn(instanceId);
        when(variableInstance.getExecutionId()).thenReturn("exec1");
        when(variableInstance.getTaskId()).thenReturn(null);

        // 执行测试
        List<ProcessVariableDTO> result = processInstanceService.variables(instanceId, false);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("var1", result.get(0).getName());
        assertEquals("value1", result.get(0).getValue());
    }

    @Test
    void testVariables_Local() {
        // 准备测试数据
        String instanceId = "test-id";
        List<VariableInstance> variables = Arrays.asList(variableInstance);

        when(runtimeService.createVariableInstanceQuery()
                .variableScopeIdIn(instanceId).list()).thenReturn(variables);

        // 执行测试
        List<ProcessVariableDTO> result = processInstanceService.variables(instanceId, true);

        // 验证结果
        assertNotNull(result);
        verify(runtimeService).createVariableInstanceQuery().variableScopeIdIn(instanceId);
    }

    @Test
    void testVariables_Exception() {
        // 准备测试数据
        String instanceId = "test-id";
        when(runtimeService.createVariableInstanceQuery()
                .processInstanceIdIn(instanceId).list())
                .thenThrow(new RuntimeException("查询变量失败"));

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.variables(instanceId, false);
        });
    }

    @Test
    void testVariable_Success() {
        // 准备测试数据
        String instanceId = "test-id";
        String variableName = "var1";
        Object value = "value1";

        when(runtimeService.getVariable(instanceId, variableName)).thenReturn(value);

        when(runtimeService.createVariableInstanceQuery()
                .processInstanceIdIn(instanceId).variableName(variableName).singleResult())
                .thenReturn(variableInstance);

        when(variableInstance.getTypeName()).thenReturn("String");

        // 执行测试
        ProcessVariableDTO result = processInstanceService.variable(instanceId, variableName, false);

        // 验证结果
        assertNotNull(result);
        assertEquals(variableName, result.getName());
        assertEquals(value, result.getValue());
        assertEquals("String", result.getType());
    }

    @Test
    void testVariable_NotFound() {
        // 准备测试数据
        String instanceId = "test-id";
        String variableName = "non-existent";

        when(runtimeService.createVariableInstanceQuery()
                .processInstanceIdIn(instanceId).variableName(variableName).singleResult())
                .thenReturn(null);

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.variable(instanceId, variableName, false);
        });
    }

    @Test
    void testVariable_Local() {
        // 准备测试数据
        String instanceId = "test-id";
        String variableName = "var1";
        Object value = "value1";

        when(runtimeService.getVariableLocal(instanceId, variableName)).thenReturn(value);

        when(runtimeService.createVariableInstanceQuery()
                .processInstanceIdIn(instanceId).variableName(variableName).singleResult())
                .thenReturn(variableInstance);

        when(variableInstance.getTypeName()).thenReturn("String");

        // 执行测试
        ProcessVariableDTO result = processInstanceService.variable(instanceId, variableName, true);

        // 验证结果
        assertNotNull(result);
        verify(runtimeService).getVariableLocal(instanceId, variableName);
    }

    @Test
    void testSetVariables_Success() {
        // 准备测试数据
        String instanceId = "test-id";

        // 执行测试
        processInstanceService.setVariables(instanceId, variablesRequest);

        // 验证结果
        verify(runtimeService).setVariables(eq(instanceId), eq(variablesRequest.getVariables()));
    }

    @Test
    void testSetVariables_Local() {
        // 准备测试数据
        String instanceId = "test-id";
        variablesRequest.setLocal(true);

        // 执行测试
        processInstanceService.setVariables(instanceId, variablesRequest);

        // 验证结果
        verify(runtimeService).setVariablesLocal(eq(instanceId), eq(variablesRequest.getVariables()));
    }

    @Test
    void testSetVariables_Exception() {
        // 准备测试数据
        String instanceId = "test-id";
        doThrow(new RuntimeException("设置变量失败")).when(runtimeService)
                .setVariables(anyString(), anyMap());

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.setVariables(instanceId, variablesRequest);
        });
    }

    @Test
    void testDeleteVariable_Success() {
        // 准备测试数据
        String instanceId = "test-id";
        String variableName = "var1";

        // 执行测试
        processInstanceService.deleteVariable(instanceId, variableName, false);

        // 验证结果
        verify(runtimeService).removeVariable(instanceId, variableName);
    }

    @Test
    void testDeleteVariable_Local() {
        // 准备测试数据
        String instanceId = "test-id";
        String variableName = "var1";

        // 执行测试
        processInstanceService.deleteVariable(instanceId, variableName, true);

        // 验证结果
        verify(runtimeService).removeVariableLocal(instanceId, variableName);
    }

    @Test
    void testDeleteVariable_Exception() {
        // 准备测试数据
        String instanceId = "test-id";
        String variableName = "var1";
        doThrow(new RuntimeException("删除变量失败")).when(runtimeService)
                .removeVariable(anyString(), anyString());

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.deleteVariable(instanceId, variableName, false);
        });
    }

    @Test
    void testMigrate_Success() {
        // 准备测试数据
        String instanceId = "test-id";
        String sourceDefinitionId = "source-def";

        when(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult())
                .thenReturn(processInstance);

        when(processInstance.getProcessDefinitionId()).thenReturn(sourceDefinitionId);

        when(runtimeService.createMigrationPlan(eq(sourceDefinitionId), eq("new-definition-id")))
                .thenReturn(migrationPlanBuilder);
        // Note: migrationPlanExecutionBuilder is a local mock created in setUp

        // 执行测试
        processInstanceService.migrate(instanceId, migrationRequest);

        // 验证结果
        verify(runtimeService).createMigrationPlan(eq(sourceDefinitionId), eq("new-definition-id"));
    }

    @Test
    void testMigrate_InstanceNotFound() {
        // 准备测试数据（实例不存在）
        String instanceId = "non-existent";
        when(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult())
                .thenReturn(null);

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.migrate(instanceId, migrationRequest);
        });
    }

    @Test
    void testMigrate_Exception() {
        // 准备测试数据
        String instanceId = "test-id";
        when(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult())
                .thenReturn(processInstance);

        when(processInstance.getProcessDefinitionId()).thenReturn("source-def");

        doThrow(new RuntimeException("迁移失败")).when(runtimeService)
                .createMigrationPlan(anyString(), anyString());

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.migrate(instanceId, migrationRequest);
        });
    }

    @Test
    void testHistory_Success() {
        // 准备测试数据
        HistoricProcessInstance historic1 = mock(HistoricProcessInstance.class);
        HistoricProcessInstance historic2 = mock(HistoricProcessInstance.class);

        when(historic1.getId()).thenReturn("hist1");
        when(historic1.getProcessDefinitionId()).thenReturn("def1");
        when(historic1.getProcessDefinitionKey()).thenReturn("key1");
        when(historic1.getProcessDefinitionName()).thenReturn("Name1");
        when(historic1.getBusinessKey()).thenReturn("biz1");
        when(historic1.getStartTime()).thenReturn(null);
        when(historic1.getEndTime()).thenReturn(null);
        when(historic1.getDurationInMillis()).thenReturn(1000L);
        when(historic1.getStartUserId()).thenReturn("user1");
        when(historic1.getDeleteReason()).thenReturn(null);
        when(historic1.getState()).thenReturn(null);
        when(historic1.getTenantId()).thenReturn("tenant1");

        when(historic2.getId()).thenReturn("hist2");
        when(historic2.getProcessDefinitionId()).thenReturn("def2");
        when(historic2.getProcessDefinitionKey()).thenReturn("key2");
        when(historic2.getProcessDefinitionName()).thenReturn("Name2");
        when(historic2.getBusinessKey()).thenReturn("biz2");
        when(historic2.getStartTime()).thenReturn(null);
        when(historic2.getEndTime()).thenReturn(null);
        when(historic2.getDurationInMillis()).thenReturn(2000L);
        when(historic2.getStartUserId()).thenReturn("user2");
        when(historic2.getDeleteReason()).thenReturn(null);
        when(historic2.getState()).thenReturn(null);
        when(historic2.getTenantId()).thenReturn("tenant2");

        List<HistoricProcessInstance> instances = Arrays.asList(historic1, historic2);
        long total = instances.size();

        // 模拟查询
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.listPage(anyInt(), anyInt())).thenReturn(instances);
        when(historicProcessInstanceQuery.count()).thenReturn(total);

        // 执行测试
        PageResult<HistoricProcessInstanceDTO> result = processInstanceService.history(historyQuery);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getRecords().size());
        assertEquals(total, result.getTotal());

        // 验证转换结果
        HistoricProcessInstanceDTO dto1 = result.getRecords().get(0);
        assertEquals("hist1", dto1.getId());
        assertEquals("key1", dto1.getProcessDefinitionKey());
    }

    @Test
    void testHistory_Exception() {
        // 模拟异常
        when(historyService.createHistoricProcessInstanceQuery())
                .thenThrow(new RuntimeException("查询失败"));

        // 执行测试并验证异常
        assertThrows(CamundaServiceException.class, () -> {
            processInstanceService.history(historyQuery);
        });
    }

    @Test
    void testApplyQueryFilters_WithProcessDefinitionKey() {
        // 准备测试数据
        ProcessInstancePageQuery query = new ProcessInstancePageQuery();
        query.setProcessDefinitionKey("test-process");

        // 执行测试
        processInstanceService.page(query);

        // 验证结果
        verify(processInstanceQuery).processDefinitionKey("test-process");
    }

    @Test
    void testApplyQueryFilters_WithBusinessKey() {
        // 准备测试数据
        ProcessInstancePageQuery query = new ProcessInstancePageQuery();
        query.setBusinessKey("test-key");

        // 执行测试
        processInstanceService.page(query);

        // 验证结果
        verify(processInstanceQuery).processInstanceBusinessKeyLike("%test-key%");
    }

    @Test
    void testApplyQueryFilters_WithSuspended() {
        // 准备测试数据
        ProcessInstancePageQuery query = new ProcessInstancePageQuery();
        query.setSuspended(true);

        // 执行测试
        processInstanceService.page(query);

        // 验证结果
        verify(processInstanceQuery).suspended();
    }

    @Test
    void testApplyQueryFilters_WithActive() {
        // 准备测试数据
        ProcessInstancePageQuery query = new ProcessInstancePageQuery();
        query.setSuspended(false);

        // 执行测试
        processInstanceService.page(query);

        // 验证结果
        verify(processInstanceQuery).active();
    }
}
