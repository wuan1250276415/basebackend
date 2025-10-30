package com.basebackend.admin.controller;

import com.basebackend.admin.dto.profile.ChangePasswordDTO;
import com.basebackend.admin.dto.profile.ProfileDetailDTO;
import com.basebackend.admin.dto.profile.UpdateProfileDTO;
import com.basebackend.admin.service.ProfileService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心控制器
 *
 * @author Claude Code
 * @since 2025-10-29
 */
@Slf4j
@Tag(name = "个人中心", description = "个人资料管理相关接口")
@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

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
