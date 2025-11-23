package com.basebackend.scheduler.camunda.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * 数据同步委托实现测试
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
class DataSyncDelegateTest {

    @Mock
    private DelegateExecution execution;

    private JavaDelegate delegate;

    @Mock
    private  ObjectMapper objectMapper;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new DataSyncDelegate(objectMapper);
    }

    @Test
    void testExecuteWithDatabaseSync() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "database");
        variables.put("sourceConnection", "jdbc:mysql://localhost:3306/source");
        variables.put("targetConnection", "jdbc:mysql://localhost:3306/target");
        variables.put("tableName", "users");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("syncStatus", "success");
        verify(execution, times(1)).setVariable("recordsSynced", 0L);
    }

    @Test
    void testExecuteWithApiSync() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "api");
        variables.put("sourceApiUrl", "https://api.source.com/data");
        variables.put("targetApiUrl", "https://api.target.com/data");
        variables.put("batchSize", 100);
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("syncStatus", "success");
    }

    @Test
    void testExecuteWithFileSync() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "file");
        variables.put("sourceFile", "/data/source.csv");
        variables.put("targetFile", "/data/target.csv");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("syncStatus", "success");
    }

    @Test
    void testExecuteWithInvalidSyncType() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "invalid");
        when(execution.getVariables()).thenReturn(variables);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> delegate.execute(execution));
    }

    @Test
    void testExecuteWithMissingSyncType() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        when(execution.getVariables()).thenReturn(variables);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> delegate.execute(execution));
    }

    @Test
    void testExecuteWithDatabaseConnectionFailure() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "database");
        variables.put("sourceConnection", "invalid_connection");
        variables.put("targetConnection", "jdbc:mysql://localhost:3306/target");
        variables.put("tableName", "users");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("syncStatus", "failed");
        verify(execution, times(1)).setVariable("errorMessage", anyString());
    }

    @Test
    void testExecuteWithApiError() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "api");
        variables.put("sourceApiUrl", "https://invalid-api.com/data");
        variables.put("targetApiUrl", "https://api.target.com/data");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("syncStatus", "failed");
    }

    @Test
    void testExecuteWithPartialSuccess() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "database");
        variables.put("sourceConnection", "jdbc:mysql://localhost:3306/source");
        variables.put("targetConnection", "jdbc:mysql://localhost:3306/target");
        variables.put("tableName", "users");
        variables.put("partialSync", true);
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("syncStatus", "partial_success");
    }

    @Test
    void testExecuteWithIncrementalSync() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "database");
        variables.put("sourceConnection", "jdbc:mysql://localhost:3306/source");
        variables.put("targetConnection", "jdbc:mysql://localhost:3306/target");
        variables.put("tableName", "users");
        variables.put("lastSyncTime", "2025-01-01T00:00:00Z");
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("syncStatus", "success");
        verify(execution, times(1)).setVariable("incrementalSync", true);
    }

    @Test
    void testExecuteWithDataValidation() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "database");
        variables.put("sourceConnection", "jdbc:mysql://localhost:3306/source");
        variables.put("targetConnection", "jdbc:mysql://localhost:3306/target");
        variables.put("tableName", "users");
        variables.put("validateData", true);
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("validationResult", "passed");
    }

    @Test
    void testExecuteWithBatchProcessing() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "api");
        variables.put("sourceApiUrl", "https://api.source.com/data");
        variables.put("targetApiUrl", "https://api.target.com/data");
        variables.put("batchSize", 500);
        variables.put("totalRecords", 10000);
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("totalBatches", 20);
    }

    @Test
    void testExecuteWithRetryConfiguration() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("syncType", "api");
        variables.put("sourceApiUrl", "https://api.source.com/data");
        variables.put("targetApiUrl", "https://api.target.com/data");
        variables.put("retryCount", 3);
        variables.put("retryDelay", 5000);
        when(execution.getVariables()).thenReturn(variables);

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution, times(1)).setVariable("syncStatus", "success");
    }
}
