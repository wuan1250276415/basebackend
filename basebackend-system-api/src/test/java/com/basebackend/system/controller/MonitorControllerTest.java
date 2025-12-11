package com.basebackend.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basebackend.system.base.BaseWebMvcTest;
import com.basebackend.system.service.MonitorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * 监控控制器测试
 */
@DisplayName("MonitorController 监控控制器测试")
@WebMvcTest(controllers = MonitorController.class)
class MonitorControllerTest extends BaseWebMvcTest {

    @MockBean
    private MonitorService monitorService;

    @Test
    @DisplayName("GET /api/system/monitor/online - 应返回在线用户列表")
    void shouldReturnOnlineUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/monitor/online"))
            .andExpect(status().isOk());

        verify(monitorService).getOnlineUsers();
    }

    @Test
    @DisplayName("DELETE /api/system/monitor/online/{token} - 应强制下线指定用户")
    void shouldForceLogout() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/monitor/online/token123"))
            .andExpect(status().isOk());

        verify(monitorService).forceLogout("token123");
    }

    @Test
    @DisplayName("GET /api/system/monitor/server - 应返回服务器信息")
    void shouldReturnServerInfo() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/monitor/server"))
            .andExpect(status().isOk());

        verify(monitorService).getServerInfo();
    }

    @Test
    @DisplayName("GET /api/system/monitor/cache - 应返回缓存信息列表")
    void shouldReturnCacheInfo() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/monitor/cache"))
            .andExpect(status().isOk());

        verify(monitorService).getCacheInfo();
    }

    @Test
    @DisplayName("DELETE /api/system/monitor/cache/{cacheName} - 应清空指定缓存")
    void shouldClearCache() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/monitor/cache/user_permissions"))
            .andExpect(status().isOk());

        verify(monitorService).clearCache("user_permissions");
    }

    @Test
    @DisplayName("DELETE /api/system/monitor/cache - 应清空所有缓存")
    void shouldClearAllCache() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/monitor/cache"))
            .andExpect(status().isOk());

        verify(monitorService).clearAllCache();
    }

    @Test
    @DisplayName("GET /api/system/monitor/stats - 应返回系统统计信息")
    void shouldReturnSystemStats() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/monitor/stats"))
            .andExpect(status().isOk());

        verify(monitorService).getSystemStats();
    }
}
