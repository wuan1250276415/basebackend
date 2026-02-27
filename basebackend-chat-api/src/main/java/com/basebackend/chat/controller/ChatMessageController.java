package com.basebackend.chat.controller;

import com.basebackend.chat.dto.request.SendMessageRequest;
import com.basebackend.chat.dto.request.ForwardMessageRequest;
import com.basebackend.chat.service.ChatMessageService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
@Validated
@Tag(name = "聊天消息", description = "消息发送、撤回、转发、历史查询等接口")
public class ChatMessageController {

    private final ChatMessageService messageService;

    @PostMapping
    @Operation(summary = "发送消息", description = "REST 备用通道，主要消息发送通过 WebSocket")
    public Result<Map<String, Object>> sendMessage(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @Validated @RequestBody SendMessageRequest request) {
        var data = messageService.sendMessage(currentUserId, tenantId, request);
        return Result.success("发送成功", data);
    }

    @GetMapping
    @Operation(summary = "获取历史消息", description = "按会话分页加载历史消息")
    public Result<Map<String, Object>> getMessages(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @Parameter(description = "会话ID", required = true) @RequestParam Long conversationId,
            @Parameter(description = "从此消息ID向前加载（不含）") @RequestParam(required = false) Long beforeId,
            @Parameter(description = "从此消息ID向后加载（不含）") @RequestParam(required = false) Long afterId,
            @Parameter(description = "条数，默认30，最大100") @RequestParam(required = false, defaultValue = "30") Integer limit) {
        var data = messageService.getMessages(currentUserId, tenantId, conversationId, beforeId, afterId, limit);
        return Result.success(data);
    }

    @PostMapping("/{messageId}/revoke")
    @Operation(summary = "撤回消息", description = "仅发送者本人可撤回，且在发送后2分钟内")
    public Result<Map<String, Object>> revokeMessage(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long messageId) {
        var data = messageService.revokeMessage(currentUserId, tenantId, messageId);
        return Result.success("消息已撤回", data);
    }

    @PostMapping("/forward")
    @Operation(summary = "转发消息", description = "支持逐条转发和合并转发")
    public Result<Map<String, Object>> forwardMessage(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @Validated @RequestBody ForwardMessageRequest request) {
        var data = messageService.forwardMessage(currentUserId, tenantId, request);
        return Result.success("转发成功", data);
    }

    @GetMapping("/{messageId}/read-status")
    @Operation(summary = "群已读回执详情", description = "查看消息的已读/未读用户列表")
    public Result<Map<String, Object>> getReadStatus(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @PathVariable Long messageId) {
        var data = messageService.getReadStatus(currentUserId, tenantId, messageId);
        return Result.success(data);
    }
}
