package com.basebackend.security.zerotrust.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 设备指纹管理器
 *
 * 收集和分析设备特征，建立设备信任档案
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
public class DeviceFingerprintManager {

    private boolean enabled = true;
    private int timeout = 30000; // 30秒超时

    // 设备指纹缓存
    private final Map<String, DeviceFingerprint> deviceFingerprints = new ConcurrentHashMap<>();

    /**
     * 收集设备指纹
     *
     * @param request HTTP请求
     * @return 设备指纹
     */
    public DeviceFingerprint collectFingerprint(HttpServletRequest request) {
        log.debug("收集设备指纹 - IP: {}, User-Agent: {}",
            getClientIp(request), request.getHeader("User-Agent"));

        DeviceFingerprint fingerprint = DeviceFingerprint.builder()
            .clientIp(getClientIp(request))
            .userAgent(request.getHeader("User-Agent"))
            .browserType(detectBrowserType(request.getHeader("User-Agent")))
            .osType(detectOsType(request.getHeader("User-Agent")))
            .deviceType(detectDeviceType(request))
            .language(request.getHeader("Accept-Language"))
            .acceptEncoding(request.getHeader("Accept-Encoding"))
            .accept(request.getHeader("Accept"))
            .connectionType(detectConnectionType(request))
            .screenResolution(detectScreenResolution(request))
            .timeZone(request.getHeader("X-Timezone"))
            .platform(request.getHeader("X-Platform"))
            .appVersion(request.getHeader("X-App-Version"))
            .build();

        // 生成设备指纹哈希
        String fingerprintHash = generateFingerprintHash(fingerprint);
        fingerprint.setFingerprintHash(fingerprintHash);

        // 计算设备信任度
        int trustScore = calculateDeviceTrustScore(fingerprint);
        fingerprint.setTrustScore(trustScore);

        // 缓存设备指纹
        deviceFingerprints.put(fingerprintHash, fingerprint);

        log.info("设备指纹收集完成 - Hash: {}, Trust Score: {}", fingerprintHash, trustScore);

        return fingerprint;
    }

    /**
     * 评估设备信任度
     *
     * @param fingerprintHash 设备指纹哈希
     * @return 设备信任度评估结果
     */
    public DeviceTrustAssessment assessDeviceTrust(String fingerprintHash) {
        log.debug("评估设备信任度 - Hash: {}", fingerprintHash);

        DeviceFingerprint fingerprint = deviceFingerprints.get(fingerprintHash);
        if (fingerprint == null) {
            log.warn("未找到设备指纹: {}", fingerprintHash);
            return DeviceTrustAssessment.builder()
                .trustScore(0)
                .riskLevel(TrustLevel.HIGH)
                .reason("设备指纹不存在")
                .requiresVerification(true)
                .build();
        }

        // 计算信任度分数
        int trustScore = calculateDeviceTrustScore(fingerprint);
        TrustLevel riskLevel = determineRiskLevel(trustScore);

        // 检查是否需要额外验证
        boolean requiresVerification = requiresAdditionalVerification(fingerprint, trustScore);

        // 生成信任评估结果
        DeviceTrustAssessment assessment = DeviceTrustAssessment.builder()
            .deviceFingerprint(fingerprint)
            .trustScore(trustScore)
            .riskLevel(riskLevel)
            .requiresVerification(requiresVerification)
            .isKnownDevice(isKnownDevice(fingerprintHash))
            .firstSeenTime(fingerprint.getFirstSeenTime())
            .lastSeenTime(fingerprint.getLastSeenTime())
            .accessCount(fingerprint.getAccessCount())
            .build();

        // 更新设备访问记录
        updateDeviceAccessRecord(fingerprintHash);

        log.info("设备信任度评估完成 - Hash: {}, Trust Score: {}, Risk Level: {}",
            fingerprintHash, trustScore, riskLevel);

        return assessment;
    }

    /**
     * 标记设备为可信设备
     *
     * @param fingerprintHash 设备指纹哈希
     * @param userId 用户ID
     */
    public void markDeviceAsTrusted(String fingerprintHash, String userId) {
        log.info("标记设备为可信设备 - Hash: {}, User: {}", fingerprintHash, userId);

        DeviceFingerprint fingerprint = deviceFingerprints.get(fingerprintHash);
        if (fingerprint != null) {
            fingerprint.setTrusted(true);
            fingerprint.setTrustedBy(userId);
            fingerprint.setTrustedTime(java.time.Instant.now());
            fingerprint.setTrustScore(Math.max(fingerprint.getTrustScore(), 80));

            log.debug("设备已标记为可信 - Trust Score: {}", fingerprint.getTrustScore());
        }
    }

