package com.basebackend.user.controller;

import com.basebackend.user.dto.ApplicationResourceDTO;
import com.basebackend.user.service.ApplicationResourceService;
import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用资源管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/user/application/resource")
@RequiredArgsConstructor
@Validated
@Tag(name = "应用资源管理", description = "应用资源管理相关接口")
public class ApplicationResourceController {

    private final ApplicationResourceService resourceService;
    private final JwtUtil jwtUtil;

    @GetMapping("/tree/{appId}")
    @Operation(summary = "查询应用的资源树")
    @OperationLog(operation="查询应用的资源树",businessType = BusinessType.SELECT)
    public Result<List<ApplicationResourceDTO>> getResourceTree(@PathVariable Long appId) {
        List<ApplicationResourceDTO> tree = resourceService.getResourceTree(appId);
        return Result.success(tree);
    }

    @GetMapping("/user/tree/{appId}")
    @Operation(summary = "查询用户的资源树")
    @OperationLog(operation="查询用户的资源树", businessType = BusinessType.SELECT)
    public Result<List<ApplicationResourceDTO>> getUserResourceTree(
            @PathVariable Long appId,
            HttpServletRequest request) {
        // 从Token中获取用户ID
        String token = request.getHeader(CommonConstants.TOKEN_HEADER);
        if (token != null && token.startsWith(CommonConstants.TOKEN_PREFIX)) {
            token = token.substring(CommonConstants.TOKEN_PREFIX.length());
        }

        Claims claims = jwtUtil.getClaimsFromToken(token);
        Long userId = Long.parseLong(claims.get("userId").toString());

        List<ApplicationResourceDTO> tree = resourceService.getUserResourceTree(appId, userId);
        return Result.success(tree);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询资源")
    @OperationLog(operation="根据ID查询资源", businessType = BusinessType.SELECT)
    public Result<ApplicationResourceDTO> getResourceById(@PathVariable Long id) {
        ApplicationResourceDTO dto = resourceService.getResourceById(id);
        if (dto == null) {
            return Result.error("资源不存在");
        }
        return Result.success(dto);
    }

    @PostMapping
    @Operation(summary = "创建资源")
    @OperationLog(operation="创建资源", businessType = BusinessType.INSERT)
    public Result<Void> createResource(@Validated @RequestBody ApplicationResourceDTO dto) {
        log.info("创建资源: {}", dto.getResourceName());
        boolean success = resourceService.createResource(dto);
        return success ? Result.success() : Result.error("创建失败");
    }

    @PutMapping
    @Operation(summary = "更新资源")
    @OperationLog(operation="更新资源", businessType = BusinessType.UPDATE)
    public Result<Void> updateResource(@Validated @RequestBody ApplicationResourceDTO dto) {
        log.info("更新资源: {}", dto.getId());
        boolean success = resourceService.updateResource(dto);
        return success ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资源")
    @OperationLog(operation="删除资源", businessType = BusinessType.DELETE)
    public Result<Void> deleteResource(@PathVariable Long id) {
        log.info("删除资源: {}", id);
        boolean success = resourceService.deleteResource(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "查询角色的资源ID列表")
    @OperationLog(operation="查询角色的资源ID列表", businessType = BusinessType.SELECT)
    public Result<List<Long>> getResourceIdsByRoleId(@PathVariable Long roleId) {
        List<Long> resourceIds = resourceService.getResourceIdsByRoleId(roleId);
        return Result.success(resourceIds);
    }

    @PostMapping("/role/{roleId}/assign")
    @Operation(summary = "分配角色资源")
    @OperationLog(operation="分配角色资源", businessType = BusinessType.UPDATE)
    public Result<Void> assignRoleResources(
            @PathVariable Long roleId,
            @RequestBody List<Long> resourceIds) {
        log.info("分配角色资源: roleId={}, resourceIds={}", roleId, resourceIds);
        boolean success = resourceService.assignRoleResources(roleId, resourceIds);
        return success ? Result.success() : Result.error("分配失败");
    }
}
