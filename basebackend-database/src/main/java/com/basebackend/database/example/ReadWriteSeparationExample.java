package com.basebackend.database.example;

import com.basebackend.database.routing.MasterOnly;
import com.basebackend.database.routing.ReadOnly;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 读写分离使用示例
 *
 * 本示例展示了如何在业务代码中正确使用读写分离功能
 *
 * @author 浮浮酱
 */
@Slf4j
@Service
public class ReadWriteSeparationExample {

    // ==================== 示例 1: 基本查询操作 ====================

    /**
     * 示例 1.1: 使用 @ReadOnly 注解标记查询方法
     *
     * 推荐：明确标注查询方法使用从库
     */
    @ReadOnly
    public User getUserById(Long id) {
        log.info("从从库查询用户: {}", id);
        // return userMapper.selectById(id);
        return new User();  // 示例代码
    }

    /**
     * 示例 1.2: 不使用注解，依赖方法名自动路由
     *
     * 方法名以 get/find/select/query 等开头，自动路由到从库
     */
    public List<User> findActiveUsers() {
        log.info("方法名自动路由到从库");
        // return userMapper.selectActiveUsers();
        return List.of();  // 示例代码
    }

    // ==================== 示例 2: 写操作 ====================

    /**
     * 示例 2.1: 事务操作自动使用主库
     *
     * 带有 @Transactional 的方法自动使用主库
     */
    @Transactional
    public void createUser(User user) {
        log.info("事务操作，自动使用主库");
        // userMapper.insert(user);
    }

    /**
     * 示例 2.2: 复杂事务操作
     *
     * 事务中的所有操作都在主库执行
     */
    @Transactional
    public void updateUserAndCreateLog(User user) {
        log.info("事务中的所有操作都在主库执行");
        // userMapper.updateById(user);
        // logMapper.insert(log);
    }

    // ==================== 示例 3: 强制使用主库 ====================

    /**
     * 示例 3.1: 实时性要求高的查询
     *
     * 使用 @MasterOnly 强制从主库读取，避免主从延迟问题
     */
    @MasterOnly
    public User getUserAfterCreate(Long id) {
        log.info("强制从主库查询，确保读取到最新数据");
        // return userMapper.selectById(id);
        return new User();  // 示例代码
    }

    /**
     * 示例 3.2: 写后立即读场景
     *
     * 先写入主库，然后立即从主库读取验证
     */
    public User createUserAndVerify(User user) {
        // 写入主库（方法名不是查询前缀，自动使用主库）
        // userMapper.insert(user);
        log.info("写入主库");

        // 立即读取验证，必须从主库读取
        return getUserAfterCreate(user.getId());  // 使用 @MasterOnly 方法
    }

    // ==================== 示例 4: 类级别注解 ====================

    /**
     * 示例 4: 整个类都使用从库的报表服务
     */
    @ReadOnly
    @Service
    public static class ReportService {

        /**
         * 日报查询 - 继承类级别 @ReadOnly，使用从库
         */
        public List<Report> getDailyReport() {
            log.info("报表查询使用从库");
            return List.of();
        }

        /**
         * 月报查询 - 继承类级别 @ReadOnly，使用从库
         */
        public List<Report> getMonthlyReport() {
            log.info("报表查询使用从库");
            return List.of();
        }

        /**
         * 实时报表 - 方法级别 @MasterOnly 覆盖类级别注解
         */
        @MasterOnly
        public Report getRealtimeReport() {
            log.info("实时报表强制使用主库");
            return new Report();
        }
    }

    // ==================== 示例 5: 常见错误示范 ====================

    /**
     * ❌ 错误示例 5.1: 事务中使用 @ReadOnly（注解会被忽略）
     *
     * 问题：@Transactional 优先级更高，会强制使用主库
     * 建议：事务方法不要使用 @ReadOnly
     */
    @Transactional
    public void wrongExample1() {
        // 这个操作会在主库执行
        // userMapper.insert(new User());

        // ❌ 错误：这个 @ReadOnly 会被忽略，仍然使用主库
        getUserById(1L);  // 虽然方法有 @ReadOnly，但在事务中会被忽略
    }

