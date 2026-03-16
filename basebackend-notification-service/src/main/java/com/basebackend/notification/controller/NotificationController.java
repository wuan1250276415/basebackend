package com.basebackend.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.model.Result;
import com.basebackend.notification.dto.CreateNotificationDTO;
import com.basebackend.notification.dto.NotificationQueryDTO;
import com.basebackend.notification.dto.UserNotificationDTO;
import com.basebackend.notification.service.NotificationService;
import com.basebackend.notification.service.SSENotificationService;
import com.basebackend.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知管理控制器
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
@Tag(name = "通知管理", description = "用户通知相关接口")
@Validated
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SSENotificationService sseNotificationService;

    @Operation(summary = "获取通知列表", description = "获取当前用户的通知列表，最多返回 200 条")
    @GetMapping
    public Result<List<UserNotificationDTO>> getNotifications(
            @RequestParam(required = false, defaultValue = "50") @Min(1) @Max(200) Integer limit) {
        return Result.success(notificationService.getCurrentUserNotifications(limit));
    }

    @Operation(summary = "获取未读数量", description = "获取当前用户未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        return Result.success(notificationService.getUnreadCount());
    }

    @Operation(summary = "标记已读", description = "标记指定通知为已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    @Operation(summary = "批量标记已读", description = "批量标记通知为已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestBody List<Long> ids) {
        notificationService.markAllAsRead(ids);
        return Result.success();
    }

    @Operation(summary = "删除通知", description = "删除指定通知")
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return Result.success();
    }

    @Operation(summary = "创建通知（管理员）", description = "创建系统通知")
    @PostMapping
    @RequiresPermission("notification:create")
    public Result<Void> createNotification(@Valid @RequestBody CreateNotificationDTO dto) {
        notificationService.createSystemNotification(dto);
        return Result.success();
    }

    @Operation(summary = "分页查询通知", description = "分页查询当前用户通知列表（支持筛选）")
    @GetMapping("/list")
    public Result<Map<String, Object>> getNotificationPage(NotificationQueryDTO queryDTO) {
        Page<UserNotificationDTO> page = notificationService.getNotificationPage(queryDTO);

        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("page", page.getCurrent());
        result.put("pageSize", page.getSize());

        return Result.success(result);
    }

    @Operation(summary = "批量删除通知", description = "批量删除通知")
    @DeleteMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        notificationService.batchDeleteNotifications(ids);
        return Result.success();
    }

    // -------------------------------------------------------------------------
    // SSE（B2：短命令牌交换机制，避免 JWT 出现在 URL 中）
    // -------------------------------------------------------------------------

    /**
     * 申请 SSE 连接令牌。
     * 客户端通过标准 Authorization 头携带 JWT 调用本接口，换取一个 30 秒有效的一次性令牌，
     * 再用该令牌建立 SSE 连接，避免 JWT 暴露在 URL 访问日志中。
     */
    @Operation(summary = "申请 SSE 连接令牌", description = "返回 30 秒有效的一次性连接令牌，用于建立 SSE 连接")
    @PostMapping("/stream-token")
    public Result<String> getStreamToken() {
        Long userId = UserContextHolder.getUserId();
        log.info("[SSE] 用户申请连接令牌: userId=***{}", Math.abs(userId % 10000));
        String token = sseNotificationService.generateStreamToken(userId);
        return Result.success(token);
    }

    /**
     * 建立 SSE 连接。
     * 使用 {@link #getStreamToken()} 返回的短命令牌（非 JWT），30 秒内有效，一次性消费。
     */
    @Operation(summary = "SSE 连接", description = "凭一次性连接令牌建立 SSE 实时推送连接")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String token) {
        Long userId = sseNotificationService.validateAndConsumeStreamToken(token);
        log.info("[SSE] 用户建立连接: userId=***{}", Math.abs(userId % 10000));
        return sseNotificationService.createConnection(userId);
    }

    @Operation(summary = "SSE 连接统计（管理员）", description = "获取 SSE 连接统计信息")
    @GetMapping("/stream/stats")
    @RequiresPermission("notification:admin")
    public Result<Map<String, Object>> getStreamStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("currentConnections", sseNotificationService.getConnectionCount());
        stats.put("totalConnections",   sseNotificationService.getTotalConnectionCount());
        stats.put("pushSuccessCount",   sseNotificationService.getPushSuccessCount());
        stats.put("pushFailureCount",   sseNotificationService.getPushFailureCount());
        return Result.success(stats);
    }
}
