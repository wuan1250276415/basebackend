package com.basebackend.admin.dto;

/**
 * 服务器信息DTO
 */
public record ServerInfoDTO(
    /** 服务器名称 */
    String serverName,
    /** 服务器IP */
    String serverIp,
    /** 操作系统 */
    String osName,
    /** 操作系统版本 */
    String osVersion,
    /** 系统架构 */
    String osArch,
    /** Java版本 */
    String javaVersion,
    /** Java供应商 */
    String javaVendor,
    /** JVM名称 */
    String jvmName,
    /** JVM版本 */
    String jvmVersion,
    /** JVM供应商 */
    String jvmVendor,
    /** 总内存 */
    String totalMemory,
    /** 已用内存 */
    String usedMemory,
    /** 空闲内存 */
    String freeMemory,
    /** 内存使用率 */
    String memoryUsage,
    /** 处理器数量 */
    Integer processorCount,
    /** 系统负载 */
    String systemLoad,
    /** 运行时间 */
    String uptime
) {}
