package com.basebackend.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basebackend.system.base.BaseWebMvcTest;
import com.basebackend.system.service.LogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.Arrays;

/**
 * 日志控制器测试
 */
@DisplayName("LogController 日志控制器测试")
@WebMvcTest(controllers = LogController.class)
class LogControllerTest extends BaseWebMvcTest {

    @MockBean
    private LogService logService;

    @Test
    @DisplayName("GET /api/system/logs/login - 应返回登录日志分页列表")
    void shouldReturnLoginLogPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/logs/login")
                .param("current", "1")
                .param("size", "10"))
            .andExpect(status().isOk());

        verify(logService).getLoginLogPage(null, null, null, null, null, 1, 10);
    }

    @Test
    @DisplayName("GET /api/system/logs/operation - 应返回操作日志分页列表")
    void shouldReturnOperationLogPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/logs/operation")
                .param("current", "1")
                .param("size", "10"))
            .andExpect(status().isOk());

        verify(logService).getOperationLogPage(null, null, null, null, null, 1, 10);
    }

    @Test
    @DisplayName("GET /api/system/logs/login/{id} - 应返回指定ID的登录日志")
    void shouldReturnLoginLogById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/logs/login/1"))
            .andExpect(status().isOk());

        verify(logService).getLoginLogById(1L);
    }

    @Test
    @DisplayName("GET /api/system/logs/operation/{id} - 应返回指定ID的操作日志")
    void shouldReturnOperationLogById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/logs/operation/1"))
            .andExpect(status().isOk());

        verify(logService).getOperationLogById(1L);
    }

    @Test
    @DisplayName("DELETE /api/system/logs/login/{id} - 应删除指定登录日志")
    void shouldDeleteLoginLog() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/logs/login/1"))
            .andExpect(status().isOk());

        verify(logService).deleteLoginLog(1L);
    }

    @Test
    @DisplayName("DELETE /api/system/logs/operation/{id} - 应删除指定操作日志")
    void shouldDeleteOperationLog() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/logs/operation/1"))
            .andExpect(status().isOk());

        verify(logService).deleteOperationLog(1L);
    }

    @Test
    @DisplayName("DELETE /api/system/logs/login/batch - 应批量删除登录日志")
    void shouldDeleteLoginLogBatch() throws Exception {
        // Given
        String requestBody = "[1, 2, 3]";

        // When & Then
        mockMvc.perform(delete("/api/system/logs/login/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        verify(logService).deleteLoginLogBatch(Arrays.asList(1L, 2L, 3L));
    }

    @Test
    @DisplayName("DELETE /api/system/logs/operation/batch - 应批量删除操作日志")
    void shouldDeleteOperationLogBatch() throws Exception {
        // Given
        String requestBody = "[1, 2, 3]";

        // When & Then
        mockMvc.perform(delete("/api/system/logs/operation/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        verify(logService).deleteOperationLogBatch(Arrays.asList(1L, 2L, 3L));
    }

    @Test
    @DisplayName("DELETE /api/system/logs/login/clean - 应清空所有登录日志")
    void shouldCleanLoginLog() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/logs/login/clean"))
            .andExpect(status().isOk());

        verify(logService).cleanLoginLog();
    }

    @Test
    @DisplayName("DELETE /api/system/logs/operation/clean - 应清空所有操作日志")
    void shouldCleanOperationLog() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/logs/operation/clean"))
            .andExpect(status().isOk());

        verify(logService).cleanOperationLog();
    }
}
