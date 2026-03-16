package com.basebackend.ticket.search;

import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.search.model.SearchResult;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.dto.TicketQueryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

/**
 * 工单全文搜索控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/search")
@RequiredArgsConstructor
@ConditionalOnBean(TicketSearchService.class)
@Tag(name = "工单搜索", description = "工单全文搜索与索引管理")
public class TicketSearchController {

    private final TicketSearchService ticketSearchService;

    @GetMapping
    @Operation(summary = "全文搜索工单", description = "根据关键词全文搜索工单，支持高亮和过滤")
    @OperationLog(operation = "全文搜索工单", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:list")
    public Result<SearchResult<TicketSearchDocument>> search(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            TicketQueryDTO filters,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        log.info("全文搜索工单: keyword={}, page={}, size={}", keyword, page, size);
        SearchResult<TicketSearchDocument> result = ticketSearchService.search(keyword, filters, page, size);
        return Result.success("搜索成功", result);
    }

    @PostMapping("/reindex")
    @Operation(summary = "重建搜索索引", description = "全量重建工单搜索索引")
    @OperationLog(operation = "重建工单搜索索引", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:admin")
    public Result<String> reindex() {
        log.info("触发工单搜索索引重建");
        ticketSearchService.reindexAll();
        return Result.success("索引重建已完成");
    }
}
