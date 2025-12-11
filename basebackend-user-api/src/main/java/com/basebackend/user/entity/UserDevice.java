package com.basebackend.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户设备实体类
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Data
@TableName("user_device")
public class UserDevice {

    /**
     * 设备ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 设备类型（PC, Mobile, Tablet）
     */
    private String deviceType;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 浏览器信息
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 登录位置
     */
    private String location;

    /**
     * 设备指纹
     */
    private String deviceFingerprint;

    /**
     * 是否信任设备（0-否，1-是）
     */
    private Integer isTrusted;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 首次登录时间
     */
    private LocalDateTime firstLoginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
