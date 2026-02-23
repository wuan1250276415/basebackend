package com.basebackend.common.masking.impl;

import com.basebackend.common.masking.MaskingStrategy;

public class AddressMaskingStrategy implements MaskingStrategy {

    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.length() <= 6) {
            return value;
        }
        int prefixLen = 6;
        int maskLen = value.length() - prefixLen;
        return value.substring(0, prefixLen)
                + String.valueOf(maskChar).repeat(maskLen);
    }
}
