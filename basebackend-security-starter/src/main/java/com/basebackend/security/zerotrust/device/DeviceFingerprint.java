package com.basebackend.security.zerotrust.device;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 设备指纹数据模型
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Builder
public class DeviceFingerprint {
    private String fingerprintHash;
    private String clientIp;
    private String userAgent;
    private String browserType;
    private String osType;
    private String deviceType;
    private String language;
    private String acceptEncoding;
    private String accept;
    private String connectionType;
    private String screenResolution;
    private String timeZone;
    private String platform;
    private String appVersion;

    // 信任度相关
    private int trustScore;
    private boolean trusted;
    private String trustedBy;
    private Instant trustedTime;

    // 访问记录
    private int accessCount;
    private Instant firstSeenTime;
    private Instant lastSeenTime;

    // 额外安全信息
    private String certificateInfo;
    private String deviceId;
    private String location;

    // Getters and Setters
    public String getFingerprintHash() { return fingerprintHash; }
    public void setFingerprintHash(String fingerprintHash) { this.fingerprintHash = fingerprintHash; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getBrowserType() { return browserType; }
    public void setBrowserType(String browserType) { this.browserType = browserType; }

    public String getOsType() { return osType; }
    public void setOsType(String osType) { this.osType = osType; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getAcceptEncoding() { return acceptEncoding; }
    public void setAcceptEncoding(String acceptEncoding) { this.acceptEncoding = acceptEncoding; }

    public String getAccept() { return accept; }
    public void setAccept(String accept) { this.accept = accept; }

    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

    public String getScreenResolution() { return screenResolution; }
    public void setScreenResolution(String screenResolution) { this.screenResolution = screenResolution; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public int getTrustScore() { return trustScore; }
    public void setTrustScore(int trustScore) { this.trustScore = trustScore; }

    public boolean isTrusted() { return trusted; }
    public void setTrusted(boolean trusted) { this.trusted = trusted; }

    public String getTrustedBy() { return trustedBy; }
    public void setTrustedBy(String trustedBy) { this.trustedBy = trustedBy; }

    public Instant getTrustedTime() { return trustedTime; }
    public void setTrustedTime(Instant trustedTime) { this.trustedTime = trustedTime; }

    public int getAccessCount() { return accessCount; }
    public void setAccessCount(int accessCount) { this.accessCount = accessCount; }

    public Instant getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Instant firstSeenTime) { this.firstSeenTime = firstSeenTime; }

    public Instant getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Instant lastSeenTime) { this.lastSeenTime = lastSeenTime; }

    public String getCertificateInfo() { return certificateInfo; }
    public void setCertificateInfo(String certificateInfo) { this.certificateInfo = certificateInfo; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
