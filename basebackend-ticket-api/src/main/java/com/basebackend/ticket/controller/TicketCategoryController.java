package com.basebackend.ticket.controller;

import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.dto.TicketCategoryDTO;
import com.basebackend.ticket.dto.TicketCategoryTreeVO;
import com.basebackend.ticket.service.TicketCategoryService;
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
 * 工单分类管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "工单分类管理", description = "工单分类 CRUD 及树形结构查询")
public class TicketCategoryController {

    private final TicketCategoryService categoryService;

    @GetMapping
    @Operation(summary = "分类列表(树)", description = "获取工单分类树形结构")
    @OperationLog(operation = "查询工单分类树", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:category:list")
    public Result<List<TicketCategoryTreeVO>> tree() {
        log.info("查询工单分类树");
        List<TicketCategoryTreeVO> tree = categoryService.tree();
        return Result.success("查询成功", tree);
    }

    @PostMapping
    @Operation(summary = "创建分类", description = "创建工单分类")
    @OperationLog(operation = "创建工单分类", businessType = BusinessType.INSERT)
    @RequiresPermission("ticket:category:create")
    public Result<String> create(@RequestBody @Valid TicketCategoryDTO dto) {
        log.info("创建工单分类: name={}", dto.name());
        categoryService.create(dto);
        return Result.success("分类创建成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类", description = "更新工单分类")
    @OperationLog(operation = "更新工单分类", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:category:update")
    public Result<String> update(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @RequestBody @Valid TicketCategoryDTO dto) {
        log.info("更新工单分类: id={}", id);
        categoryService.update(id, dto);
        return Result.success("分类更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除工单分类")
    @OperationLog(operation = "删除工单分类", businessType = BusinessType.DELETE)
    @RequiresPermission("ticket:category:delete")
    public Result<String> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        log.info("删除工单分类: id={}", id);
        categoryService.delete(id);
        return Result.success("分类删除成功");
    }
}
