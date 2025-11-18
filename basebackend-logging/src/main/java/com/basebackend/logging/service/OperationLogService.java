package com.basebackend.logging.service;

import com.basebackend.logging.model.OperationLogInfo;

/**
 * 操作日志服务接口
 * 
 * 各个微服务需要实现此接口来保存操作日志
 */
public interface OperationLogService {

    /**
     * 保存操作日志
     * 
     * @param logInfo 日志信息
     */
    void saveOperationLog(OperationLogInfo logInfo);
}
