package com.basebackend.scheduler.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 发送邮件委托实现测试
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
class SendEmailDelegateTest {

    @Mock
    private DelegateExecution execution;

    private JavaDelegate delegate;

    @Mock
    private  JavaMailSender mailSender;
    @Mock
    private  String fromAddress;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new SendEmailDelegate(mailSender,fromAddress);
    }

    @Test
    void testExecuteWithValidData() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("toEmail", "test@example.com");
        variables.put("subject", "Test Subject");
        variables.put("templateId", "welcome");
        variables.put("templateVars", "{\"name\":\"John\"}");

        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).getVariables();
        assertTrue(variables.containsKey("emailSent"));
        assertEquals("success", variables.get("emailSent"));
    }

    @Test
    void testExecuteWithMissingToEmail() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("subject", "Test Subject");
        when(execution.getVariables()).thenReturn(variables);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> delegate.execute(execution));
    }

    @Test
    void testExecuteWithEmptyToEmail() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("toEmail", "");
        variables.put("subject", "Test Subject");
        when(execution.getVariables()).thenReturn(variables);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> delegate.execute(execution));
    }

    @Test
    void testExecuteWithMissingSubject() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("toEmail", "test@example.com");
        when(execution.getVariables()).thenReturn(variables);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> delegate.execute(execution));
    }

    @Test
    void testExecuteWithCustomTemplate() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("toEmail", "test@example.com");
        variables.put("subject", "Custom Subject");
        variables.put("templateId", "custom");
        variables.put("templateVars", "{\"key\":\"value\"}");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("emailStatus", "sent");
        verify(execution, times(1)).setVariable("emailSentAt", any());
    }

    @Test
    void testExecuteWithMultipleRecipients() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("toEmail", "test1@example.com,test2@example.com");
        variables.put("subject", "Test Subject");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        assertEquals("success", variables.get("emailSent"));
    }

    @Test
    void testExecuteWithTemplateVariableReplacement() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("toEmail", "test@example.com");
        variables.put("subject", "Welcome {{name}}");
        variables.put("templateId", "welcome");
        variables.put("templateVars", "{\"name\":\"John Doe\"}");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("emailSubject", "Welcome John Doe");
    }

    @Test
    void testExecuteWithInvalidTemplateVars() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("toEmail", "test@example.com");
        variables.put("subject", "Test Subject");
        variables.put("templateId", "welcome");
        variables.put("templateVars", "invalid json");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert - Should still succeed with default values
        assertEquals("success", variables.get("emailSent"));
    }
}
