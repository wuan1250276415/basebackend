package com.basebackend.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.LoginLogDTO;
import com.basebackend.admin.dto.OperationLogDTO;
import com.basebackend.admin.entity.SysLoginLog;
import com.basebackend.admin.entity.SysOperationLog;
import com.basebackend.admin.mapper.SysLoginLogMapper;
import com.basebackend.admin.mapper.SysOperationLogMapper;
import com.basebackend.admin.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperationLogMapper operationLogMapper;

    @Override
    public Page<LoginLogDTO> getLoginLogPage(String username, String ipAddress, Integer status, String beginTime, String endTime, int current, int size) {
        log.info("分页查询登录日志: current={}, size={}", current, size);

        Page<SysLoginLog> page = new Page<>(current, size);
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (StrUtil.isNotBlank(username)) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        if (StrUtil.isNotBlank(ipAddress)) {
            wrapper.like(SysLoginLog::getIpAddress, ipAddress);
        }
        if (status != null) {
            wrapper.eq(SysLoginLog::getStatus, status);
        }
        if (StrUtil.isNotBlank(beginTime)) {
            wrapper.ge(SysLoginLog::getLoginTime, beginTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(SysLoginLog::getLoginTime, endTime);
        }

        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        Page<SysLoginLog> loginLogPage = loginLogMapper.selectPage(page, wrapper);

        // 转换为DTO
        List<LoginLogDTO> loginLogDTOs = loginLogPage.getRecords().stream()
                .map(this::convertToLoginLogDTO)
                .collect(Collectors.toList());

        Page<LoginLogDTO> result = new Page<>(current, size);
        result.setRecords(loginLogDTOs);
        result.setTotal(loginLogPage.getTotal());
        result.setPages(loginLogPage.getPages());

        return result;
    }

    @Override
    public Page<OperationLogDTO> getOperationLogPage(String username, String operation, Integer status, String beginTime, String endTime, int current, int size) {
        log.info("分页查询操作日志: current={}, size={}", current, size);

        Page<SysOperationLog> page = new Page<>(current, size);
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (StrUtil.isNotBlank(username)) {
            wrapper.like(SysOperationLog::getUsername, username);
        }
        if (StrUtil.isNotBlank(operation)) {
            wrapper.like(SysOperationLog::getOperation, operation);
        }
        if (status != null) {
            wrapper.eq(SysOperationLog::getStatus, status);
        }
        if (StrUtil.isNotBlank(beginTime)) {
            wrapper.ge(SysOperationLog::getOperationTime, beginTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(SysOperationLog::getOperationTime, endTime);
        }

        wrapper.orderByDesc(SysOperationLog::getOperationTime);
        Page<SysOperationLog> operationLogPage = operationLogMapper.selectPage(page, wrapper);

        // 转换为DTO
        List<OperationLogDTO> operationLogDTOs = operationLogPage.getRecords().stream()
                .map(this::convertToOperationLogDTO)
                .collect(Collectors.toList());

        Page<OperationLogDTO> result = new Page<>(current, size);
        result.setRecords(operationLogDTOs);
        result.setTotal(operationLogPage.getTotal());
        result.setPages(operationLogPage.getPages());

        return result;
    }

    @Override
    public LoginLogDTO getLoginLogById(Long id) {
        log.info("根据ID查询登录日志: {}", id);
        SysLoginLog loginLog = loginLogMapper.selectById(id);
        if (loginLog == null) {
            throw new RuntimeException("登录日志不存在");
        }
        return convertToLoginLogDTO(loginLog);
    }

    @Override
    public OperationLogDTO getOperationLogById(Long id) {
        log.info("根据ID查询操作日志: {}", id);
        SysOperationLog operationLog = operationLogMapper.selectById(id);
        if (operationLog == null) {
            throw new RuntimeException("操作日志不存在");
        }
        return convertToOperationLogDTO(operationLog);
    }

    @Override
    @Transactional
    public void deleteLoginLog(Long id) {
        log.info("删除登录日志: {}", id);
        loginLogMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteOperationLog(Long id) {
        log.info("删除操作日志: {}", id);
        operationLogMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteLoginLogBatch(List<Long> ids) {
        log.info("批量删除登录日志: {}", ids);
        loginLogMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public void deleteOperationLogBatch(List<Long> ids) {
        log.info("批量删除操作日志: {}", ids);
        operationLogMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public void cleanLoginLog() {
        log.info("清空登录日志");
        loginLogMapper.delete(null);
    }

    @Override
    @Transactional
    public void cleanOperationLog() {
        log.info("清空操作日志");
        operationLogMapper.delete(null);
    }

    @Override
    @Transactional
    public void recordLoginLog(LoginLogDTO loginLogDTO) {
        log.info("记录登录日志: {}", loginLogDTO.getUsername());
        SysLoginLog loginLog = new SysLoginLog();
        BeanUtil.copyProperties(loginLogDTO, loginLog);
        loginLogMapper.insert(loginLog);
    }

    @Override
    @Transactional
    public void recordOperationLog(OperationLogDTO operationLogDTO) {
        log.info("记录操作日志: {}", operationLogDTO.getOperation());
        SysOperationLog operationLog = new SysOperationLog();
        BeanUtil.copyProperties(operationLogDTO, operationLog);
        operationLogMapper.insert(operationLog);
    }

    /**
     * 转换为登录日志DTO
     */
    private LoginLogDTO convertToLoginLogDTO(SysLoginLog loginLog) {
        LoginLogDTO dto = new LoginLogDTO();
        BeanUtil.copyProperties(loginLog, dto);
        return dto;
    }

    /**
     * 转换为操作日志DTO
     */
    private OperationLogDTO convertToOperationLogDTO(SysOperationLog operationLog) {
        OperationLogDTO dto = new OperationLogDTO();
        BeanUtil.copyProperties(operationLog, dto);
        return dto;
    }
}
