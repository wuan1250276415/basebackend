package com.basebackend.system.controller;

import com.basebackend.security.enums.DataScopeType;
import com.basebackend.system.dto.DeptDTO;
import com.basebackend.system.service.DeptService;
import com.basebackend.common.model.Result;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.security.annotation.DataScope;
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
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
@Validated
@Tag(name = "部门管理", description = "部门管理相关接口")
public class DeptController {

    private final DeptService deptService;

    /**
     * 统一处理控制器异常，避免向客户端泄露敏感信息
     */
    private <T> Result<T> handleControllerError(String action, Exception e) {
        log.error("{}失败", action, e);
        return Result.error("系统繁忙，请稍后再试");
    }

    /**
     * 获取部门树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取部门树", description = "获取部门树形结构")
    @DataScope(DataScopeType.DEPT_AND_CHILD)
    public Result<List<DeptDTO>> getDeptTree() {
        log.info("获取部门树");
        try {
            List<DeptDTO> deptTree = deptService.getDeptTree();
            return Result.success("查询成功", deptTree);
        } catch (Exception e) {
            return handleControllerError("获取部门树", e);
        }
    }

    /**
     * 获取部门列表
     */
    @GetMapping
    @Operation(summary = "获取部门列表", description = "获取部门列表")
    @DataScope(DataScopeType.DEPT_AND_CHILD)
    public Result<List<DeptDTO>> getDeptList() {
        log.info("获取部门列表");
        try {
            List<DeptDTO> deptList = deptService.getDeptList();
            return Result.success("查询成功", deptList);
        } catch (Exception e) {
            return handleControllerError("获取部门列表", e);
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
            return handleControllerError("根据ID查询部门", e);
        }
    }

    /**
     * 创建部门
     */
    @PostMapping
    @Operation(summary = "创建部门", description = "创建新部门")
    @RequiresPermission("system:dept:create")
    public Result<String> create(@Validated @RequestBody DeptDTO deptDTO) {
        log.info("创建部门: {}", deptDTO.getDeptName());
        try {
            deptService.create(deptDTO);
            return Result.success("部门创建成功");
        } catch (Exception e) {
            return handleControllerError("创建部门", e);
        }
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新部门", description = "更新部门信息")
    @RequiresPermission("system:dept:update")
    public Result<String> update(
            @Parameter(description = "部门ID") @PathVariable Long id,
            @Validated @RequestBody DeptDTO deptDTO) {
        log.info("更新部门: {}", id);
        try {
            deptDTO.setId(id);
            deptService.update(deptDTO);
            return Result.success("部门更新成功");
        } catch (Exception e) {
            return handleControllerError("更新部门", e);
        }
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门", description = "删除部门")
    @RequiresPermission("system:dept:delete")
    public Result<String> delete(@Parameter(description = "部门ID") @PathVariable Long id) {
        log.info("删除部门: {}", id);
        try {
            deptService.delete(id);
            return Result.success("部门删除成功");
        } catch (Exception e) {
            return handleControllerError("删除部门", e);
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
            return handleControllerError("获取子部门列表", e);
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
            return handleControllerError("获取子部门ID列表", e);
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
            return handleControllerError("检查部门名称唯一性", e);
        }
    }

    /**
     * 根据部门名称查询部门（用于 Feign 调用）
     */
    @GetMapping("/by-name")
    @Operation(summary = "根据部门名称查询", description = "根据部门名称查询部门信息")
    public Result<DeptDTO> getByDeptName(@Parameter(description = "部门名称") @RequestParam String deptName) {
        log.info("根据部门名称查询: {}", deptName);
        try {
            DeptDTO dept = deptService.getByDeptName(deptName);
            return Result.success("查询成功", dept);
        } catch (Exception e) {
            return handleControllerError("根据部门名称查询", e);
        }
    }

    /**
     * 根据部门编码查询部门（用于 Feign 调用）
     */
    @GetMapping("/by-code")
    @Operation(summary = "根据部门编码查询", description = "根据部门编码查询部门信息")
    public Result<DeptDTO> getByDeptCode(@Parameter(description = "部门编码") @RequestParam String deptCode) {
        log.info("根据部门编码查询: {}", deptCode);
        try {
            DeptDTO dept = deptService.getByDeptCode(deptCode);
            return Result.success("查询成功", dept);
        } catch (Exception e) {
            return handleControllerError("根据部门编码查询", e);
        }
    }

    /**
     * 批量查询部门（用于 Feign 调用）
     * 限制：最多支持100个ID，参数长度不超过1000字符
     */
    @GetMapping("/batch")
    @Operation(summary = "批量查询部门", description = "根据部门ID列表批量查询部门信息（最多100个ID）")
    public Result<List<DeptDTO>> getBatchByIds(@Parameter(description = "部门ID列表（逗号分隔）") @RequestParam String deptIds) {
        log.info("批量查询部门: {}", deptIds);
        
        // 输入验证：参数长度限制
        if (deptIds == null || deptIds.isBlank()) {
            return Result.error("部门ID列表不能为空");
        }
        if (deptIds.length() > 1000) {
            return Result.error("参数过长，请减少查询数量");
        }
        
        try {
            String[] idArray = deptIds.split(",");
            
            // 输入验证：ID数量限制
            if (idArray.length > 100) {
                return Result.error("批量查询最多支持100个ID");
            }
            
            List<Long> ids = new java.util.ArrayList<>();
            for (String id : idArray) {
                String trimmedId = id.trim();
                if (trimmedId.isEmpty()) {
                    continue;
                }
                // 验证ID格式
                if (!trimmedId.matches("\\d+")) {
                    return Result.error("ID格式不正确: " + trimmedId);
                }
                ids.add(Long.parseLong(trimmedId));
            }
            
            if (ids.isEmpty()) {
                return Result.error("有效的部门ID列表不能为空");
            }
            
            List<DeptDTO> depts = deptService.getBatchByIds(ids);
            return Result.success("查询成功", depts);
        } catch (NumberFormatException e) {
            log.warn("批量查询部门ID格式错误: {}", deptIds);
            return Result.error("ID格式不正确，请输入有效的数字ID");
        } catch (Exception e) {
            return handleControllerError("批量查询部门", e);
        }
    }

    /**
     * 根据父部门ID查询直接子部门（用于 Feign 调用）
     */
    @GetMapping("/by-parent")
    @Operation(summary = "根据父部门ID查询", description = "根据父部门ID获取直接子部门列表")
    public Result<List<DeptDTO>> getByParentId(@Parameter(description = "父部门ID") @RequestParam Long parentId) {
        log.info("根据父部门ID查询: {}", parentId);
        try {
            List<DeptDTO> depts = deptService.getByParentId(parentId);
            return Result.success("查询成功", depts);
        } catch (Exception e) {
            return handleControllerError("根据父部门ID查询", e);
        }
    }
}
