package com.basebackend.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.basebackend.system.base.BaseServiceTest;
import com.basebackend.system.dto.LoginLogDTO;
import com.basebackend.system.dto.OperationLogDTO;
import com.basebackend.system.entity.SysLoginLog;
import com.basebackend.system.entity.SysOperationLog;
import com.basebackend.system.mapper.SysLoginLogMapper;
import com.basebackend.system.mapper.SysOperationLogMapper;
import com.basebackend.system.service.impl.LogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 日志服务测试
 */
@DisplayName("LogService 日志服务测试")
class LogServiceTest extends BaseServiceTest {

    @Mock
    private SysLoginLogMapper loginLogMapper;

    @Mock
    private SysOperationLogMapper operationLogMapper;

    private LogService logService;

    @BeforeEach
    void setUp() {
        logService = new LogServiceImpl(loginLogMapper, operationLogMapper);
    }

    @Test
    @DisplayName("getLoginLogPage - 应返回登录日志分页列表")
    void shouldReturnLoginLogPage() {
        // Given
        SysLoginLog log1 = createLoginLog(1L, "admin", "127.0.0.1", 1);
        given(loginLogMapper.selectPage(any(), any())).willReturn(createLoginLogPage(Arrays.asList(log1)));

        // When
        var result = logService.getLoginLogPage("admin", "127.0.0.1", 1, null, null, 1, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("admin");
        verify(loginLogMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("getOperationLogPage - 应返回操作日志分页列表")
    void shouldReturnOperationLogPage() {
        // Given
        SysOperationLog log1 = createOperationLog(1L, "admin", "创建用户", 1);
        given(operationLogMapper.selectPage(any(), any())).willReturn(createOperationLogPage(Arrays.asList(log1)));

        // When
        var result = logService.getOperationLogPage("admin", "创建", 1, null, null, 1, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("admin");
        verify(operationLogMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("getLoginLogById - 应返回指定ID的登录日志")
    void shouldReturnLoginLogById() {
        // Given
        SysLoginLog log = createLoginLog(1L, "admin", "127.0.0.1", 1);
        given(loginLogMapper.selectById(1L)).willReturn(log);

        // When
        LoginLogDTO result = logService.getLoginLogById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getIpAddress()).isEqualTo("127.0.0.1");
        verify(loginLogMapper).selectById(1L);
    }

    @Test
    @DisplayName("getOperationLogById - 应返回指定ID的操作日志")
    void shouldReturnOperationLogById() {
        // Given
        SysOperationLog log = createOperationLog(1L, "admin", "创建用户", 1);
        given(operationLogMapper.selectById(1L)).willReturn(log);

        // When
        OperationLogDTO result = logService.getOperationLogById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getOperation()).isEqualTo("创建用户");
        verify(operationLogMapper).selectById(1L);
    }

    @Test
    @DisplayName("deleteLoginLog - 应删除指定登录日志")
    void shouldDeleteLoginLog() {
        // When
        logService.deleteLoginLog(1L);

        // Then
        verify(loginLogMapper).deleteById(1L);
    }

    @Test
    @DisplayName("deleteOperationLog - 应删除指定操作日志")
    void shouldDeleteOperationLog() {
        // When
        logService.deleteOperationLog(1L);

        // Then
        verify(operationLogMapper).deleteById(1L);
    }

    @Test
    @DisplayName("deleteLoginLogBatch - 应批量删除登录日志")
    void shouldDeleteLoginLogBatch() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // When
        logService.deleteLoginLogBatch(ids);

        // Then
        verify(loginLogMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("deleteOperationLogBatch - 应批量删除操作日志")
    void shouldDeleteOperationLogBatch() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // When
        logService.deleteOperationLogBatch(ids);

        // Then
        verify(operationLogMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("cleanLoginLog - 应清空所有登录日志")
    void shouldCleanLoginLog() {
        // When
        logService.cleanLoginLog();

        // Then
        verify(loginLogMapper).delete(null);
    }

    @Test
    @DisplayName("cleanOperationLog - 应清空所有操作日志")
    void shouldCleanOperationLog() {
        // When
        logService.cleanOperationLog();

        // Then
        verify(operationLogMapper).delete(null);
    }

    @Test
    @DisplayName("recordLoginLog - 应记录登录日志")
    void shouldRecordLoginLog() {
        // Given
        LoginLogDTO loginLogDTO = fixtures.createValidLoginLogDTO();

        // When
        logService.recordLoginLog(loginLogDTO);

        // Then
        verify(loginLogMapper).insert(any(SysLoginLog.class));
    }

    @Test
    @DisplayName("recordOperationLog - 应记录操作日志")
    void shouldRecordOperationLog() {
        // Given
        OperationLogDTO operationLogDTO = fixtures.createValidOperationLogDTO();

        // When
        logService.recordOperationLog(operationLogDTO);

        // Then
        verify(operationLogMapper).insert(any(SysOperationLog.class));
    }

    private SysLoginLog createLoginLog(Long id, String username, String ipAddress, Integer status) {
        SysLoginLog log = new SysLoginLog();
        log.setId(id);
        log.setUsername(username);
        log.setIpAddress(ipAddress);
        log.setStatus(status);
        log.setLoginTime(LocalDateTime.now());
        return log;
    }

    private SysOperationLog createOperationLog(Long id, String username, String operation, Integer status) {
        SysOperationLog log = new SysOperationLog();
        log.setId(id);
        log.setUsername(username);
        log.setOperation(operation);
        log.setStatus(status);
        log.setOperationTime(LocalDateTime.now());
        return log;
    }

    private com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysLoginLog> createLoginLogPage(List<SysLoginLog> records) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysLoginLog> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(records.size());
        return page;
    }

    private com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysOperationLog> createOperationLogPage(List<SysOperationLog> records) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysOperationLog> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(records.size());
        return page;
    }
}
