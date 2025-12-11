package com.basebackend.system.controller.internal;

import com.basebackend.common.model.Result;
import com.basebackend.feign.dto.log.UserOperationLogDTO;
import com.basebackend.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志内部API控制器
 * <p>
 * 供其他微服务通过Feign调用
 *
 * @author BaseBackend
 * @since 2025-12-11
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/operation-log")
@RequiredArgsConstructor
public class OperationLogInternalController {

    private final OperationLogService operationLogService;

    /**
     * 获取用户操作日志列表
     *
     * @param userId 用户ID
     * @param limit  返回记录数量限制
     * @return 操作日志列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<UserOperationLogDTO>> getUserOperationLogs(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        log.debug("内部API - 获取用户操作日志: userId={}, limit={}", userId, limit);
        List<UserOperationLogDTO> logs = operationLogService.getUserOperationLogs(userId, limit);
        return Result.success(logs);
    }

    /**
     * 保存操作日志
     *
     * @param operationLog 操作日志信息
     * @return 操作结果
     */
    @PostMapping("/save")
    public Result<Void> saveOperationLog(@RequestBody UserOperationLogDTO operationLog) {
        log.debug("内部API - 保存操作日志: userId={}, operationType={}",
                operationLog.getUserId(), operationLog.getOperationType());
        operationLogService.saveOperationLog(operationLog);
        return Result.success();
    }
}
