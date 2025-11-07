package com.basebackend.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.notification.CreateNotificationDTO;
import com.basebackend.admin.dto.notification.NotificationQueryDTO;
import com.basebackend.admin.dto.notification.UserNotificationDTO;
import com.basebackend.admin.service.NotificationService;
import com.basebackend.admin.service.SSENotificationService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SSENotificationService sseNotificationService;

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

    @Operation(summary = "SSE 连接", description = "建立 SSE 连接以接收实时通知推送")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String token) {
        // 从 token 中获取用户信息（简化处理，实际应该验证 token）
        // 这里假设已经通过 Security 认证，直接从 SecurityContext 获取用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("未登录或登录已过期");
        }

        // 这里简化处理，实际应该从 token 解析用户ID
        // 临时方案：从当前登录用户获取
        Long userId = getCurrentUserId();

        log.info("[SSE] 用户请求建立连接: userId={}", userId);

        return sseNotificationService.createConnection(userId);
    }

    /**
     * 获取当前登录用户ID
     * TODO: 这个方法重复了，应该抽取到工具类
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 简化处理，实际需要查询数据库
        // 这里暂时返回一个模拟的用户ID
        return 1L; // TODO: 实现真实的用户ID获取逻辑
    }
}
