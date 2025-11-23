package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.dto.PageResult;
import com.basebackend.scheduler.camunda.dto.*;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentQuery;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MicroserviceCallDelegateTest {

    @Mock
    private RepositoryService repositoryService;
    @Mock
    private ProcessDefinitionQuery query;
    @Mock
    private ProcessDefinition processDefinition;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private DeploymentQuery deploymentQuery;
    @Mock
    private Deployment deployment;

    private ProcessDefinitionServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ProcessDefinitionServiceImpl(repositoryService,runtimeService);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(repositoryService.createDeploymentQuery()).thenReturn(deploymentQuery);

        when(query.processDefinitionId(anyString())).thenReturn(query);
        when(query.processDefinitionKeyLike(anyString())).thenReturn(query);
        when(query.processDefinitionNameLike(anyString())).thenReturn(query);
        when(query.tenantIdIn(anyString())).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.suspended()).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.orderByProcessDefinitionKey()).thenReturn(query);
        when(query.orderByProcessDefinitionVersion()).thenReturn(query);
        when(query.asc()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.listPage(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        when(query.count()).thenReturn(0L);

        when(deploymentQuery.deploymentId(anyString())).thenReturn(deploymentQuery);
        when(deploymentQuery.singleResult()).thenReturn(deployment);
    }

    @Test
    void testPageWithValidQuery() {
        ProcessDefinitionPageQuery pageQuery = new ProcessDefinitionPageQuery();
        pageQuery.setCurrent(1);
        pageQuery.setSize(10);

        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.count()).thenReturn(25L);
        when(query.listPage(anyInt(), anyInt())).thenReturn(Arrays.asList(processDefinition));

        PageResult<ProcessDefinitionDTO> result = service.page(pageQuery);

        assertNotNull(result);
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(25, result.getTotal());
    }

    @Test
    void testPageWithEmptyResult() {
        ProcessDefinitionPageQuery pageQuery = new ProcessDefinitionPageQuery();
        pageQuery.setCurrent(1);
        pageQuery.setSize(10);

        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.listPage(anyInt(), anyInt())).thenReturn(Arrays.asList());

        PageResult<ProcessDefinitionDTO> result = service.page(pageQuery);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getRecords().size());
    }

    @Test
    void testDetailWithValidId() {
        String definitionId = "order_approval:1:12345";

        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionId(definitionId)).thenReturn(query);
        when(query.singleResult()).thenReturn(processDefinition);

        ProcessDefinitionDetailDTO result = service.detail(definitionId);

        assertNotNull(result);
        verify(query, times(1)).processDefinitionId(definitionId);
    }

    @Test
    void testDetailWithInvalidId() {
        String definitionId = "invalid_id";

        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionId(definitionId)).thenReturn(query);
        when(query.singleResult()).thenReturn(null);

        assertThrows(CamundaServiceException.class, () -> service.detail(definitionId));
    }
}
