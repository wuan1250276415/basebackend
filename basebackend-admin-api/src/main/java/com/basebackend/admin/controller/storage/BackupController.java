package com.basebackend.admin.controller.storage;

import com.basebackend.admin.entity.storage.SysBackupRecord;
import com.basebackend.admin.service.storage.SysBackupService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 备份管理Controller
 *
 * @author BaseBackend
 */
@Slf4j
@RestController
@RequestMapping("/api/storage/backup")
@RequiredArgsConstructor
@Tag(name = "备份管理", description = "数据库备份恢复管理接口")
public class BackupController {

    private final SysBackupService backupService;

    @PostMapping("/full")
    @Operation(summary = "手动触发全量备份")
    public Result<Long> triggerFullBackup() {
        Long backupId = backupService.triggerFullBackup();
        return Result.success("全量备份已触发", backupId);
    }

    @PostMapping("/incremental")
    @Operation(summary = "手动触发增量备份")
    public Result<Long> triggerIncrementalBackup() {
        Long backupId = backupService.triggerIncrementalBackup();
        return Result.success("增量备份已触发", backupId);
    }

    @PostMapping("/restore/{backupId}")
    @Operation(summary = "恢复数据库")
    public Result<Boolean> restoreDatabase(@PathVariable Long backupId) {
        boolean success = backupService.restoreDatabase(backupId);
        return success ?
                Result.success("数据库恢复成功", true) :
                Result.error("数据库恢复失败");
    }

    @GetMapping("/list")
    @Operation(summary = "获取备份列表")
    public Result<List<SysBackupRecord>> listBackups(
            @RequestParam(required = false) String backupType,
            @RequestParam(required = false) String status) {
        List<SysBackupRecord> backups = backupService.listBackups(backupType, status);
        return Result.success(backups);
    }

    @DeleteMapping("/{backupId}")
    @Operation(summary = "删除备份")
    public Result<Boolean> deleteBackup(@PathVariable Long backupId) {
        boolean success = backupService.deleteBackup(backupId);
        return success ?
                Result.success("备份删除成功", true) :
                Result.error("备份删除失败");
    }

    @PostMapping("/clean-expired")
    @Operation(summary = "清理过期备份")
    public Result<Integer> cleanExpiredBackups() {
        int count = backupService.cleanExpiredBackups();
        return Result.success("清理了 " + count + " 个过期备份", count);
    }
}
