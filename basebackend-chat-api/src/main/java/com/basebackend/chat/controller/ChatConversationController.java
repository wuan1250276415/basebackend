package com.basebackend.chat.controller;

import com.basebackend.chat.dto.request.CreateConversationRequest;
import com.basebackend.chat.dto.response.ConversationVO;
import com.basebackend.chat.service.ChatConversationService;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 会话控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/conversations")
@RequiredArgsConstructor
@Validated
@Tag(name = "聊天会话", description = "会话列表、创建、设置等接口")
public class ChatConversationController {

    private final ChatConversationService conversationService;

    @GetMapping
    @Operation(summary = "获取会话列表", description = "按最后消息时间倒序，置顶优先")
    public Result<PageResult<ConversationVO>> listConversations(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        var data = conversationService.listConversations(currentUserId, tenantId, pageNum, pageSize);
        return Result.success(data);
    }

    @PostMapping
    @Operation(summary = "创建/打开会话", description = "私聊幂等：已存在则返回已有会话")
    public Result<Map<String, Object>> createOrOpen(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @Validated @RequestBody CreateConversationRequest request) {
        var data = conversationService.createOrOpen(currentUserId, tenantId, request);
        return Result.success(data);
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "删除会话", description = "隐藏会话，不删消息")
    public Result<Void> deleteConversation(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long conversationId) {
        conversationService.deleteConversation(currentUserId, tenantId, conversationId);
        return Result.success();
    }

    @PutMapping("/{conversationId}/pin")
    @Operation(summary = "置顶/取消置顶")
    public Result<Void> pinConversation(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long conversationId,
            @RequestBody Map<String, Boolean> body) {
        conversationService.pinConversation(currentUserId, tenantId, conversationId,
                Boolean.TRUE.equals(body.get("isPinned")));
        return Result.success();
    }

    @PutMapping("/{conversationId}/mute")
    @Operation(summary = "免打扰设置")
    public Result<Void> muteConversation(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long conversationId,
            @RequestBody Map<String, Boolean> body) {
        conversationService.muteConversation(currentUserId, tenantId, conversationId,
                Boolean.TRUE.equals(body.get("isMuted")));
        return Result.success();
    }

    @PutMapping("/{conversationId}/draft")
    @Operation(summary = "保存草稿")
    public Result<Void> saveDraft(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> body) {
        conversationService.saveDraft(currentUserId, tenantId, conversationId, body.get("draft"));
        return Result.success();
    }

    @PutMapping("/{conversationId}/read")
    @Operation(summary = "清空未读 / 标记已读")
    public Result<Map<String, Object>> markAsRead(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long conversationId,
            @RequestBody Map<String, Long> body) {
        int cleared = conversationService.markAsRead(currentUserId, tenantId, conversationId,
                body.get("lastReadMessageId"));
        return Result.success(Map.of("clearedCount", cleared));
    }
}
