package com.basebackend.database.migration.controller;

import com.basebackend.database.migration.model.MigrationBackup;
import com.basebackend.database.migration.service.MigrationBackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 迁移备份管理控制器
 * 
 * @author basebackend
 */
@RestController
@RequestMapping("/api/database/migration/backup")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class MigrationBackupController {

    private final MigrationBackupService backupService;

    /**
     * 创建备份
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/create")
    public ResponseEntity<MigrationBackup> createBackup(
            @RequestParam String migrationVersion,
            @RequestParam(required = false) List<String> tables) {
        MigrationBackup backup = backupService.createBackup(migrationVersion, tables);
        return ResponseEntity.ok(backup);
    }

    /**
     * 获取备份列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<MigrationBackup>> listBackups() {
        List<MigrationBackup> backups = backupService.listBackups();
        return ResponseEntity.ok(backups);
    }

    /**
     * 获取备份详情
     */
    @GetMapping("/{backupId}")
    public ResponseEntity<MigrationBackup> getBackup(@PathVariable String backupId) {
        MigrationBackup backup = backupService.getBackup(backupId);
        return ResponseEntity.ok(backup);
    }

    /**
     * 恢复备份
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/{backupId}/restore")
    public ResponseEntity<Map<String, String>> restoreBackup(@PathVariable String backupId) {
        String result = backupService.restoreBackup(backupId);
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除备份
     * 注意：此接口应该有严格的权限控制
     */
    @DeleteMapping("/{backupId}")
    public ResponseEntity<Map<String, String>> deleteBackup(@PathVariable String backupId) {
        String result = backupService.deleteBackup(backupId);
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }
}
