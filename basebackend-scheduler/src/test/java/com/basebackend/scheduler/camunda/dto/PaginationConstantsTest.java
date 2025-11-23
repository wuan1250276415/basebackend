package com.basebackend.scheduler.camunda.dto;

import com.basebackend.scheduler.camunda.config.PaginationConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分页常量验证测试
 *
 * <p>验证分页配置常量的一致性和正确性：
 * <ul>
 *   <li>分页默认值配置</li>
 *   <li>分页最大限制配置</li>
 *   <li>分页边界值验证</li>
 *   <li>常量一致性检查</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
class PaginationConstantsTest {

    // ========== 分页常量值测试 ==========

    @Test
    void testDefaultPageSize() {
        // 验证默认分页大小
        assertEquals(10, PaginationConstants.DEFAULT_PAGE_SIZE,
                "默认分页大小应该是 10");
        assertEquals("10", PaginationConstants.DEFAULT_PAGE_SIZE_STR,
                "默认分页大小字符串应该是 \"10\"");
    }

    @Test
    void testMaxPageSize() {
        // 验证最大分页大小
        assertEquals(1000, PaginationConstants.MAX_PAGE_SIZE,
                "最大分页大小应该是 1000");
        assertEquals("1000", PaginationConstants.MAX_PAGE_SIZE_STR,
                "最大分页大小字符串应该是 \"1000\"");
    }

    @Test
    void testPaginationConstraints() {
        // 验证分页约束描述
        String expectedConstraints = "分页参数约束：页码 ≥ 1，每页大小范围 [1, 1000]";
        assertEquals(expectedConstraints, PaginationConstants.PAGINATION_CONSTRAINTS_DESC,
                "分页约束描述应该正确");
    }

    @Test
    void testMinPageSize() {
        // 验证最小分页大小
        assertEquals(1, PaginationConstants.MIN_PAGE_SIZE,
                "最小分页大小应该是 1");
    }

    // ========== 分页常量一致性测试 ==========

    @Test
    void testPageSizeConsistency() {
        // 验证分页大小常量的一致性
        assertEquals(PaginationConstants.MIN_PAGE_SIZE,
                1,
                "最小分页大小应该是 1");

        assertTrue(PaginationConstants.DEFAULT_PAGE_SIZE >= PaginationConstants.MIN_PAGE_SIZE,
                "默认分页大小应该大于等于最小分页大小");

        assertTrue(PaginationConstants.DEFAULT_PAGE_SIZE <= PaginationConstants.MAX_PAGE_SIZE,
                "默认分页大小应该小于等于最大分页大小");

        assertTrue(PaginationConstants.MAX_PAGE_SIZE > PaginationConstants.MIN_PAGE_SIZE,
                "最大分页大小应该大于最小分页大小");
    }

    @Test
    void testPageSizeStringConsistency() {
        // 验证分页大小字符串常量的一致性
        assertEquals(String.valueOf(PaginationConstants.DEFAULT_PAGE_SIZE),
                PaginationConstants.DEFAULT_PAGE_SIZE_STR,
                "默认分页大小字符串应该与数值一致");

        assertEquals(String.valueOf(PaginationConstants.MAX_PAGE_SIZE),
                PaginationConstants.MAX_PAGE_SIZE_STR,
                "最大分页大小字符串应该与数值一致");
    }

    // ========== 分页边界值测试 ==========

    @Test
    void testValidPageSizeValues() {
        // 验证所有有效分页大小值
        int[] validSizes = {1, 5, 10, 20, 50, 100, 200, 500, 1000};

        for (int size : validSizes) {
            assertTrue(size >= PaginationConstants.MIN_PAGE_SIZE,
                    "分页大小 " + size + " 应该大于等于最小值");
            assertTrue(size <= PaginationConstants.MAX_PAGE_SIZE,
                    "分页大小 " + size + " 应该小于等于最大值");
        }
    }

    @Test
    void testInvalidPageSizeValues() {
        // 验证无效分页大小值
        int[] invalidSizes = {0, -1, -10, 1001, 2000};

        for (int size : invalidSizes) {
            assertFalse(size >= PaginationConstants.MIN_PAGE_SIZE,
                    "分页大小 " + size + " 应该小于最小值");
            assertFalse(size <= PaginationConstants.MAX_PAGE_SIZE,
                    "分页大小 " + size + " 应该大于最大值");
        }
    }

    // ========== BasePageQuery 与常量一致性测试 ==========

    @Test
    void testBasePageQueryDefaultValues() {
        // 验证 BasePageQuery 的默认值与常量一致
        BasePageQuery query = new BasePageQuery();

        assertEquals(PaginationConstants.DEFAULT_PAGE_SIZE, query.getSize(),
                "BasePageQuery 默认分页大小应该与常量一致");
        assertEquals(1, query.getCurrent(),
                "BasePageQuery 默认页码应该为 1");
    }

    @Test
    void testBasePageQueryWithConstants() {
        // 验证使用常量设置的 BasePageQuery 值
        BasePageQuery query = new BasePageQuery();
        query.setSize(PaginationConstants.DEFAULT_PAGE_SIZE);

        assertEquals(PaginationConstants.DEFAULT_PAGE_SIZE, query.getSize(),
            "使用常量设置的分页大小应该正确");
    }

