package com.basebackend.scheduler.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderApprovalDelegateTest {

    @Mock
    private DelegateExecution execution;

    private JavaDelegate delegate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new OrderApprovalDelegate();
    }

    @Test
    void testExecuteWithApprovedStatus() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", "ORDER-2025-001");
        variables.put("approvalStatus", "approved");
        variables.put("approvalAmount", 1000.0);
        when(execution.getVariables()).thenReturn(variables);

        delegate.execute(execution);

        verify(execution, times(1)).setVariable("orderStatus", "APPROVED");
    }

    @Test
    void testExecuteWithRejectedStatus() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", "ORDER-2025-002");
        variables.put("approvalStatus", "rejected");
        when(execution.getVariables()).thenReturn(variables);

        delegate.execute(execution);

        verify(execution, times(1)).setVariable("orderStatus", "REJECTED");
    }

    @Test
    void testExecuteWithInvalidStatus() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", "ORDER-2025-003");
        variables.put("approvalStatus", "invalid");
        when(execution.getVariables()).thenReturn(variables);

        assertThrows(RuntimeException.class, () -> delegate.execute(execution));
    }
}
