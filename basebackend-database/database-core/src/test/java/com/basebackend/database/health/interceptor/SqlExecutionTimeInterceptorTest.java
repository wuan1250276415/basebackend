package com.basebackend.database.health.interceptor;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.logger.SlowQueryLogger;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqlExecutionTimeInterceptor 参数安全测试")
class SqlExecutionTimeInterceptorTest {

    @Mock
    private SlowQueryLogger slowQueryLogger;

    @Mock
    private MappedStatement mappedStatement;

    private SqlExecutionTimeInterceptor interceptor;
    private Executor executor;

    @BeforeEach
    void setUp() {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getHealth().setEnabled(true);
        interceptor = new SqlExecutionTimeInterceptor(properties, slowQueryLogger);
        executor = org.mockito.Mockito.mock(Executor.class);
    }

    @Test
    @DisplayName("慢查询参数仅记录元信息，不输出具体值")
    void shouldLogOnlyParameterMetadataWithoutSensitiveValues() throws Throwable {
        SensitiveParameter parameter = new SensitiveParameter("admin", "Secr3t-Pass");
        Configuration configuration = new Configuration();
        List<ParameterMapping> mappings = List.of(
                new ParameterMapping.Builder(configuration, "username", String.class).build(),
                new ParameterMapping.Builder(configuration, "password", String.class).build()
        );
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        BoundSql boundSql = new BoundSql(configuration, sql, mappings, parameter);

        when(mappedStatement.getBoundSql(parameter)).thenReturn(boundSql);
        when(slowQueryLogger.isSlowQuery(anyLong())).thenReturn(true);
        when(executor.query(mappedStatement, parameter, RowBounds.DEFAULT, null)).thenReturn(List.of());

        Method method = Executor.class.getMethod(
                "query", MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        );
        Invocation invocation = new Invocation(
                executor,
                method,
                new Object[]{mappedStatement, parameter, RowBounds.DEFAULT, null}
        );

        interceptor.intercept(invocation);

        ArgumentCaptor<Object> parameterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(slowQueryLogger).logSlowQuery(eq(sql), anyLong(), parameterCaptor.capture());

        String metadata = String.valueOf(parameterCaptor.getValue());
        assertThat(metadata)
                .contains("count=2")
                .contains("types=[String]")
                .contains("parameterObjectType=SensitiveParameter")
                .doesNotContain("Secr3t-Pass");
    }

    private static class SensitiveParameter {
        private final String username;
        private final String password;

        private SensitiveParameter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public String toString() {
            return "SensitiveParameter{username='%s', password='%s'}".formatted(username, password);
        }
    }
}
