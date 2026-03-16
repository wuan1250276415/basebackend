package com.basebackend.database.tenant.interceptor;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.TenantContextException;
import com.basebackend.database.tenant.context.TenantContext;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TenantInterceptor 测试")
class TenantInterceptorTest {

    private TenantInterceptor tenantInterceptor;
    private MappedStatement mappedStatement;
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getMultiTenancy().setEnabled(true);
        properties.getMultiTenancy().setTenantColumn("tenant_id");
        properties.getMultiTenancy().setExcludedTables(Collections.emptyList());

        tenantInterceptor = new TenantInterceptor(properties);
        mappedStatement = mock(MappedStatement.class);
        configuration = new Configuration();

        TenantContext.setTenantId("tenant_001");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("SELECT 语句会自动注入租户条件")
    void shouldAddTenantFilterForSelect() {
        BoundSql boundSql = new BoundSql(configuration, "SELECT * FROM orders", Collections.emptyList(), null);

        tenantInterceptor.beforeQuery(
                null,
                mappedStatement,
                null,
                RowBounds.DEFAULT,
                mock(ResultHandler.class),
                boundSql
        );

        assertThat(boundSql.getSql()).contains("tenant_id");
        assertThat(boundSql.getSql()).contains("tenant_001");
    }

    @Test
    @DisplayName("SQL 解析失败时 fail-close 阻断执行")
    void shouldFailCloseWhenQuerySqlParsingFails() {
        BoundSql boundSql = new BoundSql(configuration, "SELECT * FROM", Collections.emptyList(), null);

        assertThatThrownBy(() ->
                tenantInterceptor.beforeQuery(
                        null,
                        mappedStatement,
                        null,
                        RowBounds.DEFAULT,
                        mock(ResultHandler.class),
                        boundSql
                )
        ).isInstanceOf(TenantContextException.class)
         .hasMessageContaining("Failed to apply tenant filter");
    }

    @Test
    @DisplayName("UPDATE/DELETE 预处理解析失败时 fail-close 阻断执行")
    void shouldFailCloseWhenBeforePrepareSqlParsingFails() {
        StatementHandler statementHandler = mock(StatementHandler.class);
        BoundSql boundSql = new BoundSql(configuration, "UPDATE", Collections.emptyList(), null);
        when(statementHandler.getBoundSql()).thenReturn(boundSql);

        assertThatThrownBy(() -> tenantInterceptor.beforePrepare(statementHandler, null, null))
                .isInstanceOf(TenantContextException.class)
                .hasMessageContaining("Failed to apply tenant filter");
    }
}
