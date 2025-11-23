//package com.basebackend.database.dynamic;
//
//import com.basebackend.database.dynamic.annotation.DS;
//import com.basebackend.database.dynamic.context.DataSourceContextHolder;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.EnableAspectJAutoProxy;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * 嵌套数据源切换集成测试
// * 测试真实的 AOP 切面和嵌套方法调用场景
// *
// * @author basebackend
// */
//@Slf4j
//@SpringBootTest(classes = NestedDataSourceIntegrationTest.TestConfig.class)
//class NestedDataSourceIntegrationTest {
//
//    @Autowired(required = false)
//    private OrderService orderService;
//
//    @Autowired(required = false)
//    private UserService userService;
//
//    @Autowired(required = false)
//    private ProductService productService;
//
//    @BeforeEach
//    void setUp() {
//        DataSourceContextHolder.clear();
//    }
//
//    @AfterEach
//    void tearDown() {
//        DataSourceContextHolder.clear();
//    }
//
//    /**
//     * 测试单层数据源切换
//     */
//    @Test
//    void testSingleLevelDataSourceSwitch() {
//        if (orderService == null) {
//            log.warn("OrderService not available, skipping test");
//            return;
//        }
//
//        String result = orderService.getOrderFromMaster();
//        assertEquals("master", result);
//
//        // 方法执行完毕后，上下文应该被清空
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//    }
//
//    /**
//     * 测试两层嵌套数据源切换
//     */
//    @Test
//    void testTwoLevelNestedDataSourceSwitch() {
//        if (orderService == null || userService == null) {
//            log.warn("Services not available, skipping test");
//            return;
//        }
//
//        List<String> trace = orderService.getOrderWithUser();
//
//        // 验证调用轨迹
//        assertEquals(4, trace.size());
//        assertEquals("master", trace.get(0)); // orderService 开始
//        assertEquals("slave1", trace.get(1)); // userService 调用
//        assertEquals("master", trace.get(2)); // 返回 orderService
//        assertEquals("master", trace.get(3)); // orderService 结束
//
//        // 方法执行完毕后，上下文应该被清空
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//    }
//
//    /**
//     * 测试三层嵌套数据源切换
//     */
//    @Test
//    void testThreeLevelNestedDataSourceSwitch() {
//        if (orderService == null || userService == null || productService == null) {
//            log.warn("Services not available, skipping test");
//            return;
//        }
//
//        List<String> trace = orderService.getOrderWithUserAndProduct();
//
//        // 验证调用轨迹
//        assertEquals(8, trace.size());
//        assertEquals("master", trace.get(0));  // orderService 开始
//        assertEquals("slave1", trace.get(1));  // userService 调用
//        assertEquals("slave1", trace.get(2));  // userService 中
//        assertEquals("slave2", trace.get(3));  // productService 调用
//        assertEquals("slave1", trace.get(4));  // 返回 userService
//        assertEquals("slave1", trace.get(5));  // userService 结束
//        assertEquals("master", trace.get(6));  // 返回 orderService
//        assertEquals("master", trace.get(7));  // orderService 结束
//
//        // 方法执行完毕后，上下文应该被清空
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//    }
//
//    /**
//     * 测试同一数据源的嵌套调用
//     */
//    @Test
//    void testSameDataSourceNestedCalls() {
//        if (orderService == null) {
//            log.warn("OrderService not available, skipping test");
//            return;
//        }
//
//        List<String> trace = orderService.nestedMasterCalls();
//
//        // 即使是同一数据源，栈深度也应该增加
//        assertEquals(4, trace.size());
//        assertEquals("master", trace.get(0));
//        assertEquals("master", trace.get(1)); // 嵌套调用，栈深度为 2
//        assertEquals("master", trace.get(2));
//        assertEquals("master", trace.get(3));
//
//        assertNull(DataSourceContextHolder.getDataSourceKey());
//    }
//
//    /**
//     * 测试配置类
//     */
//    @Configuration
//    @EnableAspectJAutoProxy
//    @ComponentScan(basePackageClasses = NestedDataSourceIntegrationTest.class)
//    static class TestConfig {
//    }
//
//    /**
//     * 订单服务 - 使用 master 数据源
//     */
//    @Service
//    @Slf4j
//    static class OrderService {
//
//        @Autowired(required = false)
//        private UserService userService;
//
//        @Autowired(required = false)
//        private ProductService productService;
//
//        @DS("master")
//        public String getOrderFromMaster() {
//            return DataSourceContextHolder.getDataSourceKey();
//        }
//
//        @DS("master")
//        public List<String> getOrderWithUser() {
//            List<String> trace = new ArrayList<>();
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master
//
//            if (userService != null) {
//                String userDs = userService.getUserFromSlave();
//                trace.add(userDs); // slave1
//            }
//
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master
//
//            return trace;
//        }
//
//        @DS("master")
//        public List<String> getOrderWithUserAndProduct() {
//            List<String> trace = new ArrayList<>();
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master
//
//            if (userService != null) {
//                List<String> userTrace = userService.getUserWithProduct();
//                trace.addAll(userTrace);
//            }
//
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master
//
//            return trace;
//        }
//
//        @DS("master")
//        public List<String> nestedMasterCalls() {
//            List<String> trace = new ArrayList<>();
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master, depth 1
//
//            String nested = anotherMasterMethod();
//            trace.add(nested); // master, depth 2
//
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master, depth 1
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // master, depth 1
//
//            return trace;
//        }
//
//        @DS("master")
//        public String anotherMasterMethod() {
//            return DataSourceContextHolder.getDataSourceKey();
//        }
//    }
//
//    /**
//     * 用户服务 - 使用 slave1 数据源
//     */
//    @Service
//    @Slf4j
//    static class UserService {
//
//        @Autowired(required = false)
//        private ProductService productService;
//
//        @DS("slave1")
//        public String getUserFromSlave() {
//            return DataSourceContextHolder.getDataSourceKey();
//        }
//
//        @DS("slave1")
//        public List<String> getUserWithProduct() {
//            List<String> trace = new ArrayList<>();
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // slave1
//
//            if (productService != null) {
//                String productDs = productService.getProductFromSlave2();
//                trace.add(productDs); // slave2
//            }
//
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // slave1
//            trace.add(DataSourceContextHolder.getDataSourceKey()); // slave1
//
//            return trace;
//        }
//    }
//
//    /**
//     * 产品服务 - 使用 slave2 数据源
//     */
//    @Service
//    @Slf4j
//    static class ProductService {
//
//        @DS("slave2")
//        public String getProductFromSlave2() {
//            return DataSourceContextHolder.getDataSourceKey();
//        }
//    }
//}
