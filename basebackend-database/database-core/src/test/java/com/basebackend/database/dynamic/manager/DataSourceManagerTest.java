package com.basebackend.database.dynamic.manager;

import com.basebackend.database.dynamic.DynamicDataSource;
import com.basebackend.database.dynamic.context.DataSourceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DataSourceManager 数据源管理测试")
class DataSourceManagerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private DataSource failingDataSource;

    @Mock
    private Connection connection;

    private DataSourceManager dataSourceManager;
    private DynamicDataSource dynamicDataSource;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setPrimaryDataSourceKey("master");
        dataSourceManager = new DataSourceManager(dynamicDataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        DataSourceContextHolder.clear();
        mocks.close();
    }

    @Test
    @DisplayName("连接可用时应返回 true")
    void shouldReturnTrueWhenConnectionIsAvailable() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);
        dataSourceManager.registerDataSource("tenant-a", dataSource);

        // When
        boolean result = dataSourceManager.testDataSourceConnection("tenant-a");

        // Then
        assertThat(result).isTrue();
        verify(dataSource, atLeastOnce()).getConnection();
    }

    @Test
    @DisplayName("连接失败时应返回 false")
    void shouldReturnFalseWhenConnectionIsUnavailable() throws SQLException {
        // Given
        when(failingDataSource.getConnection()).thenThrow(new SQLException("connection failed"));
        dataSourceManager.registerDataSource("tenant-b", failingDataSource);

        // When
        boolean result = dataSourceManager.testDataSourceConnection("tenant-b");

        // Then
        assertThat(result).isFalse();
        verify(failingDataSource, atLeastOnce()).getConnection();
    }

    @Test
    @DisplayName("数据源不存在时应返回 false")
    void shouldReturnFalseWhenDataSourceDoesNotExist() {
        boolean result = dataSourceManager.testDataSourceConnection("not-exists");
        assertThat(result).isFalse();
    }
}
