package com.basebackend.common.masking.jackson;

import com.basebackend.common.masking.MaskingStrategyRegistry;

public final class MaskingStrategyRegistryHolder {

    private static MaskingStrategyRegistry registry;

    private MaskingStrategyRegistryHolder() {
    }

    public static void setRegistry(MaskingStrategyRegistry registry) {
        MaskingStrategyRegistryHolder.registry = registry;
    }

    public static MaskingStrategyRegistry getRegistry() {
        return registry;
    }
}
