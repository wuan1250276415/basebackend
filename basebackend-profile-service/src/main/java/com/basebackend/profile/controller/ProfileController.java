package com.basebackend.profile.controller;

import com.basebackend.common.model.Result;
import com.basebackend.profile.dto.preference.UpdatePreferenceDTO;
import com.basebackend.profile.dto.preference.UserPreferenceDTO;
import com.basebackend.profile.dto.profile.ChangePasswordDTO;
import com.basebackend.profile.dto.profile.ProfileDetailDTO;
import com.basebackend.profile.dto.profile.UpdateProfileDTO;
import com.basebackend.profile.service.PreferenceService;
import com.basebackend.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户档案控制器
 * 统一管理用户偏好设置和个人资料
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@Tag(name = "用户档案", description = "用户偏好设置和个人资料管理")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final PreferenceService preferenceService;
    private final ProfileService profileService;

    // ==================== 偏好设置接口 ====================

    @Operation(summary = "获取偏好设置", description = "获取当前用户的偏好设置")
    @GetMapping("/preference")
    public Result<UserPreferenceDTO> getPreference() {
        UserPreferenceDTO preference = preferenceService.getCurrentUserPreference();
        return Result.success(preference);
    }

    @Operation(summary = "更新偏好设置", description = "更新当前用户的偏好设置")
    @PutMapping("/preference")
    public Result<Void> updatePreference(@Valid @RequestBody UpdatePreferenceDTO dto) {
        preferenceService.updatePreference(dto);
        return Result.success();
    }

    // ==================== 个人资料接口 ====================

    @Operation(summary = "获取个人资料", description = "获取当前登录用户的详细资料")
    @GetMapping("/info")
    public Result<ProfileDetailDTO> getProfile() {
        ProfileDetailDTO profile = profileService.getCurrentUserProfile();
        return Result.success(profile);
    }

    @Operation(summary = "更新个人资料", description = "更新当前用户的个人资料信息")
    @PutMapping("/info")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        profileService.updateProfile(dto);
        return Result.success();
    }

    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        profileService.changePassword(dto);
        return Result.success();
    }
}
