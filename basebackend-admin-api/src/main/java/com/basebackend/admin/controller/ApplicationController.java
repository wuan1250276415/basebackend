package com.basebackend.admin.controller;

import com.basebackend.admin.dto.ApplicationDTO;
import com.basebackend.admin.service.ApplicationService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/application")
@RequiredArgsConstructor
@Validated
@Tag(name = "应用管理", description = "应用管理相关接口")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/list")
    @Operation(summary = "查询应用列表")
    public Result<List<ApplicationDTO>> listApplications() {
        List<ApplicationDTO> list = applicationService.listApplications();
        return Result.success(list);
    }

    @GetMapping("/enabled")
    @Operation(summary = "查询启用的应用列表")
    public Result<List<ApplicationDTO>> listEnabledApplications() {
        List<ApplicationDTO> list = applicationService.listEnabledApplications();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询应用")
    public Result<ApplicationDTO> getApplicationById(@PathVariable Long id) {
        ApplicationDTO dto = applicationService.getApplicationById(id);
        if (dto == null) {
            return Result.error("应用不存在");
        }
        return Result.success(dto);
    }

    @GetMapping("/code/{appCode}")
    @Operation(summary = "根据编码查询应用")
    public Result<ApplicationDTO> getApplicationByCode(@PathVariable String appCode) {
        ApplicationDTO dto = applicationService.getApplicationByCode(appCode);
        if (dto == null) {
            return Result.error("应用不存在");
        }
        return Result.success(dto);
    }

    @PostMapping
    @Operation(summary = "创建应用")
    public Result<Void> createApplication(@Validated @RequestBody ApplicationDTO dto) {
        log.info("创建应用: {}", dto.getAppName());
        boolean success = applicationService.createApplication(dto);
        return success ? Result.success() : Result.error("创建失败");
    }

    @PutMapping
    @Operation(summary = "更新应用")
    public Result<Void> updateApplication(@Validated @RequestBody ApplicationDTO dto) {
        log.info("更新应用: {}", dto.getId());
        boolean success = applicationService.updateApplication(dto);
        return success ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除应用")
    public Result<Void> deleteApplication(@PathVariable Long id) {
        log.info("删除应用: {}", id);
        boolean success = applicationService.deleteApplication(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    @PutMapping("/{id}/status/{status}")
    @Operation(summary = "启用/禁用应用")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        log.info("修改应用状态: id={}, status={}", id, status);
        boolean success = applicationService.updateStatus(id, status);
        return success ? Result.success() : Result.error("操作失败");
    }
}