    /**
     * 检查设备是否为已知设备
     *
     * @param fingerprintHash 设备指纹哈希
     * @return true如果设备已知
     */
    private boolean isKnownDevice(String fingerprintHash) {
        DeviceFingerprint fingerprint = deviceFingerprints.get(fingerprintHash);
        return fingerprint != null && fingerprint.getAccessCount() > 1;
    }

    /**
     * 生成设备指纹哈希
     *
     * @param fingerprint 设备指纹
     * @return SHA-256哈希
     */
    private String generateFingerprintHash(DeviceFingerprint fingerprint) {
        try {
            String source = String.join("|",
                fingerprint.getClientIp(),
                fingerprint.getUserAgent(),
                fingerprint.getBrowserType(),
                fingerprint.getOsType(),
                fingerprint.getDeviceType(),
                fingerprint.getLanguage(),
                fingerprint.getScreenResolution(),
                fingerprint.getPlatform()
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            log.error("生成设备指纹哈希失败", e);
            return "unknown";
        }
    }

    /**
     * 计算设备信任度分数
     *
     * @param fingerprint 设备指纹
     * @return 信任度分数 (0-100)
     */
    private int calculateDeviceTrustScore(DeviceFingerprint fingerprint) {
        int score = 0;

        // 1. 基于访问次数
        if (fingerprint.getAccessCount() >= 10) {
            score += 40;
        } else if (fingerprint.getAccessCount() >= 5) {
            score += 30;
        } else if (fingerprint.getAccessCount() >= 2) {
            score += 20;
        }

        // 2. 基于设备信息完整性
        if (fingerprint.getUserAgent() != null && !fingerprint.getUserAgent().isEmpty()) score += 10;
        if (fingerprint.getBrowserType() != null) score += 5;
        if (fingerprint.getOsType() != null) score += 5;
        if (fingerprint.getDeviceType() != null) score += 5;

        // 3. 基于可信状态
        if (fingerprint.isTrusted()) score += 20;

        // 4. 基于时间因素（设备使用时间越长，信任度越高）
        if (fingerprint.getFirstSeenTime() != null) {
            long days = java.time.Duration.between(
                fingerprint.getFirstSeenTime(),
                java.time.Instant.now()
            ).toDays();

            if (days >= 30) {
                score += 10;
            } else if (days >= 7) {
                score += 5;
            }
        }

        return Math.min(score, 100);
    }

    /**
     * 确定风险等级
     *
     * @param trustScore 信任度分数
     * @return 风险等级
     */
    private TrustLevel determineRiskLevel(int trustScore) {
        if (trustScore >= 80) return TrustLevel.LOW;
        if (trustScore >= 60) return TrustLevel.MEDIUM;
        if (trustScore >= 40) return TrustLevel.HIGH;
        return TrustLevel.CRITICAL;
    }

    /**
     * 检查是否需要额外验证
     *
     * @param fingerprint 设备指纹
     * @param trustScore 信任度分数
     * @return true如果需要额外验证
     */
    private boolean requiresAdditionalVerification(DeviceFingerprint fingerprint, int trustScore) {
        // 如果信任度低，需要额外验证
        if (trustScore < 60) return true;

        // 如果是首次访问的新设备，需要额外验证
        if (fingerprint.getAccessCount() == 1 && !fingerprint.isTrusted()) return true;

        // 如果设备信息不完整，需要额外验证
        if (fingerprint.getUserAgent() == null || fingerprint.getUserAgent().isEmpty()) return true;

        return false;
    }

    /**
     * 更新设备访问记录
     *
     * @param fingerprintHash 设备指纹哈希
     */
    private void updateDeviceAccessRecord(String fingerprintHash) {
        DeviceFingerprint fingerprint = deviceFingerprints.get(fingerprintHash);
        if (fingerprint != null) {
            fingerprint.setAccessCount(fingerprint.getAccessCount() + 1);
            fingerprint.setLastSeenTime(java.time.Instant.now());
        }
    }

    // 辅助方法 - 检测各种设备信息
    private String detectBrowserType(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        return "Other";
    }

    private String detectOsType(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "macOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iOS")) return "iOS";
        return "Other";
    }

    private String detectDeviceType(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Tablet")) return "Tablet";
        return "Desktop";
    }

    private String detectConnectionType(HttpServletRequest request) {
        return request.getHeader("Connection");
    }

    private String detectScreenResolution(HttpServletRequest request) {
        return request.getHeader("X-Screen-Resolution");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    // Getters and Setters
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}
