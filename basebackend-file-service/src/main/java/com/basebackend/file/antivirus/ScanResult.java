package com.basebackend.file.antivirus;

import lombok.Data;

/**
 * 病毒扫描结果
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class ScanResult {

    /**
     * 是否安全（无病毒）
     */
    private boolean safe;

    /**
     * 检测到的威胁名称
     */
    private String threatName;

    /**
     * 扫描消息
     */
    private String message;

    /**
     * 扫描耗时（毫秒）
     */
    private long scanTimeMs;

    /**
     * 扫描引擎名称
     */
    private String engineName;

    /**
     * 创建安全结果
     */
    public static ScanResult safe(String engineName, long scanTimeMs) {
        ScanResult result = new ScanResult();
        result.setSafe(true);
        result.setThreatName(null);
        result.setMessage("文件安全");
        result.setScanTimeMs(scanTimeMs);
        result.setEngineName(engineName);
        return result;
    }

    /**
     * 创建检测到威胁的结果
     */
    public static ScanResult threat(String threatName, String engineName, long scanTimeMs) {
        ScanResult result = new ScanResult();
        result.setSafe(false);
        result.setThreatName(threatName);
        result.setMessage("检测到威胁: " + threatName);
        result.setScanTimeMs(scanTimeMs);
        result.setEngineName(engineName);
        return result;
    }

    /**
     * 创建扫描失败结果
     */
    public static ScanResult error(String message, String engineName) {
        ScanResult result = new ScanResult();
        result.setSafe(false);
        result.setThreatName(null);
        result.setMessage("扫描失败: " + message);
        result.setScanTimeMs(0);
        result.setEngineName(engineName);
        return result;
    }
}
