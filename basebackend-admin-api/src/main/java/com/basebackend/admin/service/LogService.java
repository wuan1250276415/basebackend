package com.basebackend.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.LoginLogDTO;
import com.basebackend.admin.dto.OperationLogDTO;

import java.util.List;

/**
 * 日志服务接口
 */
public interface LogService {

    /**
     * 分页查询登录日志
     */
    Page<LoginLogDTO> getLoginLogPage(String username, String ipAddress, Integer status, String beginTime, String endTime, int current, int size);

    /**
     * 分页查询操作日志
     */
    Page<OperationLogDTO> getOperationLogPage(String username, String operation, Integer status, String beginTime, String endTime, int current, int size);

    /**
     * 根据ID查询登录日志
     */
    LoginLogDTO getLoginLogById(Long id);

    /**
     * 根据ID查询操作日志
     */
    OperationLogDTO getOperationLogById(Long id);

    /**
     * 删除登录日志
     */
    void deleteLoginLog(Long id);

    /**
     * 删除操作日志
     */
    void deleteOperationLog(Long id);

    /**
     * 批量删除登录日志
     */
    void deleteLoginLogBatch(List<Long> ids);

    /**
     * 批量删除操作日志
     */
    void deleteOperationLogBatch(List<Long> ids);

    /**
     * 清空登录日志
     */
    void cleanLoginLog();

    /**
     * 清空操作日志
     */
    void cleanOperationLog();

    /**
     * 记录登录日志
     */
    void recordLoginLog(LoginLogDTO loginLogDTO);

    /**
     * 记录操作日志
     */
    void recordOperationLog(OperationLogDTO operationLogDTO);
}
