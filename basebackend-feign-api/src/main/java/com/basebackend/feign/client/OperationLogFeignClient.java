package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import com.basebackend.feign.constant.FeignServiceConstants;
import com.basebackend.feign.dto.log.UserOperationLogDTO;
import com.basebackend.feign.fallback.OperationLogFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志Feign客户端
 * <p>
 * 用于user-api调用system-api的操作日志服务
 *
 * @author BaseBackend
 * @since 2025-12-11
 */
@FeignClient(name = FeignServiceConstants.SYS_SERVICE, contextId = "operationLogFeignClient", path = "/api/internal/operation-log", fallback = OperationLogFeignClientFallback.class)
public interface OperationLogFeignClient {

    /**
     * 获取用户操作日志列表
     *
     * @param userId 用户ID
     * @param limit  返回记录数量限制
     * @return 操作日志列表
     */
    @GetMapping("/user/{userId}")
    Result<List<UserOperationLogDTO>> getUserOperationLogs(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit);

    /**
     * 保存操作日志
     *
     * @param operationLog 操作日志信息
     * @return 操作结果
     */
    @PostMapping("/save")
    Result<Void> saveOperationLog(@RequestBody UserOperationLogDTO operationLog);
}
