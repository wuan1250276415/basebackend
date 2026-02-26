//package com.basebackend.database.dynamic;
//
//import com.basebackend.database.dynamic.annotation.DS;
//import com.basebackend.database.dynamic.context.DataSourceContextHolder;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.stereotype.Service;
//import org.springframework.test.context.TestPropertySource;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * 嵌套数据源切换测试
// * 验证数据源栈管理和嵌套调用场景
// *
// * @author basebackend
// */
//@SpringBootTest(classes = {NestedDataSourceSwitchingTest.TestService.class})
//@TestPropertySource(properties = {
//    "database.enhanced.dynamic-datasource.enabled=true",
//    "database.enhanced.dynamic-datasource.primary=master"
//})
//class NestedDataSourceSwitchingTest {
//
//    @BeforeEach
//    void setUp() {
//        // 确保每个测试开始前上下文是干净的
//        DataSourceContextHolder.clear();
//    }
//
//    @AfterEach
//    void tearDown() {
//        // 清理上下文
//        DataSourceContextHolder.clear();
//    }
//
//    /**
//     * 测试基本的数据源设置和获取
//     */
//    @Test
//    void testBasicDataSourceSetting() {
//        // 初始状态应该为空
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//
//        // 设置数据源
//        DataSourceContextHolder.setDataSourceKey("slave1");
//        assertEquals("slave1", DataSourceContextHolder.getDataSourceKey());
//        assertEquals(1, DataSourceContextHolder.getStackDepth());
//
//        // 清除数据源
//        DataSourceContextHolder.clearDataSourceKey();
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//    }
//
//    /**
//     * 测试嵌套数据源切换
//     */
//    @Test
//    void testNestedDataSourceSwitching() {
//        // 第一层：设置为 slave1
//        DataSourceContextHolder.setDataSourceKey("slave1");
//        assertEquals("slave1", DataSourceContextHolder.getDataSourceKey());
//        assertEquals(1, DataSourceContextHolder.getStackDepth());
//
//        // 第二层：嵌套设置为 slave2
//        DataSourceContextHolder.setDataSourceKey("slave2");
//        assertEquals("slave2", DataSourceContextHolder.getDataSourceKey());
//        assertEquals(2, DataSourceContextHolder.getStackDepth());
//
//        // 第三层：嵌套设置为 master
//        DataSourceContextHolder.setDataSourceKey("master");
//        assertEquals("master", DataSourceContextHolder.getDataSourceKey());
//        assertEquals(3, DataSourceContextHolder.getStackDepth());
//
//        // 清除第三层，应该恢复到 slave2
//        DataSourceContextHolder.clearDataSourceKey();
//        assertEquals("slave2", DataSourceContextHolder.getDataSourceKey());
//        assertEquals(2, DataSourceContextHolder.getStackDepth());
//
//        // 清除第二层，应该恢复到 slave1
//        DataSourceContextHolder.clearDataSourceKey();
//        assertEquals("slave1", DataSourceContextHolder.getDataSourceKey());
//        assertEquals(1, DataSourceContextHolder.getStackDepth());
//
//        // 清除第一层，应该为空
//        DataSourceContextHolder.clearDataSourceKey();
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//    }
//
//    /**
//     * 测试深层嵌套（5层）
//     */
//    @Test
//    void testDeepNestedDataSourceSwitching() {
//        String[] dataSources = {"ds1", "ds2", "ds3", "ds4", "ds5"};
//
//        // 逐层设置数据源
//        for (int i = 0; i < dataSources.length; i++) {
//            DataSourceContextHolder.setDataSourceKey(dataSources[i]);
//            assertEquals(dataSources[i], DataSourceContextHolder.getDataSourceKey());
//            assertEquals(i + 1, DataSourceContextHolder.getStackDepth());
//        }
//
//        // 逐层清除数据源
//        for (int i = dataSources.length - 1; i >= 0; i--) {
//            assertEquals(dataSources[i], DataSourceContextHolder.getDataSourceKey());
//            assertEquals(i + 1, DataSourceContextHolder.getStackDepth());
//            DataSourceContextHolder.clearDataSourceKey();
//        }
//
//        // 最终应该为空
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//    }
//
//    /**
//     * 测试完全清空操作
//     */
//    @Test
//    void testClearAllDataSources() {
//        // 设置多层数据源
//        DataSourceContextHolder.setDataSourceKey("ds1");
//        DataSourceContextHolder.setDataSourceKey("ds2");
//        DataSourceContextHolder.setDataSourceKey("ds3");
//        assertEquals(3, DataSourceContextHolder.getStackDepth());
//
//        // 完全清空
//        DataSourceContextHolder.clear();
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//    }
//
//    /**
//     * 测试空值和空字符串处理
//     */
//    @Test
//    void testNullAndEmptyDataSourceKey() {
//        // 设置 null 应该被忽略
//        DataSourceContextHolder.setDataSourceKey(null);
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//
//        // 设置空字符串应该被忽略
//        DataSourceContextHolder.setDataSourceKey("");
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//
//        // 设置空白字符串应该被忽略
//        DataSourceContextHolder.setDataSourceKey("   ");
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//    }
//
//    /**
//     * 测试在空栈上清除操作
//     */
//    @Test
//    void testClearOnEmptyStack() {
//        // 在空栈上清除不应该抛出异常
//        assertDoesNotThrow(() -> DataSourceContextHolder.clearDataSourceKey());
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//        assertEquals(0, DataSourceContextHolder.getStackDepth());
//    }
//
//    /**
//     * 测试模拟方法调用场景
//     */
//    @Test
//    void testSimulatedMethodCallScenario() {
//        // 模拟方法 A 使用 master
//        DataSourceContextHolder.setDataSourceKey("master");
//        assertEquals("master", DataSourceContextHolder.getDataSourceKey());
//
//        // 方法 A 调用方法 B，方法 B 使用 slave1
//        DataSourceContextHolder.setDataSourceKey("slave1");
//        assertEquals("slave1", DataSourceContextHolder.getDataSourceKey());
//
//        // 方法 B 调用方法 C，方法 C 使用 slave2
//        DataSourceContextHolder.setDataSourceKey("slave2");
//        assertEquals("slave2", DataSourceContextHolder.getDataSourceKey());
//
//        // 方法 C 执行完毕，恢复到 slave1
//        DataSourceContextHolder.clearDataSourceKey();
//        assertEquals("slave1", DataSourceContextHolder.getDataSourceKey());
//
//        // 方法 B 执行完毕，恢复到 master
//        DataSourceContextHolder.clearDataSourceKey();
//        assertEquals("master", DataSourceContextHolder.getDataSourceKey());
//
//        // 方法 A 执行完毕，清空
//        DataSourceContextHolder.clearDataSourceKey();
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//    }
//
//    /**
//     * 测试服务类，用于测试 @DS 注解的嵌套场景
//     */
//    @Service
//    static class TestService {
//
//        @DS("master")
//        public String methodWithMaster() {
//            return DataSourceContextHolder.getDataSourceKey();
//        }
//
//        @DS("slave1")
//        public String methodWithSlave1() {
//            return DataSourceContextHolder.getDataSourceKey();
//        }
//
//        @DS("slave2")
//        public String methodWithSlave2() {
//            return DataSourceContextHolder.getDataSourceKey();
//        }
//
//        @DS("master")
//        public String nestedMethodCall() {
//            String ds1 = DataSourceContextHolder.getDataSourceKey();
//            String ds2 = methodWithSlave1();
//            String ds3 = DataSourceContextHolder.getDataSourceKey();
//            return ds1 + "," + ds2 + "," + ds3;
//        }
//    }
//}