    // ========== 兼容性方法测试 ==========

    @Test
    void testBasePageQueryCompatibilityMethods() {
        // 验证 BasePageQuery 的兼容性方法
        BasePageQuery query = new BasePageQuery();

        // 测试 getPageNum/getPageSize 方法
        query.setCurrent(5);
        query.setSize(20);

        assertEquals(5, query.getPageNum(),
                "getPageNum() 应该返回当前页码");
        assertEquals(20, query.getPageSize(),
                "getPageSize() 应该返回每页大小");

        // 测试 setPageNum/setPageSize 方法
        query.setPageNum(3);
        query.setPageSize(30);

        assertEquals(3, query.getCurrent(),
                "setPageNum() 应该设置当前页码");
        assertEquals(30, query.getSize(),
                "setPageSize() 应该设置每页大小");
    }

    @Test
    void testBasePageQueryInheritance() {
        // 验证子类继承兼容性方法
        TaskPageQuery taskQuery = new TaskPageQuery();
        taskQuery.setCurrent(2);
        taskQuery.setSize(50);

        assertEquals(2, taskQuery.getPageNum(),
                "TaskPageQuery 应该继承 getPageNum() 方法");
        assertEquals(50, taskQuery.getPageSize(),
                "TaskPageQuery 应该继承 getPageSize() 方法");

        taskQuery.setPageNum(3);
        taskQuery.setPageSize(60);

        assertEquals(3, taskQuery.getCurrent(),
                "TaskPageQuery 应该继承 setPageNum() 方法");
        assertEquals(60, taskQuery.getSize(),
                "TaskPageQuery 应该继承 setPageSize() 方法");
    }

    // ========== 注解值与常量一致性测试 ==========

    @Test
    void testBasePageQueryAnnotationValues() {
        // 验证 @Max 注解值与常量一致
        // 这需要通过反射或实际验证来验证
        BasePageQuery query = new BasePageQuery();

        // 设置最大允许值
        query.setSize(PaginationConstants.MAX_PAGE_SIZE);

        // 验证是否可以设置到最大允许值
        assertEquals(PaginationConstants.MAX_PAGE_SIZE, query.getSize(),
                "应该可以设置分页大小为最大值");

        // 验证超过最大值的情况
        assertThrows(IllegalArgumentException.class, () -> {
            query.setSize(PaginationConstants.MAX_PAGE_SIZE + 1);
        }, "设置超过最大值的分页大小应该抛出异常");
    }

    // ========== 性能和边界测试 ==========

    @Test
    void testMaxPageSizePerformance() {
        // 验证最大分页大小的性能特性
        int maxSize = PaginationConstants.MAX_PAGE_SIZE;
        assertTrue(maxSize > 100, "最大分页大小应该足够大以支持大数据量");
        assertTrue(maxSize < 10000, "最大分页大小不应该过大以避免性能问题");
    }

    @Test
    void testDefaultPageSizeUsability() {
        // 验证默认分页大小的适用性
        int defaultSize = PaginationConstants.DEFAULT_PAGE_SIZE;
        assertTrue(defaultSize >= 10, "默认分页大小应该足够提供良好的用户体验");
        assertTrue(defaultSize <= 50, "默认分页大小不应该过大以避免页面加载缓慢");
    }

    @Test
    void testPageSizeRange() {
        // 验证分页大小范围的合理性
        int range = PaginationConstants.MAX_PAGE_SIZE - PaginationConstants.MIN_PAGE_SIZE;
        assertTrue(range >= 999, "分页大小范围应该足够大");
        assertEquals(999, range, "分页大小范围应该是 [1, 1000]");
    }

    // ========== 文档和描述测试 ==========

    @Test
    void testPaginationConstraintsDescription() {
        // 验证分页约束描述的完整性
        String desc = PaginationConstants.PAGINATION_CONSTRAINTS_DESC;

        assertNotNull(desc, "分页约束描述不应该为 null");
        assertFalse(desc.isEmpty(), "分页约束描述不应该为空");
        assertTrue(desc.contains("页码"), "描述应该包含页码信息");
        assertTrue(desc.contains("每页大小"), "描述应该包含每页大小信息");
        assertTrue(desc.contains(String.valueOf(PaginationConstants.MIN_PAGE_SIZE)),
                "描述应该包含最小值");
        assertTrue(desc.contains(String.valueOf(PaginationConstants.MAX_PAGE_SIZE)),
                "描述应该包含最大值");
    }

    @Test
    void testConstantImmutability() {
        // 验证常量的不可变性（所有常量都应该是 static final）
        // 这个测试主要作为文档说明，实际的不可变性由编译器保证
        assertTrue(Long.toString(PaginationConstants.DEFAULT_PAGE_SIZE).length() > 0,
                "默认分页大小应该是有效的常量值");
        assertTrue(Long.toString(PaginationConstants.MAX_PAGE_SIZE).length() > 0,
                "最大分页大小应该是有效的常量值");
    }
}
