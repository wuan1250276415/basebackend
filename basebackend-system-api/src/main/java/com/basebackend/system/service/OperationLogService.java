package com.basebackend.system.service;

import com.basebackend.feign.dto.log.UserOperationLogDTO;

import java.util.List;

/**
 * 用户操作日志服务接口
 *
 * @author BaseBackend
 * @since 2025-12-11
 */
public interface OperationLogService {

    /**
     * 获取用户操作日志列表
     *
     * @param userId 用户ID
     * @param limit  返回记录数量限制
     * @return 操作日志列表
     */
    List<UserOperationLogDTO> getUserOperationLogs(Long userId, Integer limit);

    /**
     * 保存操作日志
     *
     * @param operationLog 操作日志信息
     */
    void saveOperationLog(UserOperationLogDTO operationLog);
}
