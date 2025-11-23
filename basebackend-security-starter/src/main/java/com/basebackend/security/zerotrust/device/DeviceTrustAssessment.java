package com.basebackend.security.zerotrust.device;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 设备信任评估结果
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Builder
public class DeviceTrustAssessment {
    private DeviceFingerprint deviceFingerprint;
    private int trustScore;
    private TrustLevel riskLevel;
    private boolean requiresVerification;
    private boolean isKnownDevice;
    private Instant firstSeenTime;
    private Instant lastSeenTime;
    private int accessCount;

    // 风险评估详细信息
    private List<String> riskFactors;
    private List<String> trustFactors;
    private String reason;

    // 建议操作
    private String suggestedAction;
    private List<String> requiredVerifications;

    // 额外信息
    private String location;
    private String devicePattern;
    private long daysSinceFirstSeen;

    // Manual getters and setters
    public DeviceFingerprint getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(DeviceFingerprint deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public int getTrustScore() { return trustScore; }
    public void setTrustScore(int trustScore) { this.trustScore = trustScore; }

    public TrustLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(TrustLevel riskLevel) { this.riskLevel = riskLevel; }

    public boolean isRequiresVerification() { return requiresVerification; }
    public void setRequiresVerification(boolean requiresVerification) { this.requiresVerification = requiresVerification; }

    public boolean isKnownDevice() { return isKnownDevice; }
    public void setKnownDevice(boolean isKnownDevice) { this.isKnownDevice = isKnownDevice; }

    public Instant getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Instant firstSeenTime) { this.firstSeenTime = firstSeenTime; }

    public Instant getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Instant lastSeenTime) { this.lastSeenTime = lastSeenTime; }

    public int getAccessCount() { return accessCount; }
    public void setAccessCount(int accessCount) { this.accessCount = accessCount; }

    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

    public List<String> getTrustFactors() { return trustFactors; }
    public void setTrustFactors(List<String> trustFactors) { this.trustFactors = trustFactors; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public List<String> getRequiredVerifications() { return requiredVerifications; }
    public void setRequiredVerifications(List<String> requiredVerifications) { this.requiredVerifications = requiredVerifications; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDevicePattern() { return devicePattern; }
    public void setDevicePattern(String devicePattern) { this.devicePattern = devicePattern; }

    public long getDaysSinceFirstSeen() { return daysSinceFirstSeen; }
    public void setDaysSinceFirstSeen(long daysSinceFirstSeen) { this.daysSinceFirstSeen = daysSinceFirstSeen; }
}
