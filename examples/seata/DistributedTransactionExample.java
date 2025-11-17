package com.basebackend.examples.seata;

import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Seata 分布式事务场景示例
 * 包含多个业务场景的分布式事务实现
 */
@Slf4j
@Service
public class DistributedTransactionExample {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private RoleServiceClient roleServiceClient;

    // ========================================
    // 场景1: 用户创建 + 角色分配
    // ========================================

    /**
     * 场景1: 创建用户并分配角色
     * 这是一个典型的跨服务分布式事务场景
     */
    @GlobalTransactional(name = "create-user-and-assign-role", timeoutMills = 300000)
    public UserDTO createUserWithRole(UserCreateRequest request) {
        log.info("开始执行分布式事务: 创建用户并分配角色");

        try {
            // Step 1: 在用户服务中创建用户
            log.info("Step 1: 创建用户 - {}", request.getUsername());
            User user = userService.createUser(request);
            log.info("用户创建成功: ID={}", user.getId());

            // Step 2: 调用角色服务分配角色
            if (request.getRoleCode() != null) {
                log.info("Step 2: 分配角色 - {}", request.getRoleCode());
                roleServiceClient.assignRoleToUser(user.getId(), request.getRoleCode());
                log.info("角色分配成功");
            }

            // Step 3: 可选操作 - 发送欢迎邮件（异步，不阻塞事务）
            CompletableFuture.runAsync(() -> {
                try {
                    sendWelcomeEmail(user);
                } catch (Exception e) {
                    log.error("发送欢迎邮件失败", e);
                    // 邮件发送失败不应影响主事务
                }
            });

            log.info("分布式事务执行成功: 创建用户并分配角色");
            return userService.convertToDTO(user);

        } catch (Exception e) {
            log.error("分布式事务执行失败: {}", e.getMessage(), e);
            // Seata 会自动回滚所有分支事务
            throw e;
        }
    }

    // ========================================
    // 场景2: 权限变更 + 缓存刷新
    // ========================================

