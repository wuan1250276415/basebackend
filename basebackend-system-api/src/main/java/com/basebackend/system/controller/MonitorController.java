package com.basebackend.system.controller;

import com.basebackend.system.dto.CacheInfoDTO;
import com.basebackend.system.dto.OnlineUserDTO;
import com.basebackend.system.dto.ServerInfoDTO;
import com.basebackend.system.service.MonitorService;
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
 * 系统监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system/monitor")
@RequiredArgsConstructor
@Validated
@Tag(name = "系统监控", description = "系统监控相关接口")
public class MonitorController {

    private final MonitorService monitorService;

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online")
    @Operation(summary = "获取在线用户", description = "获取在线用户列表")
//    @RequiresPermission("system:monitor:online")
    public Result<List<OnlineUserDTO>> getOnlineUsers() {
        log.info("获取在线用户列表");
        try {
            List<OnlineUserDTO> onlineUsers = monitorService.getOnlineUsers();
            return Result.success("查询成功", onlineUsers);
        } catch (Exception e) {
            log.error("获取在线用户列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 强制下线用户
     */
    @DeleteMapping("/online/{token}")
    @Operation(summary = "强制下线用户", description = "强制下线指定用户")
//    @RequiresPermission("system:monitor:forceLogout")
    public Result<String> forceLogout(@Parameter(description = "用户Token") @PathVariable String token) {
        log.info("强制下线用户: {}", token);
        try {
            monitorService.forceLogout(token);
            return Result.success("用户强制下线成功");
        } catch (Exception e) {
            log.error("强制下线用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取服务器信息
     */
    @GetMapping("/server")
    @Operation(summary = "获取服务器信息", description = "获取服务器详细信息")
//    @RequiresPermission("system:monitor:server")
    public Result<ServerInfoDTO> getServerInfo() {
        log.info("获取服务器信息");
        try {
            ServerInfoDTO serverInfo = monitorService.getServerInfo();
            return Result.success("查询成功", serverInfo);
        } catch (Exception e) {
            log.error("获取服务器信息失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取缓存信息
     */
    @GetMapping("/cache")
    @Operation(summary = "获取缓存信息", description = "获取缓存详细信息")
//    @RequiresPermission("system:monitor:cache")
    public Result<List<CacheInfoDTO>> getCacheInfo() {
        log.info("获取缓存信息");
        try {
            List<CacheInfoDTO> cacheInfo = monitorService.getCacheInfo();
            return Result.success("查询成功", cacheInfo);
        } catch (Exception e) {
            log.error("获取缓存信息失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 清空指定缓存
     */
    @DeleteMapping("/cache/{cacheName}")
    @Operation(summary = "清空指定缓存", description = "清空指定名称的缓存")
//    @RequiresPermission("system:monitor:cacheClean")
    public Result<String> clearCache(@Parameter(description = "缓存名称") @PathVariable String cacheName) {
        log.info("清空指定缓存: {}", cacheName);
        try {
            monitorService.clearCache(cacheName);
            return Result.success("缓存清空成功");
        } catch (Exception e) {
            log.error("清空指定缓存失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 清空所有缓存
     */
    @DeleteMapping("/cache")
    @Operation(summary = "清空所有缓存", description = "清空所有缓存")
//    @RequiresPermission("system:monitor:cacheClean")
    public Result<String> clearAllCache() {
        log.info("清空所有缓存");
        try {
            monitorService.clearAllCache();
            return Result.success("所有缓存清空成功");
        } catch (Exception e) {
            log.error("清空所有缓存失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取系统统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取系统统计信息", description = "获取系统统计信息")
//    @RequiresPermission("system:monitor:stats")
    public Result<Object> getSystemStats() {
        log.info("获取系统统计信息");
        try {
            Object stats = monitorService.getSystemStats();
            return Result.success("查询成功", stats);
        } catch (Exception e) {
            log.error("获取系统统计信息失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
