package com.basebackend.auth.controller;

import com.basebackend.auth.dto.PermissionDTO;
import com.basebackend.auth.service.PermissionService;
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
 * 权限管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Validated
@Tag(name = "权限管理", description = "权限管理相关接口")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 获取权限列表
     */
    @GetMapping
    @Operation(summary = "获取权限列表", description = "获取权限列表，支持按类型过滤")
    public Result<List<PermissionDTO>> getPermissionList(
            @Parameter(description = "权限类型（可选）") @RequestParam(required = false) Integer permissionType) {
        log.info("获取权限列表, permissionType={}", permissionType);
        try {
            List<PermissionDTO> permissions;
            if (permissionType != null) {
                permissions = permissionService.getPermissionsByType(permissionType);
            } else {
                permissions = permissionService.getPermissionList();
            }
            return Result.success("查询成功", permissions);
        } catch (Exception e) {
            log.error("获取权限列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据权限类型获取权限列表
     */
    @GetMapping("/type/{permissionType}")
    @Operation(summary = "根据权限类型获取权限列表", description = "根据权限类型获取权限列表")
    public Result<List<PermissionDTO>> getPermissionsByType(
            @Parameter(description = "权限类型") @PathVariable Integer permissionType) {
        log.info("根据权限类型获取权限列表: {}", permissionType);
        try {
            List<PermissionDTO> permissions = permissionService.getPermissionsByType(permissionType);
            return Result.success("查询成功", permissions);
        } catch (Exception e) {
            log.error("根据权限类型获取权限列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询权限
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询权限", description = "根据ID查询权限详情")
    public Result<PermissionDTO> getById(@Parameter(description = "权限ID") @PathVariable Long id) {
        log.info("根据ID查询权限: {}", id);
        try {
            PermissionDTO permission = permissionService.getById(id);
            return Result.success("查询成功", permission);
        } catch (Exception e) {
            log.error("根据ID查询权限失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建权限
     */
    @PostMapping
    @Operation(summary = "创建权限", description = "创建新权限")
    public Result<String> create(@Validated @RequestBody PermissionDTO permissionDTO) {
        log.info("创建权限: {}", permissionDTO.getPermissionName());
        try {
            permissionService.create(permissionDTO);
            return Result.success("权限创建成功");
        } catch (Exception e) {
            log.error("创建权限失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新权限", description = "更新权限信息")
    public Result<String> update(
            @Parameter(description = "权限ID") @PathVariable Long id,
            @Validated @RequestBody PermissionDTO permissionDTO) {
        log.info("更新权限: {}", id);
        try {
            permissionDTO.setId(id);
            permissionService.update(permissionDTO);
            return Result.success("权限更新成功");
        } catch (Exception e) {
            log.error("更新权限失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限", description = "删除权限")
    public Result<String> delete(@Parameter(description = "权限ID") @PathVariable Long id) {
        log.info("删除权限: {}", id);
        try {
            permissionService.delete(id);
            return Result.success("权限删除成功");
        } catch (Exception e) {
            log.error("删除权限失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据用户ID获取权限列表
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户权限", description = "根据用户ID获取权限列表")
    public Result<List<PermissionDTO>> getPermissionsByUserId(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("根据用户ID获取权限列表: {}", userId);
        try {
            List<PermissionDTO> permissions = permissionService.getPermissionsByUserId(userId);
            return Result.success("查询成功", permissions);
        } catch (Exception e) {
            log.error("根据用户ID获取权限列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据角色ID获取权限列表
     */
    @GetMapping("/role/{roleId}")
    @Operation(summary = "获取角色权限", description = "根据角色ID获取权限列表")
    public Result<List<PermissionDTO>> getPermissionsByRoleId(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        log.info("根据角色ID获取权限列表: {}", roleId);
        try {
            List<PermissionDTO> permissions = permissionService.getPermissionsByRoleId(roleId);
            return Result.success("查询成功", permissions);
        } catch (Exception e) {
            log.error("根据角色ID获取权限列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查权限标识是否唯一
     */
    @GetMapping("/check-permission-key")
    @Operation(summary = "检查权限标识唯一性", description = "检查权限标识是否唯一")
    public Result<Boolean> checkPermissionKeyUnique(
            @Parameter(description = "权限标识") @RequestParam String permissionKey,
            @Parameter(description = "权限ID") @RequestParam(required = false) Long permissionId) {
        try {
            boolean unique = permissionService.checkPermissionKeyUnique(permissionKey, permissionId);
            return Result.success("检查完成", unique);
        } catch (Exception e) {
            log.error("检查权限标识唯一性失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
