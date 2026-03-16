package com.basebackend.ticket.ai;

import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.ai.dto.TicketClassifyResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工单 AI 智能控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/ai")
@RequiredArgsConstructor
@ConditionalOnBean(TicketAiService.class)
@Tag(name = "工单AI", description = "AI 智能分类、摘要、推荐回复")
public class TicketAiController {

    private final TicketAiService ticketAiService;

    @PostMapping("/classify")
    @Operation(summary = "AI自动分类", description = "根据工单标题和描述进行AI自动分类")
    @OperationLog(operation = "AI自动分类工单", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:ai")
    public Result<TicketClassifyResult> classify(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String description = body.get("description");
        log.info("AI自动分类: title={}", title);
        TicketClassifyResult result = ticketAiService.classifyTicket(title, description);
        return Result.success("分类完成", result);
    }

    @GetMapping("/summary/{ticketId}")
    @Operation(summary = "AI工单摘要", description = "AI 生成工单摘要")
    @OperationLog(operation = "AI生成工单摘要", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:ai")
    public Result<String> summary(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        log.info("AI工单摘要: ticketId={}", ticketId);
        String summary = ticketAiService.summarizeTicket(ticketId);
        return Result.success("摘要生成完成", summary);
    }

    @GetMapping("/suggest-reply/{ticketId}")
    @Operation(summary = "AI推荐回复", description = "AI 推荐回复内容")
    @OperationLog(operation = "AI推荐回复", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:ai")
    public Result<List<String>> suggestReply(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        log.info("AI推荐回复: ticketId={}", ticketId);
        List<String> suggestions = ticketAiService.suggestReply(ticketId);
        return Result.success("推荐回复生成完成", suggestions);
    }
}
