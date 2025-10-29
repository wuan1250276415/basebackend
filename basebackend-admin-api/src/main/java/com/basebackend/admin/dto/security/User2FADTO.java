package com.basebackend.admin.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户双因素认证信息 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Data
@Schema(description = "用户双因素认证信息")
public class User2FADTO {

    @Schema(description = "2FA ID")
    private Long id;

    @Schema(description = "2FA类型")
    private String type;

    @Schema(description = "是否启用")
    private Integer enabled;

    @Schema(description = "验证手机号（脱敏）")
    private String verifyPhone;

    @Schema(description = "验证邮箱（脱敏）")
    private String verifyEmail;

    @Schema(description = "最后验证时间")
    private LocalDateTime lastVerifyTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
