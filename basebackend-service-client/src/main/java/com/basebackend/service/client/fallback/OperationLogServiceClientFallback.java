package com.basebackend.service.client.fallback;

import com.basebackend.api.model.log.UserOperationLogDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.OperationLogServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 操作日志服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@Component
public class OperationLogServiceClientFallback implements OperationLogServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceClientFallback.class);

    @Override
    public Result<List<UserOperationLogDTO>> getUserOperationLogs(Long userId, Integer limit) {
        log.warn("[服务降级] 获取用户操作日志失败: userId={}, limit={}", userId, limit);
        return Result.success("操作日志服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<Void> saveOperationLog(UserOperationLogDTO operationLog) {
        log.warn("[服务降级] 保存操作日志失败: userId={}", operationLog != null ? operationLog.userId() : null);
        return Result.success();
    }
}
