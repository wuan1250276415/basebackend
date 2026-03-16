package com.basebackend.chat.controller;

import com.basebackend.chat.service.ChatFriendService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 黑名单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/blacklist")
@RequiredArgsConstructor
@Tag(name = "黑名单", description = "拉黑、取消拉黑、黑名单列表")
public class ChatBlacklistController {

    private final ChatFriendService friendService;

    @PostMapping
    @Operation(summary = "拉黑用户")
    public Result<Void> blockUser(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @RequestBody Map<String, Object> body) {
        Long blockedId = ((Number) body.get("blockedId")).longValue();
        String reason = (String) body.get("reason");
        friendService.blockUser(currentUserId, tenantId, blockedId, reason);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "取消拉黑")
    public Result<Void> unblockUser(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long userId) {
        friendService.unblockUser(currentUserId, tenantId, userId);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "获取黑名单列表")
    public Result<List<Map<String, Object>>> listBlacklist(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId) {
        return Result.success(friendService.listBlacklist(currentUserId, tenantId));
    }
}
