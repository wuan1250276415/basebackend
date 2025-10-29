package com.basebackend.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户操作日志实体类
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Data
@TableName("user_operation_log")
public class UserOperationLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作类型（login, logout, update_profile, change_password等）
     */
    private String operationType;

    /**
     * 操作描述
     */
    private String operationDesc;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 位置
     */
    private String location;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 操作状态（0-失败，1-成功）
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
