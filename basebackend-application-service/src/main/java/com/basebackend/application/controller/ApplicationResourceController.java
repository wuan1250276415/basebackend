package com.basebackend.application.controller;

import com.basebackend.admin.dto.ApplicationResourceDTO;
import com.basebackend.application.service.ApplicationResourceService;
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
 * 应用资源管理控制器
 * 负责应用资源的管理和查询
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@RestController
@RequestMapping("/api/application/resources")
@RequiredArgsConstructor
@Validated
@Tag(name = "应用资源管理", description = "应用资源管理相关接口")
public class ApplicationResourceController {

    private final ApplicationResourceService resourceService;

    /**
     * 查询应用的资源树
     *
     * @param appId 应用ID
     * @return 资源树
     */
    @GetMapping("/tree/{appId}")
    @Operation(summary = "查询应用的资源树", description = "根据应用ID查询资源树")
    public Result<List<ApplicationResourceDTO>> getResourceTree(@Parameter(description = "应用ID") @PathVariable Long appId) {
        log.info("查询应用资源树: appId={}", appId);
        try {
            List<ApplicationResourceDTO> tree = resourceService.getResourceTree(appId);
            return Result.success("查询成功", tree);
        } catch (Exception e) {
            log.error("查询应用资源树失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询用户的资源树
     *
     * @param appId 应用ID
     * @param userId 用户ID
     * @return 用户资源树
     */
    @GetMapping("/user/tree/{appId}")
    @Operation(summary = "查询用户的资源树", description = "根据应用ID和用户ID查询用户的资源树")
    public Result<List<ApplicationResourceDTO>> getUserResourceTree(
            @Parameter(description = "应用ID") @PathVariable Long appId,
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        log.info("查询用户资源树: appId={}, userId={}", appId, userId);
        try {
            List<ApplicationResourceDTO> tree = resourceService.getUserResourceTree(appId, userId);
            return Result.success("查询成功", tree);
        } catch (Exception e) {
            log.error("查询用户资源树失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询资源
     *
     * @param id 资源ID
     * @return 资源详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询资源", description = "根据资源ID查询资源详情")
    public Result<ApplicationResourceDTO> getResourceById(@Parameter(description = "资源ID") @PathVariable Long id) {
        log.info("查询资源详情: id={}", id);
        try {
            ApplicationResourceDTO dto = resourceService.getResourceById(id);
            return Result.success("查询成功", dto);
        } catch (Exception e) {
            log.error("查询资源详情失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
