package com.basebackend.scheduler.camunda.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MicroserviceCallDelegateTest {

    @Mock
    private DelegateExecution execution;

    private JavaDelegate delegate;

    @Mock
    private  RestTemplate restTemplate;
    @Mock
    private  ObjectMapper objectMapper;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new MicroserviceCallDelegate(restTemplate,objectMapper);
    }

    @Test
    void testExecuteWithValidApiCall() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceUrl", "https://api.example.com/data");
        variables.put("httpMethod", "GET");
        when(execution.getVariables()).thenReturn(variables);

        delegate.execute(execution);

        verify(execution, times(1)).setVariable("serviceCallStatus", "success");
    }

    @Test
    void testExecuteWithPostMethod() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceUrl", "https://api.example.com/data");
        variables.put("httpMethod", "POST");
        when(execution.getVariables()).thenReturn(variables);

        delegate.execute(execution);

        verify(execution, times(1)).setVariable("responseCode", 200);
    }

    @Test
    void testExecuteWithTimeout() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceUrl", "https://slow-api.com/data");
        variables.put("timeout", 1000);
        when(execution.getVariables()).thenReturn(variables);

        delegate.execute(execution);

        verify(execution, times(1)).setVariable("serviceCallStatus", "timeout");
    }

    @Test
    void testExecuteWithRetry() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceUrl", "https://unreliable-api.com/data");
        variables.put("retryCount", 3);
        when(execution.getVariables()).thenReturn(variables);

        delegate.execute(execution);

        verify(execution, times(1)).setVariable("retryAttempts", 3);
    }

    @Test
    void testExecuteWithAuthentication() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceUrl", "https://secure-api.com/data");
        variables.put("authType", "Bearer");
        variables.put("authToken", "secret-token");
        when(execution.getVariables()).thenReturn(variables);

        delegate.execute(execution);

        verify(execution, times(1)).setVariable("authenticated", true);
    }
}
