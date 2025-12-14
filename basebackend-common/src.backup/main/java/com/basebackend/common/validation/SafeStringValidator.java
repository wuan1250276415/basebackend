package com.basebackend.common.validation;

import com.basebackend.common.util.SanitizationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * {@link SafeString} 注解的校验实现
 */
public class SafeStringValidator implements ConstraintValidator<SafeString, String> {

    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{Punct}\\p{Space}]+$");

    private boolean required;
    private int maxLength;

    @Override
    public void initialize(SafeString constraintAnnotation) {
        this.required = constraintAnnotation.required();
        this.maxLength = constraintAnnotation.maxLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return !required;
        }

        if (value.length() > maxLength) {
            return false;
        }

        if (SanitizationUtils.containsUnsafeContent(value)) {
            return false;
        }

        return SAFE_TEXT_PATTERN.matcher(value).matches();
    }
}
