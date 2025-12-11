package com.basebackend.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.system.dto.LoginLogDTO;
import com.basebackend.system.dto.OperationLogDTO;
import com.basebackend.system.service.LogService;
import com.basebackend.common.model.Result;
import com.basebackend.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日志管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "日志管理", description = "日志管理相关接口")
public class LogController {

    private static final String ERROR_MESSAGE = "系统繁忙，请稍后再试";
    
    private final LogService logService;

    /**
     * 统一处理控制器异常，避免向客户端泄露敏感信息
     */
    private <T> Result<T> handleControllerError(String action, Exception e) {
        log.error("{}失败", action, e);
        return Result.error(ERROR_MESSAGE);
    }

    /**
     * 分页查询登录日志
     */
    @GetMapping("/login")
    @Operation(summary = "分页查询登录日志", description = "分页查询登录日志")
    public Result<Page<LoginLogDTO>> getLoginLogPage(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "IP地址") @RequestParam(required = false) String ipAddress,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始时间") @RequestParam(required = false) String beginTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        log.info("分页查询登录日志: current={}, size={}", current, size);
        try {
            Page<LoginLogDTO> result = logService.getLoginLogPage(username, ipAddress, status, beginTime, endTime, current, size);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            return handleControllerError("分页查询登录日志", e);
        }
    }

    /**
     * 分页查询操作日志
     */
    @GetMapping("/operation")
    @Operation(summary = "分页查询操作日志", description = "分页查询操作日志")
    public Result<Page<OperationLogDTO>> getOperationLogPage(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "操作") @RequestParam(required = false) String operation,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始时间") @RequestParam(required = false) String beginTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        log.info("分页查询操作日志: current={}, size={}", current, size);
        try {
            Page<OperationLogDTO> result = logService.getOperationLogPage(username, operation, status, beginTime, endTime, current, size);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            return handleControllerError("分页查询操作日志", e);
        }
    }

    /**
     * 根据ID查询登录日志
     */
    @GetMapping("/login/{id}")
    @Operation(summary = "根据ID查询登录日志", description = "根据ID查询登录日志详情")
    public Result<LoginLogDTO> getLoginLogById(@Parameter(description = "日志ID") @PathVariable Long id) {
        log.info("根据ID查询登录日志: {}", id);
        try {
            LoginLogDTO loginLog = logService.getLoginLogById(id);
            return Result.success("查询成功", loginLog);
        } catch (Exception e) {
            return handleControllerError("根据ID查询登录日志", e);
        }
    }

    /**
     * 根据ID查询操作日志
     */
    @GetMapping("/operation/{id}")
    @Operation(summary = "根据ID查询操作日志", description = "根据ID查询操作日志详情")
    public Result<OperationLogDTO> getOperationLogById(@Parameter(description = "日志ID") @PathVariable Long id) {
        log.info("根据ID查询操作日志: {}", id);
        try {
            OperationLogDTO operationLog = logService.getOperationLogById(id);
            return Result.success("查询成功", operationLog);
        } catch (Exception e) {
            return handleControllerError("根据ID查询操作日志", e);
        }
    }

    /**
     * 删除登录日志
     */
    @DeleteMapping("/login/{id}")
    @Operation(summary = "删除登录日志", description = "删除登录日志")
    @RequiresPermission("system:log:delete")
    public Result<String> deleteLoginLog(@Parameter(description = "日志ID") @PathVariable Long id) {
        log.info("删除登录日志: {}", id);
        try {
            logService.deleteLoginLog(id);
            return Result.success("登录日志删除成功");
        } catch (Exception e) {
            return handleControllerError("删除登录日志", e);
        }
    }

    /**
     * 删除操作日志
     */
    @DeleteMapping("/operation/{id}")
    @Operation(summary = "删除操作日志", description = "删除操作日志")
    @RequiresPermission("system:log:delete")
    public Result<String> deleteOperationLog(@Parameter(description = "日志ID") @PathVariable Long id) {
        log.info("删除操作日志: {}", id);
        try {
            logService.deleteOperationLog(id);
            return Result.success("操作日志删除成功");
        } catch (Exception e) {
            return handleControllerError("删除操作日志", e);
        }
    }

    /**
     * 批量删除登录日志
     */
    @DeleteMapping("/login/batch")
    @Operation(summary = "批量删除登录日志", description = "批量删除登录日志")
    @RequiresPermission("system:log:delete")
    public Result<String> deleteLoginLogBatch(@RequestBody List<Long> ids) {
        log.info("批量删除登录日志: {}", ids);
        try {
            logService.deleteLoginLogBatch(ids);
            return Result.success("批量删除登录日志成功");
        } catch (Exception e) {
            return handleControllerError("批量删除登录日志", e);
        }
    }

    /**
     * 批量删除操作日志
     */
    @DeleteMapping("/operation/batch")
    @Operation(summary = "批量删除操作日志", description = "批量删除操作日志")
    @RequiresPermission("system:log:delete")
    public Result<String> deleteOperationLogBatch(@RequestBody List<Long> ids) {
        log.info("批量删除操作日志: {}", ids);
        try {
            logService.deleteOperationLogBatch(ids);
            return Result.success("批量删除操作日志成功");
        } catch (Exception e) {
            return handleControllerError("批量删除操作日志", e);
        }
    }

    /**
     * 清空登录日志
     */
    @DeleteMapping("/login/clean")
    @Operation(summary = "清空登录日志", description = "清空所有登录日志")
    @RequiresPermission("system:log:clean")
    public Result<String> cleanLoginLog() {
        log.info("清空登录日志");
        try {
            logService.cleanLoginLog();
            return Result.success("登录日志清空成功");
        } catch (Exception e) {
            return handleControllerError("清空登录日志", e);
        }
    }

    /**
     * 清空操作日志
     */
    @DeleteMapping("/operation/clean")
    @Operation(summary = "清空操作日志", description = "清空所有操作日志")
    @RequiresPermission("system:log:clean")
    public Result<String> cleanOperationLog() {
        log.info("清空操作日志");
        try {
            logService.cleanOperationLog();
            return Result.success("操作日志清空成功");
        } catch (Exception e) {
            return handleControllerError("清空操作日志", e);
        }
    }
}
