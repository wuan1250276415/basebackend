package com.basebackend.chat.controller;

import com.basebackend.chat.dto.request.FriendRequestDTO;
import com.basebackend.chat.dto.request.HandleFriendRequestDTO;
import com.basebackend.chat.dto.response.FriendVO;
import com.basebackend.chat.service.ChatFriendService;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 好友控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/friends")
@RequiredArgsConstructor
@Validated
@Tag(name = "好友管理", description = "好友、好友申请、好友分组、黑名单等接口")
public class ChatFriendController {

    private final ChatFriendService friendService;

    // ======================== 好友搜索 ========================

    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "按用户名/手机号/邮箱搜索")
    public Result<List<Map<String, Object>>> searchUsers(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @RequestParam String keyword) {
        return Result.success(friendService.searchUsers(currentUserId, tenantId, keyword));
    }

    // ======================== 好友申请 ========================

    @PostMapping("/request")
    @Operation(summary = "发送好友申请")
    public Result<Map<String, Object>> sendFriendRequest(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @Validated @RequestBody FriendRequestDTO request) {
        var data = friendService.sendFriendRequest(currentUserId, tenantId, request);
        return Result.success("好友申请已发送", data);
    }

    @PutMapping("/request/{requestId}")
    @Operation(summary = "处理好友申请", description = "同意或拒绝")
    public Result<Void> handleFriendRequest(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long requestId,
            @Validated @RequestBody HandleFriendRequestDTO request) {
        friendService.handleFriendRequest(currentUserId, tenantId, requestId, request);
        String msg = "accept".equalsIgnoreCase(request.getAction()) ? "已同意好友申请" : "已拒绝好友申请";
        return Result.success(msg, null);
    }

    @GetMapping("/request/list")
    @Operation(summary = "获取好友申请列表")
    public Result<PageResult<Map<String, Object>>> listFriendRequests(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(friendService.listFriendRequests(currentUserId, tenantId, pageNum, pageSize));
    }

    // ======================== 好友列表 ========================

    @GetMapping
    @Operation(summary = "获取好友列表", description = "可按分组筛选")
    public Result<List<FriendVO>> listFriends(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @RequestParam(required = false) Long groupId) {
        return Result.success(friendService.listFriends(currentUserId, tenantId, groupId));
    }

    @PutMapping("/{friendUserId}/remark")
    @Operation(summary = "修改好友备注")
    public Result<Void> updateRemark(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long friendUserId,
            @RequestBody Map<String, String> body) {
        friendService.updateRemark(currentUserId, tenantId, friendUserId, body.get("remark"));
        return Result.success();
    }

    @DeleteMapping("/{friendUserId}")
    @Operation(summary = "删除好友", description = "单向删除，对方无感知")
    public Result<Void> deleteFriend(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long friendUserId) {
        friendService.deleteFriend(currentUserId, tenantId, friendUserId);
        return Result.success();
    }

    // ======================== 好友分组 ========================

    @GetMapping("/groups")
    @Operation(summary = "获取好友分组列表")
    public Result<List<Map<String, Object>>> listFriendGroups(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId) {
        return Result.success(friendService.listFriendGroups(currentUserId, tenantId));
    }

    @PostMapping("/groups")
    @Operation(summary = "创建好友分组")
    public Result<Map<String, Object>> createFriendGroup(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        return Result.success(friendService.createFriendGroup(currentUserId, tenantId, name, sortOrder));
    }

    @PutMapping("/groups/{groupId}")
    @Operation(summary = "修改好友分组")
    public Result<Void> updateFriendGroup(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        friendService.updateFriendGroup(currentUserId, tenantId, groupId, name, sortOrder);
        return Result.success();
    }

    @DeleteMapping("/groups/{groupId}")
    @Operation(summary = "删除好友分组", description = "好友移至默认组")
    public Result<Void> deleteFriendGroup(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId) {
        friendService.deleteFriendGroup(currentUserId, tenantId, groupId);
        return Result.success();
    }

    @PutMapping("/{friendUserId}/move-group")
    @Operation(summary = "移动好友到指定分组")
    public Result<Void> moveFriendToGroup(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long friendUserId,
            @RequestBody Map<String, Long> body) {
        friendService.moveFriendToGroup(currentUserId, tenantId, friendUserId, body.get("groupId"));
        return Result.success();
    }
}
