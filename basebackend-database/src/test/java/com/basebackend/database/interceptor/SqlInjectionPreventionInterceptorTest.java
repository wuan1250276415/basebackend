package com.basebackend.database.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SQL注入防护拦截器测试
 * 测试各种SQL注入攻击的检测和阻断
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SqlInjectionPreventionInterceptor SQL注入防护测试")
class SqlInjectionPreventionInterceptorTest {

    @Mock
    private StatementHandler statementHandler;

    @Mock
    private BoundSql boundSql;

    @Mock
    private Connection connection;

    private SqlInjectionPreventionInterceptor interceptor = new SqlInjectionPreventionInterceptor();

    @Test
    @DisplayName("正常SQL查询应该被放行")
    void shouldAllowNormalSqlQuery() {
        // Given
        String normalSql = "SELECT * FROM users WHERE id = ?";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(normalSql);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);
    }

    @Test
    @DisplayName("带参数的正常SQL应该被放行")
    void shouldAllowParameterizedSql() {
        // Given
        String parameterizedSql = "SELECT * FROM users WHERE name = ? AND age > ?";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(parameterizedSql);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);
    }

    @Test
    @DisplayName("SQL注释应该被阻断")
    void shouldBlockSqlComments() {
        // Given - 单行注释
        String sqlWithComment = "SELECT * FROM users -- This is a comment";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(sqlWithComment);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");

        // Given - 多行注释
        String sqlWithMultiLineComment = "SELECT * /* comment */ FROM users";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(sqlWithMultiLineComment);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");
    }

    @Test
    @DisplayName("DROP语句应该被阻断")
    void shouldBlockDropStatements() {
        // Given - DROP DATABASE
        String dropDbSql = "DROP DATABASE testdb";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(dropDbSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");

        // Given - DROP TABLE
        String dropTableSql = "DROP TABLE users";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(dropTableSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");

        // Given - DROP INDEX
        String dropIndexSql = "DROP INDEX idx_users";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(dropIndexSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");
    }

    @Test
    @DisplayName("ALTER语句应该被阻断")
    void shouldBlockAlterStatements() {
        // Given
        String alterSql = "ALTER TABLE users ADD COLUMN email VARCHAR(100)";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(alterSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");
    }

    @Test
    @DisplayName("TRUNCATE语句应该被阻断")
    void shouldBlockTruncateStatements() {
        // Given
        String truncateSql = "TRUNCATE TABLE users";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(truncateSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");
    }

    @Test
    @DisplayName("OR 1=1攻击应该被阻断")
    void shouldBlockOr111Attack() {
        // Given
        String or111Sql = "SELECT * FROM users WHERE id = 1 OR 1=1";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(or111Sql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");
    }

    @Test
    @DisplayName("UNION SELECT攻击应该被阻断")
    void shouldBlockUnionSelectAttack() {
        // Given
        String unionSql = "SELECT * FROM users UNION SELECT * FROM admin_users";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(unionSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");
    }

    @Test
    @DisplayName("分号分隔的多条语句应该被阻断")
    void shouldBlockMultipleStatementsWithSemicolon() {
        // Given
        String multiStmtSql = "SELECT * FROM users; DROP TABLE admin;";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(multiStmtSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");
    }

    @Test
    @DisplayName("大小写不敏感的检测")
    void shouldDetectCaseInsensitively() {
        // Given - 大写
        String upperCaseSql = "SELECT * FROM users DROP TABLE admin";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(upperCaseSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");

        // Given - 小写
        String lowerCaseSql = "select * from users drop table admin";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(lowerCaseSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");

        // Given - 混合大小写
        String mixedCaseSql = "SeLeCt * FrOm users DrOp TaBlE aDmIn";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(mixedCaseSql);

        // When & Then - 应该抛出异常
        assertThatIllegalArgumentException()
            .isThrownBy(() -> interceptor.beforePrepare(statementHandler, connection, 30))
            .withMessage("Potentially dangerous SQL detected");
    }

    @Test
    @DisplayName("null BoundSql应该被跳过")
    void shouldSkipNullBoundSql() {
        // Given
        when(statementHandler.getBoundSql()).thenReturn(null);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);

        // 验证getBoundSql被调用一次，但没有其他调用
        verify(statementHandler, times(1)).getBoundSql();
    }

    @Test
    @DisplayName("null SQL应该被跳过")
    void shouldSkipNullSql() {
        // Given
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(null);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);
    }

    @Test
    @DisplayName("空SQL应该被跳过")
    void shouldSkipEmptySql() {
        // Given
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn("");

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);
    }

    @Test
    @DisplayName("纯空白SQL应该被跳过")
    void shouldSkipWhitespaceOnlySql() {
        // Given
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn("   ");

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);
    }

    @Test
    @DisplayName("INSERT、UPDATE、DELETE语句应该被放行")
    void shouldAllowInsertUpdateDelete() {
        // Given - INSERT
        String insertSql = "INSERT INTO users (name, email) VALUES (?, ?)";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(insertSql);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);

        // Given - UPDATE
        String updateSql = "UPDATE users SET email = ? WHERE id = ?";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(updateSql);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);

        // Given - DELETE
        String deleteSql = "DELETE FROM users WHERE id = ?";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(deleteSql);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);
    }

    @Test
    @DisplayName("事务相关语句应该被正确处理")
    void shouldHandleTransactionStatements() {
        // Given
        String beginSql = "BEGIN";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(beginSql);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);

        // Given
        String commitSql = "COMMIT";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(commitSql);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);

        // Given
        String rollbackSql = "ROLLBACK";
        when(statementHandler.getBoundSql()).thenReturn(boundSql);
        when(boundSql.getSql()).thenReturn(rollbackSql);

        // When & Then - 不应该抛出异常
        interceptor.beforePrepare(statementHandler, connection, 30);
    }
}
