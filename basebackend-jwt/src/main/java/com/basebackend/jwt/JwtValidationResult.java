package com.basebackend.jwt;

import io.jsonwebtoken.Claims;
import lombok.Getter;

/**
 * Token 验证结果 — 供不想处理异常的调用方使用
 */
@Getter
public class JwtValidationResult {

    private final boolean valid;
    private final Claims claims;
    private final JwtException.ErrorType errorType;
    private final String errorMessage;

    private JwtValidationResult(boolean valid, Claims claims,
                                JwtException.ErrorType errorType, String errorMessage) {
        this.valid = valid;
        this.claims = claims;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public static JwtValidationResult success(Claims claims) {
        return new JwtValidationResult(true, claims, null, null);
    }

    public static JwtValidationResult failure(JwtException.ErrorType errorType, String message) {
        return new JwtValidationResult(false, null, errorType, message);
    }
}
