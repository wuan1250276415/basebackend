package com.basebackend.observability.logging.service;

import com.basebackend.observability.entity.ExceptionAggregation;
import com.basebackend.observability.mapper.ExceptionAggregationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常聚合服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionAggregationService {

    private final ExceptionAggregationMapper aggregationMapper;

    /**
     * 记录异常
     */
    @Transactional
    public void recordException(
            String exceptionClass,
            String exceptionMessage,
            String stackTrace,
            String serviceName,
            String logId) {
        
        try {
            // 1. 计算堆栈哈希
            String stackTraceHash = calculateStackTraceHash(stackTrace);
            
            // 2. 查找是否已存在
            ExceptionAggregation existing = aggregationMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExceptionAggregation>()
                            .eq(ExceptionAggregation::getStackTraceHash, stackTraceHash)
            );
            
            if (existing != null) {
                // 3. 更新现有记录
                existing.setOccurrenceCount(existing.getOccurrenceCount() + 1);
                existing.setLastSeen(LocalDateTime.now());
                aggregationMapper.updateById(existing);
            } else {
                // 4. 创建新记录
                ExceptionAggregation newRecord = new ExceptionAggregation();
                newRecord.setExceptionClass(exceptionClass);
                newRecord.setExceptionMessage(truncate(exceptionMessage, 500));
                newRecord.setStackTraceHash(stackTraceHash);
                newRecord.setOccurrenceCount(1L);
                newRecord.setFirstSeen(LocalDateTime.now());
                newRecord.setLastSeen(LocalDateTime.now());
                newRecord.setSampleLogId(logId);
                newRecord.setServiceName(serviceName);
                newRecord.setStatus("NEW");
                newRecord.setSeverity(determineSeverity(exceptionClass));
                
                aggregationMapper.insert(newRecord);
            }
            
        } catch (Exception e) {
            log.error("Failed to record exception", e);
        }
    }

    /**
     * 计算堆栈哈希
     */
    private String calculateStackTraceHash(String stackTrace) {
        try {
            // 归一化堆栈跟踪（去除行号和变量名）
            String normalized = normalizeStackTrace(stackTrace);
            
            // 计算SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            
            // 转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            log.error("Failed to calculate stack trace hash", e);
            return String.valueOf(stackTrace.hashCode());
        }
    }

    /**
     * 归一化堆栈跟踪
     */
    private String normalizeStackTrace(String stackTrace) {
        if (stackTrace == null) return "";
        
        // 去除行号
        String normalized = stackTrace.replaceAll(":\\d+\\)", ")");
        
        // 去除文件名
        normalized = normalized.replaceAll("\\([^:)]+\\.java:\\d+\\)", "()");
        
        // 去除变量值
        normalized = normalized.replaceAll("\\$\\d+", "");
        
        // 只保留前20行
        String[] lines = normalized.split("\n");
        int limit = Math.min(20, lines.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            sb.append(lines[i]).append("\n");
        }
        
        return sb.toString().trim();
    }

    /**
     * 确定严重程度
     */
    private String determineSeverity(String exceptionClass) {
        if (exceptionClass == null) return "MEDIUM";
        
        String lower = exceptionClass.toLowerCase();
        
        // 严重异常
        if (lower.contains("outofmemory") || 
            lower.contains("stackoverflowerror") ||
            lower.contains("deadlock")) {
            return "CRITICAL";
        }
        
        // 高优先级异常
        if (lower.contains("nullpointer") ||
            lower.contains("illegalstate") ||
            lower.contains("illegalargument")) {
            return "HIGH";
        }
        
        // 低优先级异常
        if (lower.contains("notfound") ||
            lower.contains("validation")) {
            return "LOW";
        }
        
        return "MEDIUM";
    }

    /**
     * 获取Top N异常
     */
    public List<ExceptionAggregation> getTopExceptions(int limit, int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return aggregationMapper.selectTopExceptions(limit, startTime);
    }

    /**
     * 获取异常趋势
     */
    public List<ExceptionAggregation> getExceptionTrend(String stackTraceHash) {
        return aggregationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExceptionAggregation>()
                        .eq(ExceptionAggregation::getStackTraceHash, stackTraceHash)
                        .orderByDesc(ExceptionAggregation::getLastSeen)
        );
    }

    /**
     * 更新异常状态
     */
    @Transactional
    public void updateExceptionStatus(Long id, String status) {
        ExceptionAggregation aggregation = aggregationMapper.selectById(id);
        if (aggregation != null) {
            aggregation.setStatus(status);
            aggregationMapper.updateById(aggregation);
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
