package com.basebackend.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 登录IP
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 登录地点
     */
    @TableField("login_location")
    private String loginLocation;

    /**
     * 浏览器类型
     */
    @TableField("browser")
    private String browser;

    /**
     * 操作系统
     */
    @TableField("os")
    private String os;

    /**
     * 登录状态：0-失败，1-成功
     */
    @TableField("status")
    private Integer status;

    /**
     * 提示消息
     */
    @TableField("msg")
    private String msg;

    /**
     * 登录时间
     */
    @TableField("login_time")
    private LocalDateTime loginTime;
}
