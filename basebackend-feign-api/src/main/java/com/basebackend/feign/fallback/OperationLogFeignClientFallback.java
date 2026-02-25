package com.basebackend.feign.fallback;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.OperationLogFeignClient;
import com.basebackend.feign.dto.log.UserOperationLogDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 操作日志Feign客户端熔断降级处理
 *
 * @author BaseBackend
 * @since 2025-12-11
 */
@Component
public class OperationLogFeignClientFallback implements OperationLogFeignClient {

    private static final Logger log = LoggerFactory.getLogger(OperationLogFeignClientFallback.class);

    @Override
    public Result<List<UserOperationLogDTO>> getUserOperationLogs(Long userId, Integer limit) {
        log.warn("获取用户操作日志失败，触发熔断降级: userId={}, limit={}", userId, limit);
        return Result.success(Collections.emptyList());
    }

    @Override
    public Result<Void> saveOperationLog(UserOperationLogDTO operationLog) {
        log.warn("保存操作日志失败，触发熔断降级: operationType={}",
                operationLog != null ? operationLog.operationType() : "null");
        return Result.success();
    }
}
