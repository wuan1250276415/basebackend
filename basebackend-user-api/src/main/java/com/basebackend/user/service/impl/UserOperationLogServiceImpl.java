package com.basebackend.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.basebackend.logging.model.OperationLogInfo;
import com.basebackend.logging.service.OperationLogService;
import com.basebackend.user.entity.SysOperationLog;
import com.basebackend.user.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 用户服务操作日志实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    @Async
    @Override
    public void saveOperationLog(OperationLogInfo logInfo) {
        try {
            SysOperationLog operationLog = new SysOperationLog();
            BeanUtil.copyProperties(logInfo, operationLog);
            operationLogMapper.insert(operationLog);
            log.debug("操作日志保存成功: {}", logInfo.getOperation());
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }
}
