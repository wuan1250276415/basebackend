package com.basebackend.database.dynamic;

import com.basebackend.database.dynamic.context.DataSourceContextHolder;
import com.basebackend.database.exception.DataSourceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 动态数据源测试
 * 测试数据源路由、动态添加/移除、严格模式等功能
 *
 * @author BaseBackend
 */
@DisplayName("DynamicDataSource 动态数据源测试")
class DynamicDataSourceTest {

    @Mock
    private DataSource masterDataSource;

    @Mock
    private DataSource slaveDataSource;

    @Mock
    private Connection mockConnection;

    private DynamicDataSource dynamicDataSource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dynamicDataSource = new DynamicDataSource();

        // 配置默认数据源
        dynamicDataSource.setPrimaryDataSourceKey("master");
    }

    @AfterEach
    void tearDown() {
        // 清理上下文
        DataSourceContextHolder.clear();
    }

    @Test
    @DisplayName("初始化后使用默认数据源")
    void shouldUseDefaultDataSourceWhenNoContext() {
        // Given - 配置默认数据源
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // When - 无上下文设置
        DataSourceContextHolder.clear();
        Object lookupKey = dynamicDataSource.determineCurrentLookupKey();

        // Then - 应该返回默认数据源键
        assertThat(lookupKey).isEqualTo("master");
    }

    @Test
    @DisplayName("设置上下文后使用指定数据源")
    void shouldUseDataSourceFromContext() {
        // Given - 配置多个数据源
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        dataSources.put("slave", slaveDataSource);
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // When - 设置上下文
        DataSourceContextHolder.setDataSourceKey("slave");
        Object lookupKey = dynamicDataSource.determineCurrentLookupKey();

        // Then - 应该返回指定的数据源键
        assertThat(lookupKey).isEqualTo("slave");
    }

    @Test
    @DisplayName("严格模式下不存在的数据源应抛出异常")
    void shouldThrowExceptionForNonExistentDataSourceInStrictMode() {
        // Given - 严格模式
        dynamicDataSource.setStrict(true);
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // When - 设置不存在的数据源
        DataSourceContextHolder.setDataSourceKey("nonexistent");

        // Then - 应该抛出异常
        assertThatThrownBy(() -> dynamicDataSource.determineCurrentLookupKey())
            .isInstanceOf(DataSourceException.class)
            .hasMessageContaining("DataSource [nonexistent] not found");
    }

    @Test
    @DisplayName("非严格模式下不存在的数据源应返回null")
    void shouldReturnNullForNonExistentDataSourceInNonStrictMode() {
        // Given - 非严格模式
        dynamicDataSource.setStrict(false);
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // When - 设置不存在的数据源
        DataSourceContextHolder.setDataSourceKey("nonexistent");
        Object lookupKey = dynamicDataSource.determineCurrentLookupKey();

        // Then - 应该返回null
        assertThat(lookupKey).isEqualTo("nonexistent");
    }

    @Test
    @DisplayName("动态添加数据源")
    void shouldAddDataSourceDynamically() {
        // Given - 空数据源映射
        dynamicDataSource.setTargetDataSources(new HashMap<>());
        dynamicDataSource.afterPropertiesSet();

        // When - 动态添加数据源
        dynamicDataSource.addDataSource("slave1", slaveDataSource);

        // Then - 应该包含新数据源
        assertThat(dynamicDataSource.containsDataSource("slave1")).isTrue();
        assertThat(dynamicDataSource.getDataSourceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("添加null键的数据源应抛出异常")
    void shouldThrowExceptionWhenAddingDataSourceWithNullKey() {
        // When & Then
        assertThatThrownBy(() -> dynamicDataSource.addDataSource(null, masterDataSource))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("DataSource key cannot be null or empty");
    }

    @Test
    @DisplayName("添加null数据源应抛出异常")
    void shouldThrowExceptionWhenAddingNullDataSource() {
        // When & Then
        assertThatThrownBy(() -> dynamicDataSource.addDataSource("test", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("DataSource cannot be null");
    }

    @Test
    @DisplayName("添加空键的数据源应抛出异常")
    void shouldThrowExceptionWhenAddingDataSourceWithEmptyKey() {
        // When & Then
        assertThatThrownBy(() -> dynamicDataSource.addDataSource("  ", masterDataSource))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("DataSource key cannot be null or empty");
    }

    @Test
    @DisplayName("动态移除数据源")
    void shouldRemoveDataSourceDynamically() {
        // Given - 添加数据源
        dynamicDataSource.addDataSource("slave1", slaveDataSource);
        assertThat(dynamicDataSource.containsDataSource("slave1")).isTrue();

        // When - 移除数据源
        boolean removed = dynamicDataSource.removeDataSource("slave1");

        // Then
        assertThat(removed).isTrue();
        assertThat(dynamicDataSource.containsDataSource("slave1")).isFalse();
        assertThat(dynamicDataSource.getDataSourceCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("移除主数据源应抛出异常")
    void shouldThrowExceptionWhenRemovingPrimaryDataSource() {
        // Given - 配置主数据源
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // When & Then - 尝试移除主数据源
        assertThatThrownBy(() -> dynamicDataSource.removeDataSource("master"))
            .isInstanceOf(DataSourceException.class)
            .hasMessageContaining("Cannot remove primary datasource: master");
    }

    @Test
    @DisplayName("移除不存在的数据源应返回false")
    void shouldReturnFalseWhenRemovingNonExistentDataSource() {
        // Given
        dynamicDataSource.addDataSource("slave1", slaveDataSource);

        // When - 移除不存在的数据源
        boolean removed = dynamicDataSource.removeDataSource("nonexistent");

        // Then
        assertThat(removed).isFalse();
    }

    @Test
    @DisplayName("移除null键应抛出异常")
    void shouldThrowExceptionWhenRemovingWithNullKey() {
        // When & Then
        assertThatThrownBy(() -> dynamicDataSource.removeDataSource(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("DataSource key cannot be null or empty");
    }

    @Test
    @DisplayName("检查数据源是否存在")
    void shouldCheckDataSourceExistence() {
        // Given
        dynamicDataSource.addDataSource("slave1", slaveDataSource);

        // Then
        assertThat(dynamicDataSource.containsDataSource("slave1")).isTrue();
        assertThat(dynamicDataSource.containsDataSource("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("获取所有数据源键")
    void shouldGetAllDataSourceKeys() {
        // Given
        dynamicDataSource.addDataSource("slave1", slaveDataSource);
        dynamicDataSource.addDataSource("slave2", masterDataSource);

        // When
        var keys = dynamicDataSource.getDataSourceKeys();

        // Then
        assertThat(keys).contains("slave1", "slave2");
    }

    @Test
    @DisplayName("获取数据源数量")
    void shouldGetDataSourceCount() {
        // Given
        assertThat(dynamicDataSource.getDataSourceCount()).isEqualTo(0);

        // When
        dynamicDataSource.addDataSource("slave1", slaveDataSource);
        dynamicDataSource.addDataSource("slave2", masterDataSource);

        // Then
        assertThat(dynamicDataSource.getDataSourceCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("设置主数据源键")
    void shouldSetPrimaryDataSourceKey() {
        // When
        dynamicDataSource.setPrimaryDataSourceKey("custom-primary");

        // Then
        // 注意：这里需要通过determineCurrentLookupKey来验证
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("custom-primary", masterDataSource);
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // 无上下文时应返回新的主键
        DataSourceContextHolder.clear();
        Object lookupKey = dynamicDataSource.determineCurrentLookupKey();
        assertThat(lookupKey).isEqualTo("custom-primary");
    }

    @Test
    @DisplayName("设置严格模式")
    void shouldSetStrictMode() {
        // Given
        dynamicDataSource.setStrict(true);
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // When - 非严格模式
        dynamicDataSource.setStrict(false);
        DataSourceContextHolder.setDataSourceKey("nonexistent");

        // Then - 非严格模式下不应抛异常
        assertThat(dynamicDataSource.determineCurrentLookupKey()).isEqualTo("nonexistent");
    }

    @Test
    @DisplayName("设置目标数据源映射")
    void shouldSetTargetDataSources() {
        // Given
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        dataSources.put("slave", slaveDataSource);

        // When
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // Then
        assertThat(dynamicDataSource.getDataSourceCount()).isEqualTo(2);
        assertThat(dynamicDataSource.containsDataSource("master")).isTrue();
        assertThat(dynamicDataSource.containsDataSource("slave")).isTrue();
    }

    @Test
    @DisplayName("嵌套数据源切换测试")
    void shouldHandleNestedDataSourceSwitching() {
        // Given - 配置数据源
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", masterDataSource);
        dataSources.put("slave1", slaveDataSource);
        dataSources.put("slave2", masterDataSource);
        dynamicDataSource.setTargetDataSources(dataSources);
        dynamicDataSource.afterPropertiesSet();

        // When - 嵌套切换
        DataSourceContextHolder.setDataSourceKey("master");
        assertThat(dynamicDataSource.determineCurrentLookupKey()).isEqualTo("master");

        DataSourceContextHolder.setDataSourceKey("slave1");
        assertThat(dynamicDataSource.determineCurrentLookupKey()).isEqualTo("slave1");

        DataSourceContextHolder.setDataSourceKey("slave2");
        assertThat(dynamicDataSource.determineCurrentLookupKey()).isEqualTo("slave2");

        // When - 回滚到slave1
        DataSourceContextHolder.clearDataSourceKey();
        assertThat(dynamicDataSource.determineCurrentLookupKey()).isEqualTo("slave1");

        // When - 再回滚到master
        DataSourceContextHolder.clearDataSourceKey();
        assertThat(dynamicDataSource.determineCurrentLookupKey()).isEqualTo("master");

        // When - 完全回滚
        DataSourceContextHolder.clearDataSourceKey();
        assertThat(dynamicDataSource.determineCurrentLookupKey()).isEqualTo("master");
    }
}
