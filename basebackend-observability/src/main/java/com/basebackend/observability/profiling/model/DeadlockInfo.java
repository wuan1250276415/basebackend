package com.basebackend.observability.profiling.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 死锁信息模型
 */
@Data
@Builder
public class DeadlockInfo {
    
    /**
     * 涉及的线程
     */
    private List<ThreadInfo> threads;
    
    /**
     * 死锁描述
     */
    private String description;
    
    /**
     * 检测时间
     */
    private Long detectedAt;
    
    /**
     * 严重程度
     */
    private String severity;
}
