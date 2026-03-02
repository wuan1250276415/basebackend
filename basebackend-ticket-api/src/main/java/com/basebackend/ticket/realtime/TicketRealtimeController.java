package com.basebackend.ticket.realtime;

import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

/**
 * 工单实时推送控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/realtime")
@RequiredArgsConstructor
@ConditionalOnBean(TicketRealtimeService.class)
@Tag(name = "工单实时推送", description = "工单 WebSocket 实时推送订阅管理")
public class TicketRealtimeController {

    private final TicketRealtimeService realtimeService;

    @PostMapping("/subscribe/{ticketId}")
    @Operation(summary = "订阅工单更新", description = "订阅指定工单的实时更新推送")
    @OperationLog(operation = "订阅工单实时推送", businessType = BusinessType.INSERT)
    public Result<String> subscribe(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        String userId = String.valueOf(UserContextHolder.getUserId());
        realtimeService.subscribeTicket(ticketId, userId);
        return Result.success("订阅成功");
    }

    @DeleteMapping("/unsubscribe/{ticketId}")
    @Operation(summary = "取消订阅工单更新", description = "取消订阅指定工单的实时更新推送")
    @OperationLog(operation = "取消订阅工单实时推送", businessType = BusinessType.DELETE)
    public Result<String> unsubscribe(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        String userId = String.valueOf(UserContextHolder.getUserId());
        realtimeService.unsubscribeTicket(ticketId, userId);
        return Result.success("取消订阅成功");
    }
}
