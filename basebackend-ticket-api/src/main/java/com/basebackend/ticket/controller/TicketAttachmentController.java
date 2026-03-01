package com.basebackend.ticket.controller;

import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.dto.TicketAttachmentAddDTO;
import com.basebackend.ticket.entity.TicketAttachment;
import com.basebackend.ticket.service.TicketAttachmentService;
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
 * 工单附件控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
@Validated
@Tag(name = "工单附件", description = "工单附件关联管理")
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;

    @GetMapping
    @Operation(summary = "附件列表", description = "查询工单的附件列表")
    @OperationLog(operation = "查询工单附件列表", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:query")
    public Result<List<TicketAttachment>> list(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        log.info("查询工单附件列表: ticketId={}", ticketId);
        List<TicketAttachment> attachments = attachmentService.listByTicketId(ticketId);
        return Result.success("查询成功", attachments);
    }

    @PostMapping
    @Operation(summary = "关联附件", description = "关联附件到工单")
    @OperationLog(operation = "关联工单附件", businessType = BusinessType.INSERT)
    @RequiresPermission("ticket:attachment:create")
    public Result<String> add(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @RequestBody @Valid TicketAttachmentAddDTO dto) {
        log.info("关联附件到工单: ticketId={}, fileId={}", ticketId, dto.fileId());
        attachmentService.add(ticketId, dto.fileId(), dto.fileName(),
                dto.fileSize(), dto.fileType(), dto.fileUrl());
        return Result.success("附件关联成功");
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "移除附件", description = "移除工单附件")
    @OperationLog(operation = "移除工单附件", businessType = BusinessType.DELETE)
    @RequiresPermission("ticket:attachment:delete")
    public Result<String> delete(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        log.info("移除工单附件: ticketId={}, attachmentId={}", ticketId, attachmentId);
        attachmentService.delete(ticketId, attachmentId);
        return Result.success("附件移除成功");
    }
}
