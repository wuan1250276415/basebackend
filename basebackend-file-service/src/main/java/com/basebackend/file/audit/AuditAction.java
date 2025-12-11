package com.basebackend.file.audit;

/**
 * 审计动作枚举
 *
 * 定义所有需要审计的操作类型
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
public enum AuditAction {

    /**
     * 访问分享（预览）
     */
    PREVIEW("PREVIEW", "预览分享文件"),

    /**
     * 下载分享文件
     */
    DOWNLOAD("DOWNLOAD", "下载分享文件"),

    /**
     * 密码验证失败
     */
    PASSWORD_FAIL("PASSWORD_FAIL", "密码验证失败"),

    /**
     * 密码错误次数达到冷却阈值
     */
    PASSWORD_COOLDOWN_HIT("PASSWORD_COOLDOWN_HIT", "密码错误次数达到冷却阈值"),

    /**
     * 验证码触发
     */
    CAPTCHA_TRIGGER("CAPTCHA_TRIGGER", "触发验证码"),

    /**
     * 访问限流触发
     */
    RATE_LIMIT_HIT("RATE_LIMIT_HIT", "访问限流触发"),

    /**
     * 创建分享
     */
    CREATE_SHARE("CREATE_SHARE", "创建文件分享"),

    /**
     * 更新分享
     */
    UPDATE_SHARE("UPDATE_SHARE", "更新文件分享"),

    /**
     * 删除分享
     */
    DELETE_SHARE("DELETE_SHARE", "删除文件分享"),

    /**
     * 禁用分享
     */
    DISABLE_SHARE("DISABLE_SHARE", "禁用文件分享"),

    /**
     * 启用分享
     */
    ENABLE_SHARE("ENABLE_SHARE", "启用文件分享"),

    /**
     * 权限更新
     */
    PERMISSION_UPDATE("PERMISSION_UPDATE", "更新分享权限"),

    /**
     * 系统异常
     */
    UNEXPECTED_ERROR("UNEXPECTED_ERROR", "系统异常");

    private final String code;
    private final String description;

    AuditAction(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code;
    }
}
