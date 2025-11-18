package com.basebackend.user.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码 DTO
 *
 * @author Claude Code
 * @since 2025-10-29
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordDTO {

    @NotBlank(message = "当前密码不能为空")
    @Schema(description = "当前密码", required = true)
    private String oldPassword;

    @NotBlank(message = "新密码不能为")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{6,20}$",
        message = "密码必须包含大小写字母和数字"
    )
    @Schema(description = "新密", required = true, example = "NewPass123")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认新密", required = true)
    private String confirmPassword;
}
