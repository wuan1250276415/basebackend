package com.basebackend.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户双因素认证实体类
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Data
@TableName("user_2fa")
public class User2FA {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 2FA类型（totp, sms, email）
     */
    private String type;

    /**
     * 密钥（TOTP使用）
     */
    private String secretKey;

    /**
     * 备用码（逗号分隔）
     */
    private String backupCodes;

    /**
     * 是否启用（0-未启用，1-已启用）
     */
    private Integer enabled;

    /**
     * 验证手机号（SMS使用）
     */
    private String verifyPhone;

    /**
     * 验证邮箱（Email使用）
     */
    private String verifyEmail;

    /**
     * 最后验证时间
     */
    private LocalDateTime lastVerifyTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