    /**
     * 场景2: 更新权限并刷新缓存
     * 涉及数据库修改和缓存一致性
     */
    @GlobalTransactional(name = "update-permission-and-refresh-cache")
    public void updatePermissionWithCacheRefresh(PermissionUpdateRequest request) {
        log.info("开始执行分布式事务: 更新权限并刷新缓存");

        try {
            // Step 1: 更新权限信息
            log.info("Step 1: 更新权限 - ID={}", request.getId());
            Permission permission = permissionService.updatePermission(request);
            log.info("权限更新成功");

            // Step 2: 刷新相关缓存
            log.info("Step 2: 刷新缓存");
            cacheService.evictPermissionCache(request.getId());
            cacheService.evictUserPermissionCache();
            log.info("缓存刷新成功");

            // Step 3: 记录操作日志
            logService.saveOperationLog(
                "permission_update",
                permission.getId(),
                "更新权限: " + permission.getName()
            );
            log.info("操作日志记录成功");

            log.info("分布式事务执行成功: 更新权限并刷新缓存");

        } catch (Exception e) {
            log.error("分布式事务执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ========================================
    // 场景3: 复杂业务流转
    // ========================================

    /**
     * 场景3: 跨多个服务的复杂业务流程
     * 涉及用户服务、认证服务、权限服务等
     */
    @GlobalTransactional(
        name = "user-onboarding-process",
        timeoutMills = 600000, // 10分钟超时
        rollbackFor = Exception.class
    )
    public UserOnboardingResult userOnboardingProcess(UserOnboardingRequest request) {
        log.info("开始执行分布式事务: 用户入职流程");

        try {
            UserDTO newUser = null;

            // Step 1: 创建用户账号
            log.info("Step 1: 创建用户账号");
            newUser = userService.createUser(request.getUserInfo());
            log.info("用户账号创建成功: ID={}", newUser.getId());

            // Step 2: 创建认证信息
            log.info("Step 2: 创建认证信息");
            authService.createAuthInfo(newUser.getId(), request.getAuthInfo());
            log.info("认证信息创建成功");

            // Step 3: 分配初始权限
            log.info("Step 3: 分配初始权限");
            if (request.getInitialRoleCodes() != null && !request.getInitialRoleCodes().isEmpty()) {
                for (String roleCode : request.getInitialRoleCodes()) {
                    roleServiceClient.assignRoleToUser(newUser.getId(), roleCode);
                }
                log.info("初始权限分配成功: {} 个角色", request.getInitialRoleCodes().size());
            }

            // Step 4: 创建用户配置
            log.info("Step 4: 创建用户配置");
            userProfileService.createDefaultProfile(newUser.getId(), request.getProfileInfo());
            log.info("用户配置创建成功");

            // Step 5: 发送欢迎邮件
            log.info("Step 5: 发送欢迎邮件");
            notificationService.sendWelcomeEmail(newUser.getEmail(), newUser.getUsername());
            log.info("欢迎邮件发送成功");

            // Step 6: 创建操作日志
            log.info("Step 6: 记录操作日志");
            operationLogService.saveLog(
                "user_onboarding",
                newUser.getId(),
                "用户入职流程完成"
            );
            log.info("操作日志记录成功");

            log.info("分布式事务执行成功: 用户入职流程");

            return UserOnboardingResult.builder()
                .userId(newUser.getId())
                .username(newUser.getUsername())
                .status("SUCCESS")
                .message("用户入职流程完成")
                .build();

        } catch (Exception e) {
            log.error("分布式事务执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ========================================
    // 场景4: 批量操作
    // ========================================

    /**
     * 场景4: 批量用户权限更新
     * 确保所有用户权限要么全部更新成功，要么全部失败
     */
    @GlobalTransactional(
        name = "batch-update-user-permissions",
        timeoutMills = 900000 // 15分钟超时
    )
    public BatchOperationResult batchUpdateUserPermissions(BatchPermissionUpdateRequest request) {
        log.info("开始执行分布式事务: 批量更新用户权限 ({} 个用户)", request.getUserIds().size());

        try {
            int successCount = 0;
            int failureCount = 0;

            // 逐个处理每个用户
            for (Long userId : request.getUserIds()) {
                try {
                    // 更新用户权限
                    permissionService.updateUserPermissions(
                        userId,
                        request.getRoleCodes(),
                        request.getPermissionCodes()
                    );

                    // 刷新用户缓存
                    cacheService.evictUserCache(userId);

                    // 记录成功日志
                    logService.saveOperationLog(
                        "batch_permission_update",
                        userId,
                        String.format("批量权限更新 - 角色: %s, 权限: %s",
                            request.getRoleCodes().toString(),
                            request.getPermissionCodes().toString())
                    );

                    successCount++;
                    log.debug("用户权限更新成功: {}", userId);

                } catch (Exception e) {
                    failureCount++;
                    log.error("用户权限更新失败: {}", userId, e);
                    // 单个用户失败不应该影响整个批次
                    // 但可以在最后返回详细的失败信息
                }
            }

            log.info("分布式事务执行完成: 成功={}, 失败={}", successCount, failureCount);

            return BatchOperationResult.builder()
                .totalCount(request.getUserIds().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .status(failureCount == 0 ? "ALL_SUCCESS" : "PARTIAL_SUCCESS")
                .build();

        } catch (Exception e) {
            log.error("分布式事务执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ========================================
    // 场景5: 事务补偿
    // ========================================

    /**
     * 场景5: 带有补偿逻辑的分布式事务
     * 当主事务失败时，执行补偿操作
     */
    @GlobalTransactional(
        name = "create-user-with-compensation",
        timeoutMills = 300000,
        noRollbackFor = {BusinessException.class} // 业务异常不回滚
    )
    public UserDTO createUserWithCompensation(UserCreateRequest request) {
        log.info("开始执行分布式事务: 创建用户（含补偿逻辑）");

        CompensationAction compensationAction = new CompensationAction();

        try {
            // Step 1: 创建用户
            log.info("Step 1: 创建用户");
            User user = userService.createUser(request);
            compensationAction.setUserId(user.getId());
            compensationAction.setUsername(request.getUsername());
            log.info("用户创建成功: ID={}", user.getId());

            // Step 2: 分配角色（可选）
            if (request.getRoleCode() != null) {
                log.info("Step 2: 分配角色");
                roleServiceClient.assignRoleToUser(user.getId(), request.getRoleCode());
                compensationAction.setRoleAssigned(true);
                log.info("角色分配成功");
            }

            // Step 3: 创建用户配置
            log.info("Step 3: 创建用户配置");
            userProfileService.createDefaultProfile(user.getId(), null);
            compensationAction.setProfileCreated(true);
            log.info("用户配置创建成功");

            // Step 4: 模拟可能失败的操作
            if (request.isSimulateFailure()) {
                log.warn("模拟业务失败");
                throw new RuntimeException("模拟的业务失败");
            }

            log.info("分布式事务执行成功");

            return userService.convertToDTO(user);

        } catch (Exception e) {
            log.error("分布式事务执行失败: {}, 开始执行补偿操作", e.getMessage(), e);

            try {
                // 执行补偿操作
                executeCompensation(compensationAction);
                log.info("补偿操作执行成功");
            } catch (Exception compensationException) {
                log.error("补偿操作执行失败: {}", compensationException.getMessage(), compensationException);
                // 补偿失败需要记录，后续人工处理
                log.error("需要人工处理补偿失败: userId={}, username={}",
                    compensationAction.getUserId(),
                    compensationAction.getUsername());
            }

            throw e;
        }
    }

    // ========================================
    // 辅助方法
    // ========================================

    /**
     * 执行事务补偿
     */
    private void executeCompensation(CompensationAction action) {
        log.info("执行事务补偿: userId={}", action.getUserId());

        if (action.getUserId() != null) {
            // 删除用户配置
            if (action.isProfileCreated()) {
                try {
                    userProfileService.deleteByUserId(action.getUserId());
                    log.info("补偿: 删除用户配置");
                } catch (Exception e) {
                    log.error("补偿失败: 删除用户配置", e);
                }
            }

            // 删除用户
            try {
                userService.deleteById(action.getUserId());
                log.info("补偿: 删除用户");
            } catch (Exception e) {
                log.error("补偿失败: 删除用户", e);
            }
        }
    }

    /**
     * 发送欢迎邮件
     */
    private void sendWelcomeEmail(User user) {
        log.info("发送欢迎邮件给: {}", user.getEmail());
        // 实际邮件发送逻辑
    }

    // ========================================
    // 辅助类定义
    // ========================================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CompensationAction {
        private Long userId;
        private String username;
        private boolean roleAssigned = false;
        private boolean profileCreated = false;
    }

    // 注入其他服务 (实际项目中通过 @Autowired 注入)
    private UserService userService;
    private RoleService roleService;
    private PermissionService permissionService;
    private CacheService cacheService;
    private LogService logService;
    private AuthService authService;
    private UserProfileService userProfileService;
    private NotificationService notificationService;
    private OperationLogService operationLogService;

    // Feign 客户端
    private UserServiceClient userServiceClient;
    private RoleServiceClient roleServiceClient;

    // 下面是这些服务的接口定义（实际项目中应该是独立的类）

    public interface UserService {
        User createUser(UserCreateRequest request);
        void deleteById(Long id);
        UserDTO convertToDTO(User user);
    }

    public interface RoleService {
        void assignRoleToUser(Long userId, String roleCode);
    }

    public interface PermissionService {
        Permission updatePermission(PermissionUpdateRequest request);
        void updateUserPermissions(Long userId, java.util.List<String> roleCodes, java.util.List<String> permissionCodes);
    }

    public interface CacheService {
        void evictPermissionCache(Long id);
        void evictUserPermissionCache();
        void evictUserCache(Long userId);
    }

    public interface LogService {
        void saveOperationLog(String operation, Long targetId, String description);
    }

    public interface AuthService {
        void createAuthInfo(Long userId, AuthInfo authInfo);
    }

    public interface UserProfileService {
        void createDefaultProfile(Long userId, ProfileInfo profileInfo);
        void deleteByUserId(Long userId);
    }

    public interface NotificationService {
        void sendWelcomeEmail(String email, String username);
    }

    public interface OperationLogService {
        void saveLog(String operation, Long userId, String description);
    }

    public interface UserServiceClient {
        void assignRoleToUser(Long userId, String roleCode);
    }

    public interface RoleServiceClient {
        void assignRoleToUser(Long userId, String roleCode);
    }

    // DTO 定义
    public static class UserDTO { /* ... */ }
    public static class UserCreateRequest { /* ... */ }
    public static class User { /* ... */ }
    public static class PermissionUpdateRequest { /* ... */ }
    public static class Permission { /* ... */ }
    public static class UserOnboardingRequest { /* ... */ }
    public static class UserOnboardingResult { /* ... */ }
    public static class BatchPermissionUpdateRequest { /* ... */ }
    public static class BatchOperationResult { /* ... */ }
    public static class AuthInfo { /* ... */ }
    public static class ProfileInfo { /* ... */ }
    public static class BusinessException extends RuntimeException { /* ... */ }
}
