package com.basebackend.common.lock.exception;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;

/**
 * 锁获取异常
 * <p>
 * 当无法在指定时间内获取分布式锁时抛出此异常。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class LockAcquisitionException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public LockAcquisitionException(String message) {
        super(CommonErrorCode.TOO_MANY_REQUESTS, message);
    }

    public LockAcquisitionException(String message, Throwable cause) {
        super(CommonErrorCode.TOO_MANY_REQUESTS, message, cause);
    }
}
