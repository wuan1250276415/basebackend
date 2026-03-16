package com.basebackend.database.security.interceptor;

import com.basebackend.security.context.DataScopeContextHolder;
import com.basebackend.security.enums.DataScopeType;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DataScopeInterceptor 测试")
class DataScopeInterceptorTest {

    private final DataScopeInterceptor interceptor = new DataScopeInterceptor();

    @AfterEach
    void tearDown() {
        DataScopeContextHolder.clear();
    }

    @Test
    @DisplayName("受限数据权限下无法解析主表时应阻断执行")
    void shouldBlockWhenTableCannotBeExtracted() throws Throwable {
        DataScopeContextHolder.set(new DataScopeContextHolder.DataScopeContext(DataScopeType.DEPT, 1001L, 2001L));

        TestStatementHandler statementHandler = new TestStatementHandler(buildMappedStatement("SELECT 1"), "SELECT 1");
        Method prepareMethod = StatementHandler.class.getMethod("prepare", Connection.class, Integer.class);
        Invocation invocation = new Invocation(statementHandler, prepareMethod, new Object[]{null, null});

        assertThatThrownBy(() -> interceptor.intercept(invocation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("阻断");
    }

    @Test
    @DisplayName("ALL 权限下应直接放行")
    void shouldProceedWhenDataScopeIsAll() throws Throwable {
        DataScopeContextHolder.set(DataScopeType.ALL);

        TestStatementHandler statementHandler = new TestStatementHandler(buildMappedStatement("SELECT 1"), "SELECT 1");
        Method prepareMethod = StatementHandler.class.getMethod("prepare", Connection.class, Integer.class);
        Invocation invocation = new Invocation(statementHandler, prepareMethod, new Object[]{null, null});

        assertThatCode(() -> interceptor.intercept(invocation)).doesNotThrowAnyException();
    }

    private MappedStatement buildMappedStatement(String sql) {
        Configuration configuration = new Configuration();
        StaticSqlSource sqlSource = new StaticSqlSource(configuration, sql);
        return new MappedStatement.Builder(configuration, "testMapper.select", sqlSource, SqlCommandType.SELECT).build();
    }

    static class TestStatementHandler implements StatementHandler {
        private final MappedStatement mappedStatement;
        private final BoundSql boundSql;

        TestStatementHandler(MappedStatement mappedStatement, String sql) {
            this.mappedStatement = mappedStatement;
            this.boundSql = new BoundSql(mappedStatement.getConfiguration(), sql, List.of(), null);
        }

        public MappedStatement getMappedStatement() {
            return mappedStatement;
        }

        @Override
        public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
            return null;
        }

        @Override
        public void parameterize(Statement statement) throws SQLException {
        }

        @Override
        public void batch(Statement statement) throws SQLException {
        }

        @Override
        public int update(Statement statement) throws SQLException {
            return 0;
        }

        @Override
        public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
            return List.of();
        }

        @Override
        public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
            return null;
        }

        @Override
        public BoundSql getBoundSql() {
            return boundSql;
        }

        @Override
        public ParameterHandler getParameterHandler() {
            return null;
        }
    }
}
