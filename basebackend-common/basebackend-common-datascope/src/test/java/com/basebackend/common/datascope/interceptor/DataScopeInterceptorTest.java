package com.basebackend.common.datascope.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeInterceptor 单元测试
 */
class DataScopeInterceptorTest {

    private final DataScopeInterceptor interceptor = new DataScopeInterceptor();

    @Test
    @DisplayName("已有 WHERE 且存在 ORDER BY 时应在 ORDER BY 前注入条件")
    void shouldInjectBeforeOrderByWhenWhereExists() throws Exception {
        String sql = "SELECT * FROM sys_user WHERE status = 1 ORDER BY id DESC";
        String result = invokeInject(sql, "dept_id = 10");

        assertThat(result).isEqualTo(
                "SELECT * FROM sys_user WHERE status = 1 AND dept_id = 10 ORDER BY id DESC"
        );
    }

    @Test
    @DisplayName("无 WHERE 且存在 LIMIT 时应在 LIMIT 前注入 WHERE 条件")
    void shouldInjectWhereBeforeLimitWhenNoWhere() throws Exception {
        String sql = "SELECT * FROM sys_user LIMIT 20";
        String result = invokeInject(sql, "dept_id = 10");

        assertThat(result).isEqualTo("SELECT * FROM sys_user WHERE dept_id = 10 LIMIT 20");
    }

    @Test
    @DisplayName("仅子查询包含 WHERE 时不应误判主查询 WHERE")
    void shouldIgnoreSubQueryWhere() throws Exception {
        String sql = "SELECT * FROM (SELECT * FROM sys_user WHERE status = 1) t ORDER BY id DESC";
        String result = invokeInject(sql, "dept_id = 10");

        assertThat(result).isEqualTo(
                "SELECT * FROM (SELECT * FROM sys_user WHERE status = 1) t WHERE dept_id = 10 ORDER BY id DESC"
        );
    }

    private String invokeInject(String sql, String condition) throws Exception {
        Method method = DataScopeInterceptor.class
                .getDeclaredMethod("injectDataScopeCondition", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(interceptor, sql, condition);
    }
}

