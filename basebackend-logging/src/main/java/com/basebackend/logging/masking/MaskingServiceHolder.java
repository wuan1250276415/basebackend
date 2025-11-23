package com.basebackend.logging.masking;

/**
 * 脱敏服务静态Holder
 *
 * 用于在Logback转换器中获取脱敏服务实例。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public final class MaskingServiceHolder {

    /**
     * 脱敏服务实例
     */
    private static volatile PiiMaskingService service;

    /**
     * 私有构造器
     */
    private MaskingServiceHolder() {
    }

    /**
     * 设置服务实例
     */
    public static void set(PiiMaskingService svc) {
        service = svc;
    }

    /**
     * 获取服务实例
     */
    public static PiiMaskingService get() {
        return service;
    }
}
