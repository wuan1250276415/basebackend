package com.basebackend.common.masking.impl;

import com.basebackend.common.masking.MaskingStrategy;

public class IdCardMaskingStrategy implements MaskingStrategy {

    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.length() < 7) {
            return value;
        }
        int prefixLen = 3;
        int suffixLen = 4;
        int maskLen = value.length() - prefixLen - suffixLen;
        return value.substring(0, prefixLen)
                + String.valueOf(maskChar).repeat(Math.max(0, maskLen))
                + value.substring(value.length() - suffixLen);
    }
}
