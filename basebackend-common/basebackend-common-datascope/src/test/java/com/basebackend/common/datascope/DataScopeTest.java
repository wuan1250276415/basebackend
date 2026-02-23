package com.basebackend.common.datascope;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.datascope.config.DataScopeProperties;
import com.basebackend.common.datascope.context.DataScopeContext;
import com.basebackend.common.datascope.handler.DataScopeSqlBuilder;
import com.basebackend.common.datascope.enums.DataScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据权限单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
class DataScopeTest {

    private DataScopeProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DataScopeProperties();

        UserContext userContext = UserContext.builder()
                .userId(100L)
                .username("testuser")
                .deptId(10L)
                .roles(Set.of("ROLE_USER"))
                .build();
        UserContextHolder.set(userContext);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        DataScopeContext.clear();
    }

    @Test
    @DisplayName("ALL 类型 - 不追加条件")
    void testAllScope() {
        String condition = DataScopeSqlBuilder.buildCondition(
                DataScopeType.ALL, "d", "dept_id", "u", "create_by", properties);
        assertEquals("", condition);
    }

    @Test
    @DisplayName("DEPT 类型 - 本部门条件")
    void testDeptScope() {
        String condition = DataScopeSqlBuilder.buildCondition(
                DataScopeType.DEPT, "d", "dept_id", "u", "create_by", properties);
        assertEquals("d.dept_id = 10", condition);
    }

    @Test
    @DisplayName("DEPT_AND_BELOW 类型 - 本部门及以下条件")
    void testDeptAndBelowScope() {
        String condition = DataScopeSqlBuilder.buildCondition(
                DataScopeType.DEPT_AND_BELOW, "d", "dept_id", "u", "create_by", properties);
        assertEquals(
                "d.dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id = 10 OR FIND_IN_SET(10, ancestors))",
                condition
        );
    }

    @Test
    @DisplayName("SELF 类型 - 仅本人条件")
    void testSelfScope() {
        String condition = DataScopeSqlBuilder.buildCondition(
                DataScopeType.SELF, "d", "dept_id", "u", "create_by", properties);
        assertEquals("u.create_by = 100", condition);
    }

    @Test
    @DisplayName("自定义表别名和字段名")
    void testCustomAliasAndField() {
        String condition = DataScopeSqlBuilder.buildCondition(
                DataScopeType.DEPT, "dept", "department_id", "usr", "creator", properties);
        assertEquals("dept.department_id = 10", condition);
    }

    @Test
    @DisplayName("超管跳过 - 配置项默认开启")
    void testSuperAdminSkipDefault() {
        assertTrue(properties.isSuperAdminSkip());
    }

    @Test
    @DisplayName("ThreadLocal 正确清理")
    void testThreadLocalCleanup() {
        DataScopeContext.set("test_condition");
        assertTrue(DataScopeContext.isPresent());
        assertEquals("test_condition", DataScopeContext.get());

        DataScopeContext.clear();
        assertFalse(DataScopeContext.isPresent());
        assertNull(DataScopeContext.get());
    }

    @Test
    @DisplayName("无用户上下文时不生成条件")
    void testNoUserContext() {
        UserContextHolder.clear();
        String condition = DataScopeSqlBuilder.buildCondition(
                DataScopeType.DEPT, "d", "dept_id", "u", "create_by", properties);
        assertEquals("", condition);
    }

    @Test
    @DisplayName("自定义部门表名")
    void testCustomDeptTableName() {
        properties.setDeptTableName("t_department");
        String condition = DataScopeSqlBuilder.buildCondition(
                DataScopeType.DEPT_AND_BELOW, "d", "dept_id", "u", "create_by", properties);
        assertEquals(
                "d.dept_id IN (SELECT dept_id FROM t_department WHERE dept_id = 10 OR FIND_IN_SET(10, ancestors))",
                condition
        );
    }

    @Test
    @DisplayName("DataScopeProperties 默认值")
    void testPropertiesDefaults() {
        DataScopeProperties props = new DataScopeProperties();
        assertTrue(props.isEnabled());
        assertTrue(props.isSuperAdminSkip());
        assertEquals("sys_dept", props.getDeptTableName());
    }
}
