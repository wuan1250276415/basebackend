package com.basebackend.ticket.controller;

import com.basebackend.common.model.Result;
import com.basebackend.common.ratelimit.RateLimit;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.dto.TicketCommentDTO;
import com.basebackend.ticket.entity.TicketComment;
import com.basebackend.ticket.service.TicketCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/tickets/{ticketId}/comments")
@RequiredArgsConstructor
@Validated
@Tag(name = "工单评论", description = "工单评论 CRUD")
public class TicketCommentController {

    private final TicketCommentService commentService;

    @GetMapping
    @Operation(summary = "评论列表", description = "查询工单的评论列表")
    @OperationLog(operation = "查询工单评论列表", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:query")
    public Result<List<TicketComment>> list(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        log.info("查询工单评论列表: ticketId={}", ticketId);
        List<TicketComment> comments = commentService.listByTicketId(ticketId);
        return Result.success("查询成功", comments);
    }

    @PostMapping
    @Operation(summary = "添加评论", description = "添加工单评论")
    @OperationLog(operation = "添加工单评论", businessType = BusinessType.INSERT)
    @RequiresPermission("ticket:comment:create")
    @RateLimit(limit = 30, window = 60, message = "评论过于频繁")
    public Result<TicketComment> add(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @RequestBody @Valid TicketCommentDTO dto) {
        log.info("添加工单评论: ticketId={}", ticketId);
        TicketComment comment = commentService.add(ticketId, dto);
        return Result.success("评论添加成功", comment);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "删除工单评论")
    @OperationLog(operation = "删除工单评论", businessType = BusinessType.DELETE)
    @RequiresPermission("ticket:comment:delete")
    public Result<String> delete(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @Parameter(description = "评论ID") @PathVariable Long commentId) {
        log.info("删除工单评论: ticketId={}, commentId={}", ticketId, commentId);
        commentService.delete(ticketId, commentId);
        return Result.success("评论删除成功");
    }
}
