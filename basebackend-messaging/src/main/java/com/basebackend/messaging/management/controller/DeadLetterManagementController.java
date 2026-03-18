package com.basebackend.messaging.management.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.messaging.management.dto.DeadLetterView;
import com.basebackend.messaging.management.service.DeadLetterManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/messaging/dead-letter")
@RequiredArgsConstructor
@Validated
public class DeadLetterManagementController {

    private final DeadLetterManagementService deadLetterManagementService;

    @GetMapping("/page")
    public Result<PageResult<DeadLetterView>> getPage(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String status) {
        try {
            return Result.success(deadLetterManagementService.getPage(page, size, status));
        } catch (Exception e) {
            log.error("分页查询死信失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<DeadLetterView> getById(@PathVariable Long id) {
        try {
            DeadLetterView detail = deadLetterManagementService.getById(id);
            return detail != null ? Result.success(detail) : Result.error("死信不存在");
        } catch (Exception e) {
            log.error("查询死信详情失败: id={}", id, e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/redeliver")
    public Result<Void> redeliver(@PathVariable Long id) {
        try {
            deadLetterManagementService.redeliver(id);
            return Result.success();
        } catch (Exception e) {
            log.error("重投死信失败: id={}", id, e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/batch-redeliver")
    public Result<Integer> batchRedeliver(@RequestBody List<Long> ids) {
        try {
            return Result.success("批量重投成功", deadLetterManagementService.batchRedeliver(ids));
        } catch (Exception e) {
            log.error("批量重投死信失败: ids={}", ids, e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/discard")
    public Result<Void> discard(@PathVariable Long id) {
        try {
            deadLetterManagementService.discard(id);
            return Result.success();
        } catch (Exception e) {
            log.error("丢弃死信失败: id={}", id, e);
            return Result.error(e.getMessage());
        }
    }
}
