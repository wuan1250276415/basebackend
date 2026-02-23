package com.basebackend.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备会话 — 在 DeviceInfo 基础上记录会话状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备唯一标识
     */
    private String deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型
     */
    private DeviceInfo.DeviceType deviceType;

    /**
     * 登录 IP 地址
     */
    private String ip;

    /**
     * User-Agent 字符串
     */
    private String userAgent;

    /**
     * 登录时间（毫秒时间戳）
     */
    private long loginTime;

    /**
     * 最后活跃时间（毫秒时间戳）
     */
    private long lastActiveTime;

    /**
     * 关联的 Token JTI（用于踢下线时吊销 Token）
     */
    private String tokenJti;

    /**
     * 从 DeviceInfo 创建 DeviceSession
     */
    public static DeviceSession from(DeviceInfo info, String tokenJti) {
        long now = System.currentTimeMillis();
        return DeviceSession.builder()
                .deviceId(info.getDeviceId())
                .deviceName(info.getDeviceName())
                .deviceType(info.getDeviceType())
                .ip(info.getIp())
                .userAgent(info.getUserAgent())
                .loginTime(now)
                .lastActiveTime(now)
                .tokenJti(tokenJti)
                .build();
    }
}
