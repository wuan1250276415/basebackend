package com.basebackend.security.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DynamicPermissionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DynamicPermissionService 单元测试")
class DynamicPermissionServiceTest {

    @Mock
    private PermissionDataSource mockDataSource;

    private DynamicPermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new DynamicPermissionService(mockDataSource);
    }

    @Test
    @DisplayName("hasPermission - 用户拥有指定权限")
    void testHasPermission_UserHasPermission() {
        // 准备
        Long userId = 100L;
        Set<String> permissions = new HashSet<>();
        permissions.add("system:user:view");
        permissions.add("system:user:edit");
        when(mockDataSource.isAvailable()).thenReturn(true);
        when(mockDataSource.loadPermissions(userId)).thenReturn(permissions);

        // 执行
        boolean result = permissionService.hasPermission(userId, "system:user:view");

        // 验证
        assertTrue(result);
        verify(mockDataSource).loadPermissions(userId);
    }

    @Test
    @DisplayName("hasPermission - 用户没有指定权限")
    void testHasPermission_UserLacksPermission() {
        // 准备
        Long userId = 100L;
        Set<String> permissions = new HashSet<>();
        permissions.add("system:user:view");
        when(mockDataSource.isAvailable()).thenReturn(true);
        when(mockDataSource.loadPermissions(userId)).thenReturn(permissions);

        // 执行
        boolean result = permissionService.hasPermission(userId, "system:user:delete");

        // 验证
        assertFalse(result);
    }

    @Test
    @DisplayName("hasPermission - 超级管理员权限")
    void testHasPermission_SuperAdmin() {
        // 准备
        Long userId = 1L;
        Set<String> permissions = new HashSet<>();
        permissions.add("*:*");
        when(mockDataSource.isAvailable()).thenReturn(true);
        when(mockDataSource.loadPermissions(userId)).thenReturn(permissions);

        // 执行
        boolean result = permissionService.hasPermission(userId, "any:permission:here");

        // 验证
        assertTrue(result);
    }

    @Test
    @DisplayName("hasPermission - userId为null")
    void testHasPermission_NullUserId() {
        // 执行
        boolean result = permissionService.hasPermission(null, "system:user:view");

        // 验证
        assertFalse(result);
        verify(mockDataSource, never()).loadPermissions(any());
    }

    @Test
    @DisplayName("hasPermission - permission为null")
    void testHasPermission_NullPermission() {
        // 执行
        boolean result = permissionService.hasPermission(100L, null);

        // 验证
        assertFalse(result);
    }

    @Test
    @DisplayName("getUserPermissions - 缓存命中")
    void testGetUserPermissions_CacheHit() {
        // 准备
        Long userId = 100L;
        Set<String> permissions = new HashSet<>();
        permissions.add("system:user:view");
        when(mockDataSource.isAvailable()).thenReturn(true);
        when(mockDataSource.loadPermissions(userId)).thenReturn(permissions);

        // 第一次调用
        Set<String> result1 = permissionService.getUserPermissions(userId);
        // 第二次调用（应该命中缓存）
        Set<String> result2 = permissionService.getUserPermissions(userId);

        // 验证：数据源只被调用一次
        verify(mockDataSource, times(1)).loadPermissions(userId);
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("refreshUserPermissions - 刷新后重新加载")
    void testRefreshUserPermissions() {
        // 准备
        Long userId = 100L;
        Set<String> permissions = new HashSet<>();
        permissions.add("system:user:view");
        when(mockDataSource.isAvailable()).thenReturn(true);
        when(mockDataSource.loadPermissions(userId)).thenReturn(permissions);

        // 第一次调用
        permissionService.getUserPermissions(userId);
        // 刷新缓存
        permissionService.refreshUserPermissions(userId);
        // 再次调用
        permissionService.getUserPermissions(userId);

        // 验证：数据源被调用两次
        verify(mockDataSource, times(2)).loadPermissions(userId);
    }

    @Test
    @DisplayName("refreshAllPermissions - 清空所有缓存")
    void testRefreshAllPermissions() {
        // 准备
        when(mockDataSource.isAvailable()).thenReturn(true);
        when(mockDataSource.loadPermissions(anyLong())).thenReturn(Collections.emptySet());

        // 加载多个用户的权限
        permissionService.getUserPermissions(1L);
        permissionService.getUserPermissions(2L);
        permissionService.getUserPermissions(3L);

        // 刷新所有缓存
        permissionService.refreshAllPermissions();

        // 再次加载
        permissionService.getUserPermissions(1L);
        permissionService.getUserPermissions(2L);

        // 验证：每个用户被调用两次
        verify(mockDataSource, times(2)).loadPermissions(1L);
        verify(mockDataSource, times(2)).loadPermissions(2L);
    }

    @Test
    @DisplayName("权限变更监听器通知")
    void testPermissionChangeListener() {
        // 准备
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        AtomicBoolean allChangedCalled = new AtomicBoolean(false);

        permissionService.addListener(new DynamicPermissionService.PermissionChangeListener() {
            @Override
            public void onPermissionChanged(Long userId) {
                listenerCalled.set(true);
            }

            @Override
            public void onAllPermissionsChanged() {
                allChangedCalled.set(true);
            }
        });

        // 执行
        permissionService.refreshUserPermissions(100L);
        assertTrue(listenerCalled.get());

        permissionService.refreshAllPermissions();
        assertTrue(allChangedCalled.get());
    }

    @Test
    @DisplayName("数据源不可用时返回空权限")
    void testDataSourceUnavailable() {
        // 准备
        when(mockDataSource.isAvailable()).thenReturn(false);

        // 执行
        Set<String> permissions = permissionService.getUserPermissions(100L);

        // 验证
        assertTrue(permissions.isEmpty());
        verify(mockDataSource, never()).loadPermissions(any());
    }

    @Test
    @DisplayName("数据源异常时返回空权限")
    void testDataSourceException() {
        // 准备
        when(mockDataSource.isAvailable()).thenReturn(true);
        when(mockDataSource.loadPermissions(anyLong())).thenThrow(new RuntimeException("DB error"));

        // 执行
        Set<String> permissions = permissionService.getUserPermissions(100L);

        // 验证
        assertTrue(permissions.isEmpty());
    }

    @Test
    @DisplayName("getUserRoles - 正常加载角色")
    void testGetUserRoles() {
        // 准备
        Long userId = 100L;
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        roles.add("ROLE_ADMIN");
        when(mockDataSource.loadRoles(userId)).thenReturn(roles);

        // 执行
        Set<String> result = permissionService.getUserRoles(userId);

        // 验证
        assertEquals(2, result.size());
        assertTrue(result.contains("ROLE_USER"));
        assertTrue(result.contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("使用默认数据源")
    void testDefaultDataSource() {
        // 使用null创建服务，应该使用默认数据源
        DynamicPermissionService serviceWithDefault = new DynamicPermissionService(null);

        // 执行 - 默认数据源对userId=1返回超级管理员权限
        boolean result = serviceWithDefault.hasPermission(1L, "any:permission");

        // 验证
        assertTrue(result);
    }
}
