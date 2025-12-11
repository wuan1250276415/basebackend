package com.basebackend.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.feign.dto.log.UserOperationLogDTO;
import com.basebackend.system.entity.UserOperationLog;
import com.basebackend.system.mapper.UserOperationLogMapper;
import com.basebackend.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户操作日志服务实现类
 *
 * @author BaseBackend
 * @since 2025-12-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final UserOperationLogMapper operationLogMapper;

    @Override
    public List<UserOperationLogDTO> getUserOperationLogs(Long userId, Integer limit) {
        log.debug("获取用户操作日志: userId={}, limit={}", userId, limit);

        LambdaQueryWrapper<UserOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserOperationLog::getUserId, userId)
                .orderByDesc(UserOperationLog::getCreateTime);

        // 限制返回数量
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        } else {
            wrapper.last("LIMIT 50");
        }

        List<UserOperationLog> logs = operationLogMapper.selectList(wrapper);

        return logs.stream()
                .map(logEntity -> BeanUtil.copyProperties(logEntity, UserOperationLogDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOperationLog(UserOperationLogDTO operationLog) {
        log.debug("保存操作日志: userId={}, operationType={}",
                operationLog.getUserId(), operationLog.getOperationType());

        UserOperationLog entity = BeanUtil.copyProperties(operationLog, UserOperationLog.class);

        // 设置创建时间
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }

        operationLogMapper.insert(entity);

        log.info("操作日志保存成功: id={}", entity.getId());
    }
}
