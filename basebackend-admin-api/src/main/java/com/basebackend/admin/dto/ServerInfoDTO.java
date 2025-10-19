package com.basebackend.admin.dto;

import lombok.Data;

/**
 * 服务器信息DTO
 */
@Data
public class ServerInfoDTO {

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 服务器IP
     */
    private String serverIp;

    /**
     * 操作系统
     */
    private String osName;

    /**
     * 操作系统版本
     */
    private String osVersion;

    /**
     * 系统架构
     */
    private String osArch;

    /**
     * Java版本
     */
    private String javaVersion;

    /**
     * Java供应商
     */
    private String javaVendor;

    /**
     * JVM名称
     */
    private String jvmName;

    /**
     * JVM版本
     */
    private String jvmVersion;

    /**
     * JVM供应商
     */
    private String jvmVendor;

    /**
     * 总内存
     */
    private String totalMemory;

    /**
     * 已用内存
     */
    private String usedMemory;

    /**
     * 空闲内存
     */
    private String freeMemory;

    /**
     * 内存使用率
     */
    private String memoryUsage;

    /**
     * 处理器数量
     */
    private Integer processorCount;

    /**
     * 系统负载
     */
    private String systemLoad;

    /**
     * 运行时间
     */
    private String uptime;
}
