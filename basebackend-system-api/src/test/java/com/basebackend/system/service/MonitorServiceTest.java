package com.basebackend.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.basebackend.cache.service.RedisService;
import com.basebackend.system.base.BaseServiceTest;
import com.basebackend.system.dto.CacheInfoDTO;
import com.basebackend.system.dto.OnlineUserDTO;
import com.basebackend.system.dto.ServerInfoDTO;
import com.basebackend.system.service.impl.MonitorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 监控服务测试
 */
@DisplayName("MonitorService 监控服务测试")
class MonitorServiceTest extends BaseServiceTest {

    @Mock
    private RedisService redisService;

    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorServiceImpl(redisService);
    }

    @Test
    @DisplayName("getOnlineUsers - 应返回在线用户列表")
    void shouldReturnOnlineUsers() {
        // Given
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", 1L);
        userData.put("username", "admin");
        userData.put("nickname", "管理员");
        userData.put("deptName", "技术部");
        userData.put("loginIp", "127.0.0.1");
        userData.put("loginLocation", "本地");
        userData.put("browser", "Chrome");
        userData.put("os", "Windows 10");
        userData.put("token", "token123");
        userData.put("loginTime", "2025-01-01T10:00:00");
        userData.put("lastAccessTime", "2025-01-01T10:30:00");

        Set<String> keys = new HashSet<>();
        keys.add("online_users:user1");
        when(redisService.keys(anyString())).thenReturn(keys);
        when(redisService.get(anyString())).thenReturn(userData);

        // When
        List<OnlineUserDTO> result = monitorService.getOnlineUsers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("admin");
        verify(redisService).keys("online_users:*");
        verify(redisService).get("online_users:user1");
    }

    @Test
    @DisplayName("forceLogout - 应强制下线指定用户")
    void shouldForceLogout() {
        // Given
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", 1L);
        userData.put("username", "admin");
        userData.put("token", "token123");

        Set<String> keys = new HashSet<>();
        keys.add("online_users:user1");
        when(redisService.keys(anyString())).thenReturn(keys);
        when(redisService.get(anyString())).thenReturn(userData);

        // When
        monitorService.forceLogout("token123");

        // Then
        verify(redisService).delete("online_users:user1");
        verify(redisService).delete("login_tokens:admin");
    }

    @Test
    @DisplayName("getServerInfo - 应返回服务器信息")
    void shouldReturnServerInfo() {
        // When
        ServerInfoDTO result = monitorService.getServerInfo();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.serverName()).isNotNull();
        assertThat(result.osName()).isNotNull();
            assertThat(result.javaVersion()).isNotNull();
        assertThat(result.totalMemory()).isNotNull();
        assertThat(result.processorCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("getCacheInfo - 应返回缓存信息列表")
    void shouldReturnCacheInfo() {
        // When
        List<CacheInfoDTO> result = monitorService.getCacheInfo();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSizeGreaterThan(0);
        assertThat(result.get(0).cacheName()).isNotNull();
        assertThat(result.get(0).cacheType()).isEqualTo("Redis");
    }

    @Test
    @DisplayName("clearCache - 应清空指定缓存")
    void shouldClearCache() {
        when(redisService.deleteByPattern("user:permissions:*")).thenReturn(2L);

        // When
        monitorService.clearCache("user_permissions");

        // Then
        verify(redisService).deleteByPattern("user:permissions:*");
    }

    @Test
    @DisplayName("clearAllCache - 应清空所有缓存")
    void shouldClearAllCache() {
        when(redisService.deleteByPattern(anyString())).thenReturn(1L);

        // When
        monitorService.clearAllCache();

        // Then
        verify(redisService).deleteByPattern("sys:dict:*");
        verify(redisService).deleteByPattern("online_users:*");
        verify(redisService).deleteByPattern("login_tokens:*");
        verify(redisService).deleteByPattern("user:permissions:*");
    }

    @Test
    @DisplayName("getSystemStats - 应返回系统统计信息")
    void shouldReturnSystemStats() {
        // When
        Object result = monitorService.getSystemStats();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result;
        assertThat(stats).containsKey("memory");
        assertThat(stats).containsKey("system");
        assertThat(stats).containsKey("threads");
        assertThat(stats).containsKey("onlineUsers");
    }
}
