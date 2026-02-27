package com.basebackend.chat.controller;

import com.basebackend.chat.dto.request.CreateGroupRequest;
import com.basebackend.chat.dto.request.UpdateGroupRequest;
import com.basebackend.chat.dto.response.GroupMemberVO;
import com.basebackend.chat.dto.response.GroupVO;
import com.basebackend.chat.service.ChatGroupService;
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
 * 群组控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/groups")
@RequiredArgsConstructor
@Validated
@Tag(name = "群组管理", description = "群创建、解散、成员管理、公告等接口")
public class ChatGroupController {

    private final ChatGroupService groupService;

    @PostMapping
    @Operation(summary = "创建群")
    public Result<Map<String, Object>> createGroup(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @Validated @RequestBody CreateGroupRequest request) {
        var data = groupService.createGroup(currentUserId, tenantId, request);
        return Result.success("群创建成功", data);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "获取群信息")
    public Result<GroupVO> getGroupInfo(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId) {
        return Result.success(groupService.getGroupInfo(currentUserId, tenantId, groupId));
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "修改群信息", description = "群主/管理员可操作")
    public Result<Void> updateGroup(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @RequestBody UpdateGroupRequest request) {
        groupService.updateGroup(currentUserId, tenantId, groupId, request);
        return Result.success();
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "解散群", description = "仅群主可操作")
    public Result<Void> dissolveGroup(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId) {
        groupService.dissolveGroup(currentUserId, tenantId, groupId);
        return Result.success();
    }

    // ======================== 群成员管理 ========================

    @GetMapping("/{groupId}/members")
    @Operation(summary = "获取群成员列表")
    public Result<List<GroupMemberVO>> listMembers(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId) {
        return Result.success(groupService.listMembers(currentUserId, tenantId, groupId));
    }

    @PostMapping("/{groupId}/members")
    @Operation(summary = "邀请入群")
    @SuppressWarnings("unchecked")
    public Result<Void> inviteMembers(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @RequestBody Map<String, List<Long>> body) {
        groupService.inviteMembers(currentUserId, tenantId, groupId, body.get("userIds"));
        return Result.success();
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @Operation(summary = "踢出成员", description = "群主/管理员可操作")
    public Result<Void> kickMember(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        groupService.kickMember(currentUserId, tenantId, groupId, userId);
        return Result.success();
    }

    @PostMapping("/{groupId}/leave")
    @Operation(summary = "退出群聊")
    public Result<Void> leaveGroup(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId) {
        groupService.leaveGroup(currentUserId, tenantId, groupId);
        return Result.success();
    }

    @PutMapping("/{groupId}/members/{userId}/role")
    @Operation(summary = "设置成员角色")
    public Result<Void> setMemberRole(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {
        groupService.setMemberRole(currentUserId, tenantId, groupId, userId, body.get("role"));
        return Result.success();
    }

    @PutMapping("/{groupId}/members/{userId}/mute")
    @Operation(summary = "禁言/解禁成员")
    public Result<Void> muteMember(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        boolean isMuted = Boolean.TRUE.equals(body.get("isMuted"));
        Integer duration = body.get("duration") != null ? ((Number) body.get("duration")).intValue() : null;
        groupService.muteMember(currentUserId, tenantId, groupId, userId, isMuted, duration);
        return Result.success();
    }

    @PutMapping("/{groupId}/mute-all")
    @Operation(summary = "全体禁言/解禁")
    public Result<Void> muteAll(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @RequestBody Map<String, Boolean> body) {
        groupService.muteAll(currentUserId, tenantId, groupId, Boolean.TRUE.equals(body.get("isMuted")));
        return Result.success();
    }

    @PutMapping("/{groupId}/nickname")
    @Operation(summary = "修改群内昵称")
    public Result<Void> updateNickname(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @RequestBody Map<String, String> body) {
        groupService.updateNickname(currentUserId, tenantId, groupId, body.get("nickname"));
        return Result.success();
    }

    @PutMapping("/{groupId}/transfer")
    @Operation(summary = "转让群主")
    public Result<Void> transferOwner(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @RequestBody Map<String, Long> body) {
        groupService.transferOwner(currentUserId, tenantId, groupId, body.get("newOwnerId"));
        return Result.success();
    }

    // ======================== 群公告 ========================

    @GetMapping("/{groupId}/announcements")
    @Operation(summary = "获取群公告列表")
    public Result<List<Map<String, Object>>> listAnnouncements(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId) {
        return Result.success(groupService.listAnnouncements(currentUserId, tenantId, groupId));
    }

    @PostMapping("/{groupId}/announcements")
    @Operation(summary = "发布群公告")
    public Result<Map<String, Object>> createAnnouncement(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Boolean isPinned = (Boolean) body.get("isPinned");
        return Result.success(groupService.createAnnouncement(currentUserId, tenantId, groupId,
                title, content, isPinned));
    }

    @PutMapping("/{groupId}/announcements/{id}")
    @Operation(summary = "编辑群公告")
    public Result<Void> updateAnnouncement(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Boolean isPinned = (Boolean) body.get("isPinned");
        groupService.updateAnnouncement(currentUserId, tenantId, groupId, id, title, content, isPinned);
        return Result.success();
    }

    @DeleteMapping("/{groupId}/announcements/{id}")
    @Operation(summary = "删除群公告")
    public Result<Void> deleteAnnouncement(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long groupId,
            @PathVariable Long id) {
        groupService.deleteAnnouncement(currentUserId, tenantId, groupId, id);
        return Result.success();
    }
}
