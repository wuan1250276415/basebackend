package com.basebackend.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * JWT 审计日志条目 — 记录单次 Token 相关事件的完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuditEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private JwtAuditEvent eventType;

    /**
     * 用户标识（userId 或 subject）
     */
    private String userId;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 客户端 IP 地址
     */
    private String ip;

    /**
     * User-Agent 字符串
     */
    private String userAgent;

    /**
     * Token JTI
     */
    private String tokenJti;

    /**
     * 事件时间戳（毫秒）
     */
    private long timestamp;

    /**
     * 额外信息
     */
    private Map<String, Object> details;
}
