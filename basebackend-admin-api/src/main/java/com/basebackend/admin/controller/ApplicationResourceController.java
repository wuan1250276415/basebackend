package com.basebackend.admin.controller;

import com.basebackend.admin.dto.ApplicationResourceDTO;
import com.basebackend.admin.service.ApplicationResourceService;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 应用资源管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/application/resource")
@RequiredArgsConstructor
@Validated
@Tag(name = "应用资源管理", description = "应用资源管理相关接口")
public class ApplicationResourceController {

    private final ApplicationResourceService resourceService;
    private final JwtUtil jwtUtil;

    @GetMapping("/tree/{appId}")
    @Operation(summary = "查询应用的资源树")
    public Result<List<ApplicationResourceDTO>> getResourceTree(@PathVariable Long appId) {
        List<ApplicationResourceDTO> tree = resourceService.getResourceTree(appId);
        return Result.success(tree);
    }

    @GetMapping("/user/tree/{appId}")
    @Operation(summary = "查询用户的资源树")
    public Result<List<ApplicationResourceDTO>> getUserResourceTree(
            @PathVariable Long appId,
            HttpServletRequest request) {
        // 从Token中获取用户ID
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Claims claims = jwtUtil.getClaimsFromToken(token);
        Long userId = Long.parseLong(claims.get("userId").toString());

        List<ApplicationResourceDTO> tree = resourceService.getUserResourceTree(appId, userId);
        return Result.success(tree);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询资源")
    public Result<ApplicationResourceDTO> getResourceById(@PathVariable Long id) {
        ApplicationResourceDTO dto = resourceService.getResourceById(id);
        if (dto == null) {
            return Result.error("资源不存在");
        }
        return Result.success(dto);
    }

    @PostMapping
    @Operation(summary = "创建资源")
    public Result<Void> createResource(@Validated @RequestBody ApplicationResourceDTO dto) {
        log.info("创建资源: {}", dto.getResourceName());
        boolean success = resourceService.createResource(dto);
        return success ? Result.success() : Result.error("创建失败");
    }

    @PutMapping
    @Operation(summary = "更新资源")
    public Result<Void> updateResource(@Validated @RequestBody ApplicationResourceDTO dto) {
        log.info("更新资源: {}", dto.getId());
        boolean success = resourceService.updateResource(dto);
        return success ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资源")
    public Result<Void> deleteResource(@PathVariable Long id) {
        log.info("删除资源: {}", id);
        boolean success = resourceService.deleteResource(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "查询角色的资源ID列表")
    public Result<List<Long>> getResourceIdsByRoleId(@PathVariable Long roleId) {
        List<Long> resourceIds = resourceService.getResourceIdsByRoleId(roleId);
        return Result.success(resourceIds);
    }

    @PostMapping("/role/{roleId}/assign")
    @Operation(summary = "分配角色资源")
    public Result<Void> assignRoleResources(
            @PathVariable Long roleId,
            @RequestBody List<Long> resourceIds) {
        log.info("分配角色资源: roleId={}, resourceIds={}", roleId, resourceIds);
        boolean success = resourceService.assignRoleResources(roleId, resourceIds);
        return success ? Result.success() : Result.error("分配失败");
    }
}
