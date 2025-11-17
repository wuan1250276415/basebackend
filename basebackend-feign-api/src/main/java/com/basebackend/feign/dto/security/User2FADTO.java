package com.basebackend.feign.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户2FA配置DTO
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Data
@Schema(name = "User2FA", description = "用户双因素认证配置")
public class User2FADTO {

    @Schema(description = "是否启用2FA：0-禁用，1-启用")
    private Integer enabled;

    @Schema(description = "2FA类型：sms-短信，email-邮箱，totp-TOTP")
    private String type;

    @Schema(description = "验证手机号（脱敏）")
    private String verifyPhone;

    @Schema(description = "验证邮箱（脱敏）")
    private String verifyEmail;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
