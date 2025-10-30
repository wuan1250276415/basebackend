package com.basebackend.admin.controller;

import com.basebackend.admin.dto.notification.CreateNotificationDTO;
import com.basebackend.admin.dto.notification.UserNotificationDTO;
import com.basebackend.admin.service.NotificationService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知管理控制器
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
@Tag(name = "通知管理", description = "用户通知相关接口")
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取通知列表", description = "获取当前用户的通知列表")
    @GetMapping
    public Result<List<UserNotificationDTO>> getNotifications(
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        List<UserNotificationDTO> notifications = notificationService.getCurrentUserNotifications(limit);
        return Result.success(notifications);
    }

    @Operation(summary = "获取未读数量", description = "获取当前用户未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long count = notificationService.getUnreadCount();
        return Result.success(count);
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

    @Operation(summary = "创建通知", description = "创建系统通知（管理员）")
    @PostMapping
    public Result<Void> createNotification(@Valid @RequestBody CreateNotificationDTO dto) {
        notificationService.createSystemNotification(dto);
        return Result.success();
    }
}
