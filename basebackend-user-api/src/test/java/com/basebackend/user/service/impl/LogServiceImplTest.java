package com.basebackend.user.service.impl;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.user.mapper.SysLoginLogMapper;
import com.basebackend.user.mapper.SysOperationLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("日志服务测试")
class LogServiceImplTest {

    @Mock
    private SysLoginLogMapper loginLogMapper;

    @Mock
    private SysOperationLogMapper operationLogMapper;

    @InjectMocks
    private LogServiceImpl logService;

    @Test
    @DisplayName("查询登录日志详情 - 日志不存在")
    void shouldThrowBusinessExceptionWhenLoginLogNotFound() {
        when(loginLogMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> logService.getLoginLogById(999L));

        assertEquals("登录日志不存在", exception.getMessage());
    }

    @Test
    @DisplayName("查询操作日志详情 - 日志不存在")
    void shouldThrowBusinessExceptionWhenOperationLogNotFound() {
        when(operationLogMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> logService.getOperationLogById(999L));

        assertEquals("操作日志不存在", exception.getMessage());
    }
}
