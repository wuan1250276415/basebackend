package com.basebackend.database.config;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Seata 数据源代理配置
 *
 * <p>使用 Seata DataSourceProxy 包装 Druid 数据源，实现 AT 模式的分布式事务。
 * 该代理会拦截 JDBC 操作，自动记录 undo_log 用于事务回滚。
 *
 * <p><b>启用条件:</b>
 * <pre>
 * seata.enabled=true
 * </pre>
 *
 * <p><b>代理链顺序:</b>
 * <pre>
 * Application → Seata DataSourceProxy → Druid DataSource → MySQL
 * </pre>
 *
 * <p><b>关键特性:</b>
 * <ul>
 *   <li>自动记录 before-image 和 after-image 到 undo_log 表</li>
 *   <li>支持自动回滚：当全局事务失败时，根据 undo_log 恢复数据</li>
 *   <li>透明化：对 MyBatis Plus 等 ORM 框架完全透明</li>
 *   <li>性能优化：仅在全局事务中启用代理逻辑</li>
 * </ul>
 *
 * <p><b>使用场景:</b>
 * <pre>
 * {@code
 * @Service
 * public class UserService {
 *
 *     @GlobalTransactional(rollbackFor = Exception.class)
 *     public void createUserWithFile(UserDTO dto) {
 *         // 1. 本地数据库操作 (admin-api)
 *         userMapper.insert(user);
 *
 *         // 2. 远程服务调用 (file-service)
 *         fileServiceClient.uploadAvatar(dto.getAvatar());
 *
 *         // 任一步骤失败，Seata 自动回滚所有操作
 *     }
 * }
 * }
 * </pre>
 *
 * <p><b>注意事项:</b>
 * <ul>
 *   <li>必须使用 @Primary 注解，确保 Seata 代理优先于原始数据源</li>
 *   <li>Druid 数据源配置保持不变，从 spring.datasource.druid.* 读取</li>
 *   <li>undo_log 表必须在业务数据库中创建 (通过 Flyway V1.11 迁移)</li>
 *   <li>如果禁用 Seata (seata.enabled=false)，将回退到普通的 Druid 数据源</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2025-10-30
 * @see io.seata.rm.datasource.DataSourceProxy
 * @see io.seata.spring.annotation.GlobalTransactional
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
public class SeataDataSourceConfig {

    /**
     * 创建 Druid 数据源
     *
     * <p>从配置文件读取 Druid 连接池参数：
     * <pre>
     * spring.datasource.druid.url
     * spring.datasource.druid.username
     * spring.datasource.druid.password
     * spring.datasource.druid.initial-size
     * spring.datasource.druid.max-active
     * ... (其他 Druid 配置)
     * </pre>
     *
     * @return Druid 数据源实例
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DruidDataSource druidDataSource() {
        log.info("Creating Druid DataSource for Seata proxy...");
        DruidDataSource dataSource = new DruidDataSource();
        // 设置数据库类型，防止 WallFilter 初始化时 dbType 为 null
        dataSource.setDbType("mysql");
        return dataSource;
    }

    /**
     * 创建 Seata DataSourceProxy (AT 模式代理)
     *
     * <p>包装 Druid 数据源，实现分布式事务的自动补偿功能。
     * 该代理会：
     * <ol>
     *   <li>拦截所有 JDBC 操作 (INSERT, UPDATE, DELETE, SELECT FOR UPDATE)</li>
     *   <li>在 Phase 1 记录 before-image 和 after-image 到 undo_log</li>
     *   <li>在 Phase 2 提交时删除 undo_log</li>
     *   <li>在 Phase 2 回滚时根据 undo_log 恢复数据</li>
     * </ol>
     *
     * <p><b>关键点:</b>
     * <ul>
     *   <li>使用 @Primary 注解：确保 Spring 优先注入 Seata 代理而非原始 Druid 数据源</li>
     *   <li>透明性：MyBatis Plus、JdbcTemplate 等无需修改，自动使用代理</li>
     *   <li>性能：仅在存在全局事务 (XID) 时启用代理逻辑，本地事务不受影响</li>
     * </ul>
     *
     * <p><b>工作流程示例:</b>
     * <pre>
     * 1. 方法添加 @GlobalTransactional 注解
     *    ↓
     * 2. Seata TM 向 TC 申请全局事务 XID
     *    ↓
     * 3. 执行业务方法
     *    ├─ SQL: INSERT INTO user ...
     *    │  ├─ DataSourceProxy 拦截
     *    │  ├─ 记录 before-image (null) 到 undo_log
     *    │  ├─ 执行实际 SQL
     *    │  └─ 记录 after-image (新数据) 到 undo_log
     *    │
     *    └─ Feign 调用其他服务 (XID 通过 HTTP header 传播)
     *    ↓
     * 4. 如果成功：TC 通知所有分支删除 undo_log
     *    如果失败：TC 通知所有分支根据 undo_log 回滚
     * </pre>
     *
     * <p><b>兼容性:</b>
     * <ul>
     *   <li>MyBatis Plus 3.5.5: ✅ 完全兼容</li>
     *   <li>Druid 1.2.20: ✅ 完全兼容</li>
     *   <li>Spring Boot 3.1.5: ✅ 需要 Seata 1.7.0+</li>
     *   <li>ShardingSphere 5.4.1: ✅ 代理顺序：Seata → ShardingSphere → Druid</li>
     * </ul>
     *
     * @param druidDataSource 原始 Druid 数据源
     * @return Seata 数据源代理
     */
    @Bean
    @Primary
    public DataSource dataSource(DruidDataSource druidDataSource) {
        log.info("Wrapping Druid DataSource with Seata DataSourceProxy for AT mode...");
        log.info("Seata AT mode enabled - undo_log will be automatically managed");

        DataSourceProxy dataSourceProxy = new DataSourceProxy(druidDataSource);

        log.info("Seata DataSourceProxy created successfully");
        log.info("  ├─ Resource ID: {}", dataSourceProxy.getResourceId());
        log.info("  ├─ Branch Type: AT");
        log.info("  ├─ Target DataSource: Druid");
        log.info("  └─ Undo Log Table: undo_log");

        return dataSourceProxy;
    }
}
