package com.basebackend.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.system.entity.SysListOperation;
import com.basebackend.system.mapper.SysListOperationMapper;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 列表操作管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/list-operations")
@RequiredArgsConstructor
@Tag(name = "列表操作管理", description = "列表操作管理相关接口")
public class ListOperationController {

    private final SysListOperationMapper listOperationMapper;

    /**
     * 查询所有列表操作
     */
    @GetMapping
    @Operation(summary = "查询所有列表操作", description = "查询所有可用的列表操作")
    public Result<List<SysListOperation>> list() {
        log.info("查询所有列表操作");
        try {
            LambdaQueryWrapper<SysListOperation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysListOperation::getStatus, 1)
                    .orderByAsc(SysListOperation::getOrderNum);
            List<SysListOperation> operations = listOperationMapper.selectList(wrapper);
            return Result.success("查询成功", operations);
        } catch (Exception e) {
            log.error("查询列表操作失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据资源类型查询列表操作
     */
    @GetMapping("/by-resource-type")
    @Operation(summary = "根据资源类型查询列表操作", description = "根据资源类型查询可用的列表操作")
    public Result<List<SysListOperation>> listByResourceType(
            @RequestParam(required = false) String resourceType) {
        log.info("根据资源类型查询列表操作: resourceType={}", resourceType);
        try {
            LambdaQueryWrapper<SysListOperation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysListOperation::getStatus, 1);

            if (resourceType != null && !resourceType.isEmpty()) {
                wrapper.and(w -> w.eq(SysListOperation::getResourceType, resourceType)
                        .or()
                        .isNull(SysListOperation::getResourceType));
            }

            wrapper.orderByAsc(SysListOperation::getOrderNum);
            List<SysListOperation> operations = listOperationMapper.selectList(wrapper);
            return Result.success("查询成功", operations);
        } catch (Exception e) {
            log.error("根据资源类型查询列表操作失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
