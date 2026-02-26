package com.basebackend.service.client;

import com.basebackend.api.model.log.UserOperationLogDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 操作日志服务客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@HttpExchange("/api/internal/operation-log")
public interface OperationLogServiceClient {

    @GetExchange("/user/{userId}")
    @Operation(summary = "获取用户操作日志")
    Result<List<UserOperationLogDTO>> getUserOperationLogs(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit);

    @PostExchange("/save")
    @Operation(summary = "保存操作日志")
    Result<Void> saveOperationLog(@RequestBody UserOperationLogDTO operationLog);
}
