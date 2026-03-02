package com.basebackend.ticket.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.datascope.annotation.DataScope;
import com.basebackend.common.datascope.enums.DataScopeType;
import com.basebackend.common.idempotent.annotation.Idempotent;
import com.basebackend.common.idempotent.enums.IdempotentStrategy;
import com.basebackend.common.model.Result;
import com.basebackend.common.ratelimit.RateLimit;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.observability.metrics.annotations.Counted;
import com.basebackend.observability.metrics.annotations.Timed;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.dto.*;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 工单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/tickets")
@RequiredArgsConstructor
@Validated
@Tag(name = "工单管理", description = "工单 CRUD 及状态管理")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "创建工单", description = "创建新工单")
    @OperationLog(operation = "创建工单", businessType = BusinessType.INSERT)
    @RequiresPermission("ticket:create")
    @Idempotent(strategy = IdempotentStrategy.TOKEN)
    @RateLimit(limit = 30, window = 60, message = "创建工单过于频繁")
    @Timed(name = "ticket.api.create")
    @Counted(name = "ticket.api.create.count")
    public Result<TicketDetailVO> create(@RequestBody @Valid TicketCreateDTO dto) {
        log.info("创建工单: title={}", dto.title());
        Ticket ticket = ticketService.create(dto);
        TicketDetailVO detail = ticketService.getDetail(ticket.getId());
        return Result.success("工单创建成功", detail);
    }

    @GetMapping
    @Operation(summary = "分页查询工单", description = "分页查询工单列表")
    @OperationLog(operation = "分页查询工单", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:list")
    @DataScope(type = DataScopeType.DEPT_AND_BELOW, deptAlias = "t", deptField = "dept_id")
    @Timed(name = "ticket.api.page")
    public Result<IPage<TicketListVO>> page(
            TicketQueryDTO query,
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long size) {
        log.info("分页查询工单: current={}, size={}", current, size);
        Page<Ticket> page = new Page<>(current, size);
        IPage<TicketListVO> result = ticketService.page(query, page);
        return Result.success("查询成功", result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "工单详情", description = "根据ID查询工单详情")
    @OperationLog(operation = "查询工单详情", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:query")
    public Result<TicketDetailVO> detail(@Parameter(description = "工单ID") @PathVariable Long id) {
        log.info("查询工单详情: id={}", id);
        TicketDetailVO detail = ticketService.getDetail(id);
        return Result.success("查询成功", detail);
    }

    @GetMapping("/no/{ticketNo}")
    @Operation(summary = "根据工单号查询", description = "根据工单号查询工单详情")
    @OperationLog(operation = "根据工单号查询", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:query")
    public Result<TicketDetailVO> detailByNo(@Parameter(description = "工单号") @PathVariable String ticketNo) {
        log.info("根据工单号查询: ticketNo={}", ticketNo);
        Ticket ticket = ticketService.getByTicketNo(ticketNo);
        TicketDetailVO detail = ticketService.getDetail(ticket.getId());
        return Result.success("查询成功", detail);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工单", description = "更新工单信息")
    @OperationLog(operation = "更新工单", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:update")
    public Result<String> update(
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody @Valid TicketUpdateDTO dto) {
        log.info("更新工单: id={}", id);
        ticketService.update(id, dto);
        return Result.success("工单更新成功");
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "变更工单状态", description = "变更工单状态")
    @OperationLog(operation = "变更工单状态", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:update")
    @Idempotent(strategy = IdempotentStrategy.SPEL, key = "'ticket:status:' + #id + ':' + #dto.toStatus()")
    @Timed(name = "ticket.api.changeStatus")
    @Counted(name = "ticket.api.changeStatus.count")
    public Result<String> changeStatus(
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody @Valid TicketStatusChangeDTO dto) {
        log.info("变更工单状态: id={}, toStatus={}", id, dto.toStatus());
        TicketStatus toStatus = TicketStatus.valueOf(dto.toStatus());
        ticketService.changeStatus(id, toStatus, dto.remark());
        return Result.success("状态变更成功");
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "分配处理人", description = "分配工单处理人")
    @OperationLog(operation = "分配工单处理人", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:assign")
    public Result<String> assign(
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody @Valid TicketAssignDTO dto) {
        log.info("分配工单处理人: id={}, assigneeId={}", id, dto.assigneeId());
        ticketService.assign(id, dto.assigneeId(), dto.assigneeName());
        return Result.success("分配成功");
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "关闭工单", description = "关闭工单")
    @OperationLog(operation = "关闭工单", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:update")
    public Result<String> close(
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody(required = false) TicketCloseDTO dto) {
        log.info("关闭工单: id={}", id);
        ticketService.close(id, dto != null ? dto.remark() : null);
        return Result.success("工单已关闭");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工单", description = "逻辑删除工单")
    @OperationLog(operation = "删除工单", businessType = BusinessType.DELETE)
    @RequiresPermission("ticket:delete")
    @Counted(name = "ticket.api.delete.count")
    public Result<String> delete(@Parameter(description = "工单ID") @PathVariable Long id) {
        log.info("删除工单: id={}", id);
        ticketService.delete(id);
        return Result.success("工单删除成功");
    }
}
