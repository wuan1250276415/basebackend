package com.basebackend.admin.controller;

import com.basebackend.admin.dto.DeptDTO;
import com.basebackend.admin.service.DeptService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/depts")
@RequiredArgsConstructor
@Tag(name = "部门管理", description = "部门管理相关接口")
public class DeptController {

    private final DeptService deptService;

    /**
     * 获取部门树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取部门树", description = "获取部门树形结构")
    public Result<List<DeptDTO>> getDeptTree() {
        log.info("获取部门树");
        try {
            List<DeptDTO> deptTree = deptService.getDeptTree();
            return Result.success("查询成功", deptTree);
        } catch (Exception e) {
            log.error("获取部门树失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取部门列表
     */
    @GetMapping
    @Operation(summary = "获取部门列表", description = "获取部门列表")
    public Result<List<DeptDTO>> getDeptList() {
        log.info("获取部门列表");
        try {
            List<DeptDTO> deptList = deptService.getDeptList();
            return Result.success("查询成功", deptList);
        } catch (Exception e) {
            log.error("获取部门列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询部门
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询部门", description = "根据ID查询部门详情")
    public Result<DeptDTO> getById(@Parameter(description = "部门ID") @PathVariable Long id) {
        log.info("根据ID查询部门: {}", id);
        try {
            DeptDTO dept = deptService.getById(id);
            return Result.success("查询成功", dept);
        } catch (Exception e) {
            log.error("根据ID查询部门失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建部门
     */
    @PostMapping
    @Operation(summary = "创建部门", description = "创建新部门")
    public Result<String> create(@Validated @RequestBody DeptDTO deptDTO) {
        log.info("创建部门: {}", deptDTO.getDeptName());
        try {
            deptService.create(deptDTO);
            return Result.success("部门创建成功");
        } catch (Exception e) {
            log.error("创建部门失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新部门", description = "更新部门信息")
    public Result<String> update(
            @Parameter(description = "部门ID") @PathVariable Long id,
            @Validated @RequestBody DeptDTO deptDTO) {
        log.info("更新部门: {}", id);
        try {
            deptDTO.setId(id);
            deptService.update(deptDTO);
            return Result.success("部门更新成功");
        } catch (Exception e) {
            log.error("更新部门失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门", description = "删除部门")
    public Result<String> delete(@Parameter(description = "部门ID") @PathVariable Long id) {
        log.info("删除部门: {}", id);
        try {
            deptService.delete(id);
            return Result.success("部门删除成功");
        } catch (Exception e) {
            log.error("删除部门失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据部门ID获取子部门列表
     */
    @GetMapping("/{id}/children")
    @Operation(summary = "获取子部门", description = "根据部门ID获取子部门列表")
    public Result<List<DeptDTO>> getChildrenByDeptId(@Parameter(description = "部门ID") @PathVariable Long id) {
        log.info("根据部门ID获取子部门列表: {}", id);
        try {
            List<DeptDTO> children = deptService.getChildrenByDeptId(id);
            return Result.success("查询成功", children);
        } catch (Exception e) {
            log.error("根据部门ID获取子部门列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据部门ID获取所有子部门ID列表
     */
    @GetMapping("/{id}/children-ids")
    @Operation(summary = "获取子部门ID列表", description = "根据部门ID获取所有子部门ID列表")
    public Result<List<Long>> getChildrenDeptIds(@Parameter(description = "部门ID") @PathVariable Long id) {
        log.info("根据部门ID获取所有子部门ID列表: {}", id);
        try {
            List<Long> childrenIds = deptService.getChildrenDeptIds(id);
            return Result.success("查询成功", childrenIds);
        } catch (Exception e) {
            log.error("根据部门ID获取所有子部门ID列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查部门名称是否唯一
     */
    @GetMapping("/check-dept-name")
    @Operation(summary = "检查部门名称唯一性", description = "检查部门名称是否唯一")
    public Result<Boolean> checkDeptNameUnique(
            @Parameter(description = "部门名称") @RequestParam String deptName,
            @Parameter(description = "父部门ID") @RequestParam Long parentId,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long deptId) {
        try {
            boolean unique = deptService.checkDeptNameUnique(deptName, parentId, deptId);
            return Result.success("检查完成", unique);
        } catch (Exception e) {
            log.error("检查部门名称唯一性失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
