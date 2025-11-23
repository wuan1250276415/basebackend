package com.basebackend.database.dynamic.example;

import com.basebackend.database.dynamic.annotation.DS;
import com.basebackend.database.dynamic.context.DataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 嵌套数据源切换示例
 * 演示如何在方法调用链中使用不同的数据源
 * 
 * @author basebackend
 */
@Slf4j
public class NestedDataSourceExample {
    
    /**
     * 订单服务示例
     * 演示主数据源和从数据源的嵌套调用
     */
    @Service
    @Slf4j
    public static class OrderService {
        
        @Autowired(required = false)
        private UserService userService;
        
        @Autowired(required = false)
        private ProductService productService;
        
        /**
         * 获取订单详情
         * 使用 master 数据源查询订单，然后调用其他服务查询关联数据
         */
        @DS("master")
        public void getOrderDetail(Long orderId) {
            log.info("=== 开始查询订单详情 ===");
            log.info("当前数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            // 模拟查询订单
            log.info("从 master 数据源查询订单: {}", orderId);
            
            // 调用用户服务（会切换到 slave1）
            if (userService != null) {
                userService.getUserInfo(123L);
            }
            
            // 返回后，数据源自动恢复到 master
            log.info("返回 OrderService，当前数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            log.info("=== 订单详情查询完成 ===\n");
        }
        
        /**
         * 获取完整订单信息
         * 演示三层嵌套调用
         */
        @DS("master")
        public void getCompleteOrderInfo(Long orderId) {
            log.info("=== 开始查询完整订单信息 ===");
            log.info("层级 1 - OrderService, 数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            // 调用用户服务（会进入第二层）
            if (userService != null) {
                userService.getUserWithProducts(123L);
            }
            
            log.info("返回层级 1 - OrderService, 数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            log.info("=== 完整订单信息查询完成 ===\n");
        }
    }
    
    /**
     * 用户服务示例
     * 使用 slave1 数据源
     */
    @Service
    @Slf4j
    public static class UserService {
        
        @Autowired(required = false)
        private ProductService productService;
        
        /**
         * 获取用户信息
         * 使用 slave1 数据源
         */
        @DS("slave1")
        public void getUserInfo(Long userId) {
            log.info("  层级 2 - UserService, 数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            // 模拟查询用户
            log.info("  从 slave1 数据源查询用户: {}", userId);
        }
        
        /**
         * 获取用户及其产品信息
         * 会调用产品服务，形成三层嵌套
         */
        @DS("slave1")
        public void getUserWithProducts(Long userId) {
            log.info("  层级 2 - UserService, 数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            log.info("  从 slave1 数据源查询用户: {}", userId);
            
            // 调用产品服务（会进入第三层）
            if (productService != null) {
                productService.getUserProducts(userId);
            }
            
            log.info("  返回层级 2 - UserService, 数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
        }
    }
    
    /**
     * 产品服务示例
     * 使用 slave2 数据源
     */
    @Service
    @Slf4j
    public static class ProductService {
        
        /**
         * 获取用户的产品列表
         * 使用 slave2 数据源
         */
        @DS("slave2")
        public void getUserProducts(Long userId) {
            log.info("    层级 3 - ProductService, 数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            // 模拟查询产品
            log.info("    从 slave2 数据源查询用户产品: {}", userId);
        }
    }
    
    /**
     * 演示同一数据源的嵌套调用
     */
    @Service
    @Slf4j
    public static class ReportService {
        
        @DS("master")
        public void generateReport() {
            log.info("=== 开始生成报表 ===");
            log.info("当前数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            // 调用另一个使用相同数据源的方法
            calculateStatistics();
            
            log.info("返回 generateReport，数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            log.info("=== 报表生成完成 ===\n");
        }
        
        @DS("master")
        public void calculateStatistics() {
            log.info("  嵌套调用 calculateStatistics，数据源: {}, 栈深度: {}", 
                DataSourceContextHolder.getDataSourceKey(),
                DataSourceContextHolder.getStackDepth());
            
            // 即使是同一数据源，栈深度也会增加
            log.info("  执行统计计算...");
        }
    }
}
