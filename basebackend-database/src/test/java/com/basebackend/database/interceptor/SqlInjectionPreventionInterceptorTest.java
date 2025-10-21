package com.basebackend.database.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqlInjectionPreventionInterceptorTest {

    private final SqlInjectionPreventionInterceptor interceptor = new SqlInjectionPreventionInterceptor();

    @Test
    void shouldBlockDangerousSql() {
        StatementHandler handler = mock(StatementHandler.class);
        BoundSql boundSql = mock(BoundSql.class);
        when(handler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn("SELECT * FROM users; DROP TABLE users;");

        assertThatThrownBy(() -> interceptor.beforePrepare(handler, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dangerous");
    }

    @Test
    void shouldAllowSafeSql() {
        StatementHandler handler = mock(StatementHandler.class);
        BoundSql boundSql = mock(BoundSql.class);
        when(handler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn("SELECT * FROM users WHERE id = ?");

        assertThatCode(() -> interceptor.beforePrepare(handler, null, null))
                .doesNotThrowAnyException();
    }
}
