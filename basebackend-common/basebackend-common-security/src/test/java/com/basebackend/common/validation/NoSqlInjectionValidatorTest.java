package com.basebackend.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL注入验证器测试
 */
@DisplayName("NoSqlInjectionValidator 测试")
class NoSqlInjectionValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "hello world",
            "用户名",
            "test@example.com",
            "John Doe",
            "Product Name 123"
    })
    @DisplayName("普通文本应通过验证")
    void normalTextShouldPass(String input) {
        assertFalse(NoSqlInjectionValidator.containsSqlInjection(input));
    }

    @Test
    @DisplayName("空值应通过验证")
    void nullAndEmptyShouldPass() {
        assertFalse(NoSqlInjectionValidator.containsSqlInjection(null));
        assertFalse(NoSqlInjectionValidator.containsSqlInjection(""));
        assertFalse(NoSqlInjectionValidator.containsSqlInjection("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM users",
            "INSERT INTO users VALUES (1)",
            "UPDATE users SET name='evil'",
            "DELETE FROM users",
            "DROP TABLE users"
    })
    @DisplayName("包含SQL关键字应被拒绝")
    void sqlKeywordsShouldBeRejected(String input) {
        assertTrue(NoSqlInjectionValidator.containsSqlInjection(input),
                "Should detect: " + input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin'--",
            "password' --",
            "/* comment */"
    })
    @DisplayName("SQL注释应被拒绝")
    void sqlCommentsShouldBeRejected(String input) {
        assertTrue(NoSqlInjectionValidator.containsSqlInjection(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "' OR '1'='1",
            "' OR 1=1",
            "1 = 1 --"
    })
    @DisplayName("逻辑注入应被拒绝")
    void logicInjectionShouldBeRejected(String input) {
        assertTrue(NoSqlInjectionValidator.containsSqlInjection(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1 UNION SELECT username FROM users",
            "' UNION ALL SELECT * FROM admin--"
    })
    @DisplayName("UNION注入应被拒绝")
    void unionInjectionShouldBeRejected(String input) {
        assertTrue(NoSqlInjectionValidator.containsSqlInjection(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1; SELECT * FROM users",
            "'; DROP TABLE users;--"
    })
    @DisplayName("堆叠查询应被拒绝")
    void stackedQueryShouldBeRejected(String input) {
        assertTrue(NoSqlInjectionValidator.containsSqlInjection(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SLEEP(5)",
            "BENCHMARK(1000000, SHA1('test'))"
    })
    @DisplayName("危险函数调用应被拒绝")
    void dangerousFunctionsShouldBeRejected(String input) {
        assertTrue(NoSqlInjectionValidator.containsSqlInjection(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0x61646D696E",
            "%00"
    })
    @DisplayName("编码绕过尝试应被拒绝")
    void encodingBypassShouldBeRejected(String input) {
        assertTrue(NoSqlInjectionValidator.containsSqlInjection(input));
    }

    @Test
    @DisplayName("严格模式检测更多关键字")
    void strictModeShouldDetectMoreKeywords() {
        String input = "order by name";

        // 非严格模式
        assertFalse(NoSqlInjectionValidator.containsSqlInjection(input, false));

        // 严格模式
        assertTrue(NoSqlInjectionValidator.containsSqlInjection(input, true));
    }

    @Test
    @DisplayName("部分匹配的单词不应被误报")
    void partialMatchShouldNotFalsePositive() {
        // 包含"select"但不是SQL的情况
        assertFalse(NoSqlInjectionValidator.containsSqlInjection("selection"));
        assertFalse(NoSqlInjectionValidator.containsSqlInjection("unselected"));

        // 包含"drop"但不是SQL的情况
        assertFalse(NoSqlInjectionValidator.containsSqlInjection("dropdown"));
        assertFalse(NoSqlInjectionValidator.containsSqlInjection("raindrop"));
    }

    @Test
    @DisplayName("大小写混合应被检测")
    void mixedCaseShouldBeDetected() {
        assertTrue(NoSqlInjectionValidator.containsSqlInjection("SeLeCt * FrOm users"));
        assertTrue(NoSqlInjectionValidator.containsSqlInjection("DrOp TaBlE users"));
    }
}
