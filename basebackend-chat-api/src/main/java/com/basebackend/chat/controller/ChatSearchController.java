package com.basebackend.chat.controller;

import com.basebackend.chat.dto.ChatMessageSearchDoc;
import com.basebackend.chat.enums.ChatErrorCode;
import com.basebackend.chat.service.ChatMessageSearchService;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.Result;
import com.basebackend.search.model.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 消息搜索控制器
 * <p>
 * 仅当 {@code basebackend.search.enabled=true} 时激活。
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "消息搜索", description = "基于 Elasticsearch 的聊天消息全文检索")
@ConditionalOnProperty(prefix = "basebackend.search", name = "enabled", havingValue = "true")
public class ChatSearchController {

    private final ChatMessageSearchService searchService;

    @GetMapping("/messages")
    @Operation(summary = "搜索聊天消息", description = "在指定会话内全文检索消息内容，支持时间范围过滤")
    public Result<SearchResult<ChatMessageSearchDoc>> searchMessages(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "会话ID（限定搜索范围）") @RequestParam(required = false) Long conversationId,
            @Parameter(description = "起始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，最大50") @RequestParam(defaultValue = "20") int size) {
        try {
            var result = searchService.searchMessages(tenantId, currentUserId, keyword,
                    conversationId, startTime, endTime, page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("消息搜索异常: {}", e.getMessage(), e);
            throw new BusinessException(ChatErrorCode.SEARCH_UNAVAILABLE);
        }
    }
}
