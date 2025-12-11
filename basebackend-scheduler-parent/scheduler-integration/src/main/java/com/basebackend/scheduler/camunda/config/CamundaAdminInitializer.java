package com.basebackend.scheduler.camunda.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camunda 管理员用户初始化器
 *
 * <p>在应用启动时自动创建默认管理员用户和授权，确保 Camunda Web UI 可访问。
 * 支持幂等操作，重复启动不会创建重复用户。
 *
 * <p>功能特性：
 * <ul>
 *   <li>自动创建管理员用户（从配置文件读取用户信息）</li>
 *   <li>授予管理员所有资源的完整权限</li>
 *   <li>检查用户是否已存在，避免重复创建</li>
 *   <li>使用事务确保原子性操作</li>
 *   <li>完善的异常处理和日志记录</li>
 * </ul>
 *
 * <p>安全建议：
 * <ul>
 *   <li>生产环境应通过 Nacos 配置中心管理敏感信息</li>
 *   <li>避免在代码或配置文件中硬编码密码</li>
 *   <li>定期更新管理员密码</li>
 *   <li>限制管理员账号的使用场景</li>
 * </ul>
 *
 * <p>注意：使用 CommandLineRunner 替代 @PostConstruct，确保事务正常生效。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@Order(1) // 确保在其他初始化器之前执行
@RequiredArgsConstructor
public class CamundaAdminInitializer implements CommandLineRunner {

    private final IdentityService identityService;
    private final AuthorizationService authorizationService;

    /**
     * 管理员用户 ID
     * 默认值：admin
     */
    @Value("${camunda.bpm.admin-user.id:admin}")
    private String adminUserId;

    /**
     * 管理员密码
     * 注意：生产环境应通过配置中心加密存储
     * 默认值：admin123（仅用于开发测试）
     */
    @Value("${camunda.bpm.admin-user.password:#{null}}")
    private String adminPassword;

    /**
     * 管理员名字
     * 默认值：Scheduler
     */
    @Value("${camunda.bpm.admin-user.first-name:Scheduler}")
    private String adminFirstName;

    /**
     * 管理员姓氏
     * 默认值：Admin
     */
    @Value("${camunda.bpm.admin-user.last-name:Admin}")
    private String adminLastName;

    /**
     * 管理员邮箱
     * 默认值：admin@basebackend.com
     */
    @Value("${camunda.bpm.admin-user.email:admin@basebackend.com}")
    private String adminEmail;

    /**
     * 应用启动时初始化管理员用户
     *
     * @param args 命令行参数
     */
    @Override
    public void run(String... args) {
        initializeAdminUser();
    }

    /**
     * 初始化管理员用户和授权
     *
     * <p>执行步骤：
     * <ol>
     *   <li>检查管理员密码是否已配置</li>
     *   <li>检查管理员用户是否已存在</li>
     *   <li>如不存在，创建管理员用户</li>
     *   <li>检查是否已有全局管理员授权</li>
     *   <li>如无授权，创建全部资源的管理员授权</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public void initializeAdminUser() {
        try {
            log.info("Starting Camunda admin user initialization...");

            // 检查密码是否配置
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("Camunda admin password not configured. Using default password for development only!");
                adminPassword = "admin123";
            }

            // 1. 创建或更新管理员用户
            createOrUpdateAdminUser();

            // 2. 授予全部资源的管理员权限
            grantAdminAuthorizations();

            log.info("Camunda admin user initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Camunda admin user/authorization: {}", e.getMessage(), e);
            // 不抛出异常，避免影响应用启动
            // 管理员可以后续手动创建用户
        }
    }

    /**
     * 创建或更新管理员用户
     */
    private void createOrUpdateAdminUser() {
        User existingUser = identityService.createUserQuery()
                .userId(adminUserId)
                .singleResult();

        if (existingUser == null) {
            User newUser = identityService.newUser(adminUserId);
            newUser.setFirstName(adminFirstName);
            newUser.setLastName(adminLastName);
            newUser.setPassword(adminPassword);
            newUser.setEmail(adminEmail);

            identityService.saveUser(newUser);
            log.info("Camunda admin user created: userId={}, email={}", adminUserId, adminEmail);
        } else {
            log.info("Camunda admin user already exists: userId={}", adminUserId);
        }
    }

    /**
     * 授予管理员全部资源的权限
     *
     * <p>包括：APPLICATION、PROCESS_DEFINITION、PROCESS_INSTANCE、TASK、
     * AUTHORIZATION、USER、GROUP、DEPLOYMENT 等所有资源类型。
     */
    private void grantAdminAuthorizations() {
        // 定义需要授权的所有资源类型
        Resources[] resourcesToAuthorize = {
                Resources.APPLICATION,
                Resources.PROCESS_DEFINITION,
                Resources.PROCESS_INSTANCE,
                Resources.TASK,
                Resources.AUTHORIZATION,
                Resources.USER,
                Resources.GROUP,
                Resources.DEPLOYMENT,
                Resources.FILTER,
                Resources.DECISION_DEFINITION,
                Resources.DECISION_REQUIREMENTS_DEFINITION,
                Resources.BATCH,
                Resources.TENANT
        };

        for (Resources resource : resourcesToAuthorize) {
            if (!hasAuthorization(resource)) {
                createAuthorization(resource);
            }
        }
    }

    /**
     * 检查是否已有指定资源的授权
     */
    private boolean hasAuthorization(Resources resource) {
        long count = authorizationService.createAuthorizationQuery()
                .userIdIn(adminUserId)
                .resourceType(resource)
                .resourceId(Authorization.ANY)
                .count();
        return count > 0;
    }

    /**
     * 创建指定资源的授权
     */
    private void createAuthorization(Resources resource) {
        try {
            Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            auth.setUserId(adminUserId);
            auth.setResource(resource);
            auth.setResourceId(Authorization.ANY);
            auth.addPermission(Permissions.ALL);

            authorizationService.saveAuthorization(auth);
            log.debug("Authorization granted: userId={}, resource={}", adminUserId, resource.resourceName());
        } catch (Exception e) {
            log.warn("Failed to grant authorization for resource {}: {}", resource.resourceName(), e.getMessage());
        }
    }
}
