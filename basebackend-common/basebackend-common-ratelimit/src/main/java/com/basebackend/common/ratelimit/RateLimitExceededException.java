package com.basebackend.common.ratelimit;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;

public class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException(String message) {
        super(CommonErrorCode.TOO_MANY_REQUESTS, message);
    }
}
