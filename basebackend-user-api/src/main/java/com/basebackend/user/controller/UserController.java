package com.basebackend.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.user.dto.UserCreateDTO;
import com.basebackend.user.dto.UserDTO;
import com.basebackend.user.dto.UserQueryDTO;
import com.basebackend.user.service.UserService;
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
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表")
    public Result<Page<UserDTO>> page(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            UserQueryDTO queryDTO) {
        log.info("分页查询用户列表: current={}, size={}", current, size);
        try {
            Page<UserDTO> result = userService.page(queryDTO, current, size);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询用户列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户", description = "根据ID查询用户详情")
    public Result<UserDTO> getById(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("根据ID查询用户: {}", id);
        try {
            UserDTO user = userService.getById(id);
            return Result.success("查询成功", user);
        } catch (Exception e) {
            log.error("根据ID查询用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    public Result<String> create(@Validated @RequestBody UserCreateDTO userCreateDTO) {
        log.info("创建用户: {}", userCreateDTO.getUsername());
        try {
            userService.create(userCreateDTO);
            return Result.success("用户创建成功");
        } catch (Exception e) {
            log.error("创建用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    public Result<String> update(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Validated @RequestBody UserDTO userDTO) {
        log.info("更新用户: {}", id);
        try {
            userDTO.setId(id);
            userService.update(userDTO);
            return Result.success("用户更新成功");
        } catch (Exception e) {
            log.error("更新用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除用户")
    public Result<String> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("删除用户: {}", id);
        try {
            userService.delete(id);
            return Result.success("用户删除成功");
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除用户", description = "批量删除用户")
    public Result<String> deleteBatch(@RequestBody List<Long> ids) {
        log.info("批量删除用户: {}", ids);
        try {
            userService.deleteBatch(ids);
            return Result.success("批量删除成功");
        } catch (Exception e) {
            log.error("批量删除用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置密码", description = "重置用户密码")
    public Result<String> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        log.info("重置用户密码: {}", id);
        try {
            userService.resetPassword(id, newPassword);
            return Result.success("密码重置成功");
        } catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分配角色
     */
    @PutMapping("/{id}/roles")
    @Operation(summary = "分配角色", description = "为用户分配角色")
    public Result<String> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody List<Long> roleIds) {
        log.info("分配用户角色: userId={}, roleIds={}", id, roleIds);
        try {
            userService.assignRoles(id, roleIds);
            return Result.success("角色分配成功");
        } catch (Exception e) {
            log.error("分配角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "修改用户状态", description = "启用或禁用用户")
    public Result<String> changeStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        log.info("修改用户状态: id={}, status={}", id, status);
        try {
            userService.changeStatus(id, status);
            return Result.success("状态修改成功");
        } catch (Exception e) {
            log.error("修改用户状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导出用户
     */
    @GetMapping("/export")
    @Operation(summary = "导出用户", description = "导出用户数据")
    public Result<List<UserDTO>> export(UserQueryDTO queryDTO) {
        log.info("导出用户数据");
        try {
            List<UserDTO> users = userService.export(queryDTO);
            return Result.success("导出成功", users);
        } catch (Exception e) {
            log.error("导出用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户角色列表
     */
    @GetMapping("/{id}/roles")
    @Operation(summary = "获取用户角色", description = "获取用户角色列表")
    public Result<List<Long>> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("获取用户角色: {}", id);
        try {
            List<Long> roleIds = userService.getUserRoles(id);
            return Result.success("查询成功", roleIds);
        } catch (Exception e) {
            log.error("获取用户角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查用户名是否唯一
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名唯一性", description = "检查用户名是否唯一")
    public Result<Boolean> checkUsernameUnique(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        try {
            boolean unique = userService.checkUsernameUnique(username, userId);
            return Result.success("检查完成", unique);
        } catch (Exception e) {
            log.error("检查用户名唯一性失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查邮箱是否唯一
     */
    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱唯一性", description = "检查邮箱是否唯一")
    public Result<Boolean> checkEmailUnique(
            @Parameter(description = "邮箱") @RequestParam String email,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        try {
            boolean unique = userService.checkEmailUnique(email, userId);
            return Result.success("检查完成", unique);
        } catch (Exception e) {
            log.error("检查邮箱唯一性失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查手机号是否唯一
     */
    @GetMapping("/check-phone")
    @Operation(summary = "检查手机号唯一性", description = "检查手机号是否唯一")
    public Result<Boolean> checkPhoneUnique(
            @Parameter(description = "手机号") @RequestParam String phone,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        try {
            boolean unique = userService.checkPhoneUnique(phone, userId);
            return Result.success("检查完成", unique);
        } catch (Exception e) {
            log.error("检查手机号唯一性失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据用户名查询用户（用于 Feign 调用）
     */
    @GetMapping("/by-username")
    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户信息")
    public Result<UserDTO> getByUsername(@Parameter(description = "用户名") @RequestParam String username) {
        log.info("根据用户名查询用户: {}", username);
        try {
            UserDTO user = userService.getByUsername(username);
            return Result.success("查询成功", user);
        } catch (Exception e) {
            log.error("根据用户名查询用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据手机号查询用户（用于 Feign 调用）
     */
    @GetMapping("/by-phone")
    @Operation(summary = "根据手机号查询用户", description = "根据手机号查询用户信息")
    public Result<UserDTO> getByPhone(@Parameter(description = "手机号") @RequestParam String phone) {
        log.info("根据手机号查询用户: {}", phone);
        try {
            UserDTO user = userService.getByPhone(phone);
            return Result.success("查询成功", user);
        } catch (Exception e) {
            log.error("根据手机号查询用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据邮箱查询用户（用于 Feign 调用）
     */
    @GetMapping("/by-email")
    @Operation(summary = "根据邮箱查询用户", description = "根据邮箱查询用户信息")
    public Result<UserDTO> getByEmail(@Parameter(description = "邮箱") @RequestParam String email) {
        log.info("根据邮箱查询用户: {}", email);
        try {
            UserDTO user = userService.getByEmail(email);
            return Result.success("查询成功", user);
        } catch (Exception e) {
            log.error("根据邮箱查询用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量查询用户（用于 Feign 调用）
     */
    @GetMapping("/batch")
    @Operation(summary = "批量查询用户", description = "根据用户ID列表批量查询用户信息")
    public Result<List<UserDTO>> getBatchByIds(@Parameter(description = "用户ID列表（逗号分隔）") @RequestParam String userIds) {
        log.info("批量查询用户: {}", userIds);
        try {
            String[] idArray = userIds.split(",");
            List<Long> ids = new java.util.ArrayList<>();
            for (String id : idArray) {
                ids.add(Long.parseLong(id.trim()));
            }
            List<UserDTO> users = userService.getBatchByIds(ids);
            return Result.success("查询成功", users);
        } catch (Exception e) {
            log.error("批量查询用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据部门ID查询用户（用于 Feign 调用）
     */
    @GetMapping("/by-dept")
    @Operation(summary = "根据部门ID查询用户", description = "根据部门ID查询用户列表")
    public Result<List<UserDTO>> getByDeptId(@Parameter(description = "部门ID") @RequestParam Long deptId) {
        log.info("根据部门ID查询用户: {}", deptId);
        try {
            List<UserDTO> users = userService.getByDeptId(deptId);
            return Result.success("查询成功", users);
        } catch (Exception e) {
            log.error("根据部门ID查询用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
