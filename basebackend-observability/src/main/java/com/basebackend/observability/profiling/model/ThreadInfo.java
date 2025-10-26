package com.basebackend.observability.profiling.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 线程信息模型
 */
@Data
@Builder
public class ThreadInfo {
    
    /**
     * 线程ID
     */
    private Long threadId;
    
    /**
     * 线程名
     */
    private String threadName;
    
    /**
     * 线程状态
     */
    private String state;
    
    /**
     * CPU时间（纳秒）
     */
    private Long cpuTime;
    
    /**
     * 用户时间（纳秒）
     */
    private Long userTime;
    
    /**
     * 阻塞次数
     */
    private Long blockedCount;
    
    /**
     * 阻塞时间（毫秒）
     */
    private Long blockedTime;
    
    /**
     * 等待次数
     */
    private Long waitedCount;
    
    /**
     * 等待时间（毫秒）
     */
    private Long waitedTime;
    
    /**
     * 锁名称
     */
    private String lockName;
    
    /**
     * 锁拥有者线程ID
     */
    private Long lockOwnerId;
    
    /**
     * 堆栈跟踪
     */
    private List<String> stackTrace;
    
    /**
     * 是否守护线程
     */
    private Boolean daemon;
    
    /**
     * 优先级
     */
    private Integer priority;
}
