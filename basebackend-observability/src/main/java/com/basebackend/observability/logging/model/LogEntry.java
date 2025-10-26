package com.basebackend.observability.logging.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日志条目模型
 */
@Data
public class LogEntry {
    
    private String id;
    
    private LocalDateTime timestamp;
    
    private String level;
    
    private String service;
    
    private String message;
    
    private String logger;
    
    private String thread;
    
    private String traceId;
    
    private String spanId;
    
    private String exceptionClass;
    
    private String exceptionMessage;
    
    private String stackTrace;
    
    private Map<String, Object> fields;
}
