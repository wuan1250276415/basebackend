package com.basebackend.observability.trace.model;

/**
 * 瓶颈类型枚举
 */
public enum BottleneckType {
    
    /** 单个Span耗时过长 */
    SLOW_SPAN,
    
    /** N+1查询问题 */
    N_PLUS_ONE_QUERY,
    
    /** 串行调用 */
    SERIAL_CALLS,
    
    /** 外部服务超时 */
    EXTERNAL_SERVICE_TIMEOUT,
    
    /** 数据库查询慢 */
    SLOW_DATABASE_QUERY,
    
    /** 缓存未命中 */
    CACHE_MISS,
    
    /** CPU密集计算 */
    CPU_INTENSIVE
}
