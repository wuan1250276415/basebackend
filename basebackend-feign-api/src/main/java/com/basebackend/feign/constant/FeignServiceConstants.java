package com.basebackend.feign.constant;

/**
 * Feign 服务名称常量
 *
 * @author Claude Code
 * @since 2025-11-08
 */
public class FeignServiceConstants {

    /**
     * 管理后台服务
     */
    public static final String SYS_SERVICE = "basebackend-system-api";

    /**
     * 文件服务
     */
    public static final String FILE_SERVICE = "file-service";

    /**
     * 调度服务
     */
    public static final String SCHEDULER_SERVICE = "scheduler-service";

    /**
     * 网关服务
     */
    public static final String GATEWAY_SERVICE = "gateway";

    /**
     * 演示服务
     */
    public static final String DEMO_SERVICE = "demo-api";

    private FeignServiceConstants() {
    }
}
