package com.basebackend.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备信息 — 描述登录设备的基本属性
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备唯一标识
     */
    private String deviceId;

    /**
     * 设备名称（如 "iPhone 15 Pro", "Chrome on Windows"）
     */
    private String deviceName;

    /**
     * 设备类型
     */
    private DeviceType deviceType;

    /**
     * 登录 IP 地址
     */
    private String ip;

    /**
     * User-Agent 字符串
     */
    private String userAgent;

    /**
     * 设备类型枚举
     */
    public enum DeviceType {
        MOBILE,
        DESKTOP,
        TABLET,
        UNKNOWN
    }
}
