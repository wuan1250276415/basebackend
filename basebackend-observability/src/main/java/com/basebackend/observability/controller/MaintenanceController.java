package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.cleanup.DataCleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 维护控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/maintenance")
@RequiredArgsConstructor
@Tag(name = "系统维护", description = "数据清理和存储管理")
public class MaintenanceController {

    private final DataCleanupService cleanupService;

    @PostMapping("/cleanup")
    @Operation(summary = "手动清理过期数据")
    public Result<DataCleanupService.CleanupResult> manualCleanup(
            @RequestParam(defaultValue = "7") int retentionDays) {
        try {
            DataCleanupService.CleanupResult result = cleanupService.manualCleanup(retentionDays);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to cleanup data", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/storage")
    @Operation(summary = "获取存储统计")
    public Result<DataCleanupService.StorageStatistics> getStorageStatistics() {
        try {
            DataCleanupService.StorageStatistics stats = cleanupService.getStorageStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get storage statistics", e);
            return Result.error(e.getMessage());
        }
    }
}
