package com.basebackend.admin.controller;

import com.basebackend.admin.dto.preference.UpdatePreferenceDTO;
import com.basebackend.admin.dto.preference.UserPreferenceDTO;
import com.basebackend.admin.service.PreferenceService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户偏好设置控制器
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
@Tag(name = "用户偏好设置", description = "用户偏好配置相关接口")
@RestController
@RequestMapping("/api/admin/preference")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @Operation(summary = "获取偏好设置", description = "获取当前用户的偏好设置")
    @GetMapping
    public Result<UserPreferenceDTO> getPreference() {
        UserPreferenceDTO preference = preferenceService.getCurrentUserPreference();
        return Result.success(preference);
    }

    @Operation(summary = "更新偏好设置", description = "更新当前用户的偏好设置")
    @PutMapping
    public Result<Void> updatePreference(@Valid @RequestBody UpdatePreferenceDTO dto) {
        preferenceService.updatePreference(dto);
        return Result.success();
    }
}
