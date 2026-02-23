package com.basebackend.common.masking;

import java.util.EnumMap;
import java.util.Map;

public class MaskingStrategyRegistry {

    private final Map<MaskType, MaskingStrategy> strategies = new EnumMap<>(MaskType.class);

    public void register(MaskType type, MaskingStrategy strategy) {
        strategies.put(type, strategy);
    }

    public MaskingStrategy get(MaskType type) {
        return strategies.get(type);
    }
}
