package com.basebackend.common.idempotent.exception;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;

/**
 * 幂等性异常
 * <p>
 * 当检测到重复提交时抛出此异常。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class IdempotentException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public IdempotentException(String message) {
        super(CommonErrorCode.IDEMPOTENT_CHECK_FAILED, message);
    }

    public IdempotentException(String message, Throwable cause) {
        super(CommonErrorCode.IDEMPOTENT_CHECK_FAILED, message, cause);
    }
}