    /**
     * ❌ 错误示例 5.2: 写后立即读使用从库
     *
     * 问题：主从延迟可能导致读不到刚写入的数据
     * 建议：写后立即读必须使用 @MasterOnly
     */
    public void wrongExample2(User user) {
        // 写入主库
        // userMapper.insert(user);

        // ❌ 错误：从库可能还没同步，读取可能返回 null
        User result = getUserById(user.getId());  // 可能读不到！

        // ✅ 正确做法：
        // User result = getUserAfterCreate(user.getId());  // 使用 @MasterOnly
    }

    /**
     * ❌ 错误示例 5.3: 过度使用 @MasterOnly
     *
     * 问题：普通查询没必要用主库，增加主库压力
     * 建议：只在必要时使用 @MasterOnly
     */
    @MasterOnly
    public List<User> wrongExample3() {
        log.warn("普通列表查询不应该使用 @MasterOnly");
        // return userMapper.selectList(null);
        return List.of();
    }

    // ==================== 示例 6: 最佳实践 ====================

    /**
     * ✅ 最佳实践 6.1: 创建并返回
     *
     * 在同一个事务中完成创建和查询，确保数据一致性
     */
    @Transactional
    public User bestPractice1(User user) {
        log.info("最佳实践：在事务中完成创建和查询");
        // userMapper.insert(user);
        // 事务中查询自动使用主库，无需额外注解
        // return userMapper.selectById(user.getId());
        return user;
    }

    /**
     * ✅ 最佳实践 6.2: 分离读写操作
     *
     * 查询和写入分别用不同的方法，明确数据源
     */
    @ReadOnly
    public List<User> bestPractice2_query() {
        log.info("查询操作使用从库");
        // return userMapper.selectList(null);
        return List.of();
    }

    @Transactional
    public void bestPractice2_write(User user) {
        log.info("写操作使用主库");
        // userMapper.insert(user);
    }

    /**
     * ✅ 最佳实践 6.3: 明确标注方法意图
     *
     * 即使方法名会自动路由，也建议显式使用注解
     */
    @ReadOnly  // 明确标注，提高可读性
    public User bestPractice3_getUserInfo(Long id) {
        log.info("明确标注使用从库，提高代码可读性");
        // return userMapper.selectById(id);
        return new User();
    }

    /**
     * ✅ 最佳实践 6.4: 复杂业务场景
     *
     * 根据业务需求灵活选择数据源
     */
    public void bestPractice4_complexScenario(Long userId) {
        // 1. 从库查询用户信息（对实时性要求不高）
        User user = getUserById(userId);

        // 2. 从主库查询账户余额（实时性要求高）
        Account account = getAccountFromMaster(user.getAccountId());

        // 3. 在事务中执行业务逻辑
        if (account.getBalance() > 100) {
            updateAccountBalance(account);
        }
    }

    @MasterOnly
    private Account getAccountFromMaster(Long accountId) {
        log.info("账户余额查询必须从主库读取");
        return new Account();
    }

    @Transactional
    protected void updateAccountBalance(Account account) {
        log.info("更新账户余额");
        // accountMapper.updateById(account);
    }

    // ==================== 示例数据类 ====================

    /**
     * 示例用户类
     */
    public static class User {
        private Long id;
        private String username;
        private Long accountId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
    }

    /**
     * 示例报表类
     */
    public static class Report {
        private String name;
        private String data;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    /**
     * 示例账户类
     */
    public static class Account {
        private Long id;
        private Double balance;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Double getBalance() { return balance; }
        public void setBalance(Double balance) { this.balance = balance; }
    }

    /**
     * 示例操作日志类
     */
    public static class OperationLog {
        private Long id;
        private String operation;
        private Long userId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String operation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}
