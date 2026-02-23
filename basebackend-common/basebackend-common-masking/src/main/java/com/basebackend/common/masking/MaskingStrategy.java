package com.basebackend.common.masking;

@FunctionalInterface
public interface MaskingStrategy {
    String mask(String value, char maskChar);
}
