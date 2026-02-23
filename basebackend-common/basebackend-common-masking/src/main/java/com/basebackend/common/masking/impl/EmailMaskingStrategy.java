package com.basebackend.common.masking.impl;

import com.basebackend.common.masking.MaskingStrategy;

public class EmailMaskingStrategy implements MaskingStrategy {

    @Override
    public String mask(String value, char maskChar) {
        if (value == null || !value.contains("@")) {
            return value;
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 1) {
            return value;
        }
        return value.charAt(0)
                + String.valueOf(maskChar).repeat(atIndex - 1)
                + value.substring(atIndex);
    }
}
