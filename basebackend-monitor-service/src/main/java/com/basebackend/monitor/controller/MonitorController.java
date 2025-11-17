package com.basebackend.monitor.controller;

import com.basebackend.common.model.Result;
import com.basebackend.monitor.dto.CacheInfoDTO;
import com.basebackend.monitor.dto.OnlineUserDTO;
import com.basebackend.monitor.dto.ServerInfoDTO;
import com.basebackend.monitor.service.MonitorService;
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
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Validated
@Tag(name = "系统监控", description = "系统监控相关接口")
public class MonitorController {

    private final MonitorService monitorService;

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online")
    @Operation(summary = "获取在线用户列表", description = "查询当前系统中的在线用户信息")
    public Result<List<OnlineUserDTO>> getOnlineUsers() {
        log.info("获取在线用户列表");
        try {
            List<OnlineUserDTO> onlineUsers = monitorService.getOnlineUsers();
            return Result.success("查询成功", onlineUsers);
        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 强制用户下线
     */
    @DeleteMapping("/online/{token}")
    @Operation(summary = "强制用户下线", description = "根据 token 强制指定用户下线")
    public Result<String> forceLogout(
            @Parameter(description = "用户令牌") @PathVariable String token) {
        log.info("强制用户下线: token={}", token);
        try {
            monitorService.forceLogout(token);
            return Result.success("用户强制下线成功");
        } catch (Exception e) {
            log.error("强制用户下线失败: token={}", token, e);
            return Result.error("强制下线失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务器信息
     */
    @GetMapping("/server")
    @Operation(summary = "获取服务器信息", description = "获取服务器的基本信息、JVM 信息、内存使用情况等")
    public Result<ServerInfoDTO> getServerInfo() {
        log.info("获取服务器信息");
        try {
            ServerInfoDTO serverInfo = monitorService.getServerInfo();
            return Result.success("查询成功", serverInfo);
        } catch (Exception e) {
            log.error("获取服务器信息失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存信息
     */
    @GetMapping("/cache")
    @Operation(summary = "获取缓存信息", description = "获取系统缓存的统计信息，包括命中率、大小等")
    public Result<List<CacheInfoDTO>> getCacheInfo() {
        log.info("获取缓存信息");
        try {
            List<CacheInfoDTO> cacheInfo = monitorService.getCacheInfo();
            return Result.success("查询成功", cacheInfo);
        } catch (Exception e) {
            log.error("获取缓存信息失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 清空指定缓存
     */
    @DeleteMapping("/cache/{cacheName}")
    @Operation(summary = "清空指定缓存", description = "根据缓存名称清空指定的缓存数据")
    public Result<String> clearCache(
            @Parameter(description = "缓存名称") @PathVariable String cacheName) {
        log.info("清空缓存: cacheName={}", cacheName);
        try {
            monitorService.clearCache(cacheName);
            return Result.success("缓存清空成功");
        } catch (Exception e) {
            log.error("清空缓存失败: cacheName={}", cacheName, e);
            return Result.error("缓存清空失败: " + e.getMessage());
        }
    }

    /**
     * 清空所有缓存
     */
    @DeleteMapping("/cache")
    @Operation(summary = "清空所有缓存", description = "清空系统中的所有缓存数据（危险操作，请谨慎使用）")
    public Result<String> clearAllCache() {
        log.info("清空所有缓存");
        try {
            monitorService.clearAllCache();
            return Result.success("所有缓存清空成功");
        } catch (Exception e) {
            log.error("清空所有缓存失败", e);
            return Result.error("清空所有缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取系统统计信息", description = "获取系统的统计数据，包括在线用户数、内存使用率、缓存命中率等")
    public Result<Object> getSystemStats() {
        log.info("获取系统统计信息");
        try {
            Object stats = monitorService.getSystemStats();
            return Result.success("查询成功", stats);
        } catch (Exception e) {
            log.error("获取系统统计信息失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
