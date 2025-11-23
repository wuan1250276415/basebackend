package com.basebackend.notification.config;

import com.basebackend.common.context.UserContextHolder;
import com.basebackend.security.service.PermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * 提供最小实现的 PermissionService，避免 DataScopeAspect 缺失依赖。
 * 只从 UserContextHolder 读取当前用户信息。
 */
@Configuration
public class NotificationPermissionConfig {

    @Bean
    public PermissionService permissionService() {
        return new PermissionService() {
            @Override
            public List<String> getCurrentUserPermissions() {
                return Collections.emptyList();
            }

            @Override
            public List<String> getCurrentUserRoles() {
                return Collections.emptyList();
            }

            @Override
            public Long getCurrentUserId() {
                return UserContextHolder.getUserId();
            }

            @Override
            public Long getCurrentUserDeptId() {
                return null;
            }
        };
    }
}
