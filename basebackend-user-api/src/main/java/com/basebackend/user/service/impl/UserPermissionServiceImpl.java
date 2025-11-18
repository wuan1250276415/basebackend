package com.basebackend.user.service.impl;

import com.basebackend.cache.service.RedisService;
import com.basebackend.security.service.PermissionService;
import com.basebackend.user.context.UserContextHolder;
import com.basebackend.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户权限服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements PermissionService {

    private final SysUserMapper userMapper;
    private final RedisService redisService;

    private static final String USER_PERMISSIONS_KEY = "user:permissions:";
    private static final String USER_ROLES_KEY = "user:roles:";

    @Override
    public List<String> getCurrentUserPermissions() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }

        // 从Redis缓存获取
        String key = USER_PERMISSIONS_KEY + userId;
        List<String> permissions = redisService.get(key);

        if (permissions == null) {
            // 从数据库查询
            permissions = userMapper.selectUserPermissions(userId);
            // 缓存30分钟
            redisService.set(key, permissions, 1800);
        }

        return permissions != null ? permissions : List.of();
    }

    @Override
    public List<String> getCurrentUserRoles() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }

        // 从Redis缓存获取
        String key = USER_ROLES_KEY + userId;
        List<String> roles = redisService.get(key);

        if (roles == null) {
            // 从数据库查询
            roles = userMapper.selectUserRoles(userId);
            // 缓存30分钟
            redisService.set(key, roles, 1800);
        }

        return roles != null ? roles : List.of();
    }

    @Override
    public Long getCurrentUserId() {
        return UserContextHolder.getUserId();
    }

    @Override
    public Long getCurrentUserDeptId() {
        return UserContextHolder.getDeptId();
    }
}
