package com.basebackend.common.masking.impl;

import com.basebackend.common.masking.MaskingStrategy;

public class BankCardMaskingStrategy implements MaskingStrategy {

    @Override
    public String mask(String value, char maskChar) {
        if (value == null || value.length() <= 4) {
            return value;
        }
        int suffixLen = 4;
        int maskLen = value.length() - suffixLen;
        return String.valueOf(maskChar).repeat(maskLen)
                + value.substring(value.length() - suffixLen);
    }
}
