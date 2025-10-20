package com.basebackend.admin.controller.messaging;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.entity.messaging.SysDeadLetter;
import com.basebackend.admin.service.messaging.DeadLetterService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 死信处理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/messaging/dead-letter")
@Tag(name = "死信处理", description = "死信消息的查询和处理")
public class DeadLetterController {

    private final DeadLetterService deadLetterService;

    public DeadLetterController(DeadLetterService deadLetterService) {
        this.deadLetterService = deadLetterService;
    }

    @Operation(summary = "分页查询死信")
    @GetMapping("/page")
    public Result<Page<SysDeadLetter>> getDeadLetterPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {
        return Result.success(deadLetterService.getDeadLetterPage(page, size, status));
    }

    @Operation(summary = "获取死信详情")
    @GetMapping("/{id}")
    public Result<SysDeadLetter> getDeadLetter(@PathVariable Long id) {
        return Result.success(deadLetterService.getDeadLetterById(id));
    }

    @Operation(summary = "重新投递死信")
    @PostMapping("/{id}/redeliver")
    public Result<Void> redeliverDeadLetter(@PathVariable Long id) {
        deadLetterService.redeliverDeadLetter(id);
        return Result.success();
    }

    @Operation(summary = "丢弃死信")
    @PostMapping("/{id}/discard")
    public Result<Void> discardDeadLetter(@PathVariable Long id) {
        deadLetterService.discardDeadLetter(id);
        return Result.success();
    }

    @Operation(summary = "批量重新投递")
    @PostMapping("/batch-redeliver")
    public Result<Void> batchRedeliver(@RequestBody Long[] ids) {
        deadLetterService.batchRedeliver(ids);
        return Result.success();
    }
}
