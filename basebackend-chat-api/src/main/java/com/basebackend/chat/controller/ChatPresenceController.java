package com.basebackend.chat.controller;

import com.basebackend.chat.service.OnlineStatusService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在线状态控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/presence")
@RequiredArgsConstructor
@Tag(name = "在线状态", description = "用户在线/离线/忙碌状态管理")
public class ChatPresenceController {

    private final OnlineStatusService onlineStatusService;

    @GetMapping("/batch")
    @Operation(summary = "批量获取用户在线状态")
    public Result<Map<String, Map<String, Object>>> batchGetStatus(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @RequestParam String userIds) {
        List<Long> idList = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return Result.success(onlineStatusService.batchGetStatus(tenantId, idList));
    }

    @PutMapping("/status")
    @Operation(summary = "设置当前用户在线状态", description = "status: online / busy / away")
    public Result<Void> setStatus(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "online");
        onlineStatusService.setStatus(tenantId, currentUserId, status);
        return Result.success();
    }
}
