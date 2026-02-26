package com.basebackend.common.datascope.handler;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.datascope.config.DataScopeProperties;
import com.basebackend.common.datascope.context.DataScopeContext;
import com.basebackend.common.datascope.enums.DataScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeSqlBuilder 单元测试
 */
class DataScopeSqlBuilderTest {

    private DataScopeProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DataScopeProperties();
        UserContextHolder.set(UserContext.builder()
                .userId(100L).deptId(10L).build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        DataScopeContext.clear();
    }

    @Nested
    @DisplayName("buildCondition")
    class BuildCondition {

        @Test
        @DisplayName("ALL 类型返回空字符串")
        void shouldReturnEmptyForAll() {
            String sql = DataScopeSqlBuilder.buildCondition(
                    DataScopeType.ALL, "d", "dept_id", "u", "create_by", properties);
            assertThat(sql).isEmpty();
        }

        @Test
        @DisplayName("SELF 类型生成用户过滤条件")
        void shouldBuildSelfCondition() {
            String sql = DataScopeSqlBuilder.buildCondition(
                    DataScopeType.SELF, "d", "dept_id", "u", "create_by", properties);
            assertThat(sql).isEqualTo("u.create_by = 100");
        }

        @Test
        @DisplayName("DEPT 类型生成部门过滤条件")
        void shouldBuildDeptCondition() {
            String sql = DataScopeSqlBuilder.buildCondition(
                    DataScopeType.DEPT, "d", "dept_id", "u", "create_by", properties);
            assertThat(sql).isEqualTo("d.dept_id = 10");
        }

        @Test
        @DisplayName("DEPT_AND_BELOW 类型生成子查询")
        void shouldBuildDeptAndBelowCondition() {
            String sql = DataScopeSqlBuilder.buildCondition(
                    DataScopeType.DEPT_AND_BELOW, "d", "dept_id", "u", "create_by", properties);
            assertThat(sql).contains("IN (SELECT dept_id FROM");
            assertThat(sql).contains("FIND_IN_SET(10, ancestors)");
        }

        @Test
        @DisplayName("CUSTOM 类型使用 DataScopeContext")
        void shouldBuildCustomCondition() {
            DataScopeContext.set("d.dept_id IN (1, 2, 3)");
            String sql = DataScopeSqlBuilder.buildCondition(
                    DataScopeType.CUSTOM, "d", "dept_id", "u", "create_by", properties);
            assertThat(sql).isEqualTo("d.dept_id IN (1, 2, 3)");
        }

        @Test
        @DisplayName("CUSTOM 无上下文时返回空")
        void shouldReturnEmptyForCustomWithoutContext() {
            String sql = DataScopeSqlBuilder.buildCondition(
                    DataScopeType.CUSTOM, "d", "dept_id", "u", "create_by", properties);
            assertThat(sql).isEmpty();
        }

        @Test
        @DisplayName("UserContext 为 null 时返回空")
        void shouldReturnEmptyWhenNoUser() {
            UserContextHolder.clear();
            String sql = DataScopeSqlBuilder.buildCondition(
                    DataScopeType.SELF, "d", "dept_id", "u", "create_by", properties);
            assertThat(sql).isEmpty();
        }
    }

    @Nested
    @DisplayName("内部构建方法")
    class InternalMethods {

        @Test
        @DisplayName("buildDeptCondition deptId 为 null 返回空")
        void shouldReturnEmptyForNullDeptId() {
            assertThat(DataScopeSqlBuilder.buildDeptCondition("d", "dept_id", null)).isEmpty();
        }

        @Test
        @DisplayName("buildSelfCondition userId 为 null 返回空")
        void shouldReturnEmptyForNullUserId() {
            assertThat(DataScopeSqlBuilder.buildSelfCondition("u", "create_by", null)).isEmpty();
        }

        @Test
        @DisplayName("buildDeptAndBelowCondition 使用自定义表名")
        void shouldUseCustomTableName() {
            String sql = DataScopeSqlBuilder.buildDeptAndBelowCondition(
                    "d", "dept_id", 5L, "my_dept_table");
            assertThat(sql).contains("my_dept_table");
        }
    }
}
