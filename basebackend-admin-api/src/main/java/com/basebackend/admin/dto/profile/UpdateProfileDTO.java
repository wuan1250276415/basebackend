package com.basebackend.admin.dto.profile;

import com.basebackend.common.validation.SafeString;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 更新个人资料 DTO
 *
 * @author Claude Code
 * @since 2025-10-29
 */
@Schema(description = "更新个人资料请求")
public record UpdateProfileDTO(
    @SafeString @Size(max = 50, message = "昵称长度不能超过50个字符") @Schema(description = "昵称") String nickname,
    @Email(message = "邮箱格式不正确") @Size(max = 100, message = "邮箱长度不能超过100个字符") @Schema(description = "邮箱") String email,
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") @Schema(description = "手机号") String phone,
    @Size(max = 255, message = "头像URL长度不能超过255个字符") @Schema(description = "头像URL") String avatar,
    @Schema(description = "性别: 0-未知, 1-男, 2-女") Integer gender,
    @Schema(description = "生日") LocalDate birthday
) {}
