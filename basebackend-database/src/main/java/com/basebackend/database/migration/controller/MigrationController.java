package com.basebackend.database.migration.controller;

import com.basebackend.database.migration.model.MigrationConfirmation;
import com.basebackend.database.migration.model.MigrationInfo;
import com.basebackend.database.migration.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库迁移管理控制器
 * 
 * @author basebackend
 */
@RestController
@RequestMapping("/api/database/migration")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class MigrationController {

    private final MigrationService migrationService;

    /**
     * 获取迁移信息摘要
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        String info = migrationService.info();
        String currentVersion = migrationService.getCurrentVersion();
        
        Map<String, Object> result = new HashMap<>();
        result.put("info", info);
        result.put("currentVersion", currentVersion);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取迁移历史
     */
    @GetMapping("/history")
    public ResponseEntity<List<MigrationInfo>> getMigrationHistory() {
        List<MigrationInfo> history = migrationService.getMigrationHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * 获取待执行的迁移
     */
    @GetMapping("/pending")
    public ResponseEntity<List<MigrationInfo>> getPendingMigrations() {
        List<MigrationInfo> pending = migrationService.getPendingMigrations();
        return ResponseEntity.ok(pending);
    }

    /**
     * 获取当前数据库版本
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getCurrentVersion() {
        String version = migrationService.getCurrentVersion();
        Map<String, String> result = new HashMap<>();
        result.put("version", version);
        return ResponseEntity.ok(result);
    }

    /**
     * 执行数据库迁移
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/migrate")
    public ResponseEntity<Map<String, String>> migrate() {
        String result = migrationService.migrate();
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 验证迁移脚本
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, String>> validate() {
        String result = migrationService.validate();
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 修复迁移记录
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/repair")
    public ResponseEntity<Map<String, String>> repair() {
        String result = migrationService.repair();
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 基线化数据库
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/baseline")
    public ResponseEntity<Map<String, String>> baseline(@RequestParam(required = false) String version) {
        String result = migrationService.baseline(version);
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 执行数据库迁移（带备份）
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/migrate-with-backup")
    public ResponseEntity<Map<String, String>> migrateWithBackup(
            @RequestParam(defaultValue = "true") boolean createBackup) {
        String result = migrationService.migrateWithBackup(createBackup);
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 执行数据库迁移（生产环境，需要确认）
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/migrate-with-confirmation")
    public ResponseEntity<Map<String, String>> migrateWithConfirmation(
            @RequestBody MigrationConfirmation confirmation) {
        String result = migrationService.migrateWithConfirmation(confirmation);
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 回滚到指定版本
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/rollback")
    public ResponseEntity<Map<String, String>> rollback(@RequestParam String targetVersion) {
        String result = migrationService.rollbackToVersion(targetVersion);
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 生成确认令牌（用于生产环境迁移）
     * 注意：此接口应该有严格的权限控制
     */
    @PostMapping("/generate-token")
    public ResponseEntity<Map<String, String>> generateToken() {
        String token = migrationService.generateConfirmationToken();
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("validityMinutes", "30");
        return ResponseEntity.ok(response);
    }

    /**
     * 验证确认令牌
     */
    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        boolean valid = migrationService.validateConfirmationToken(token);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        return ResponseEntity.ok(response);
    }
}
