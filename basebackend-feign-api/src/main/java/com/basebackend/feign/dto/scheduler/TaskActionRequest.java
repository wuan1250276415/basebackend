package com.basebackend.feign.dto.scheduler;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务操作请求 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
public class TaskActionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 操作用户
     */
    private String userId;

    /**
     * 操作变量
     */
    private Map<String, Object> variables;

    /**
     * 本地变量
     */
    private Map<String, Object> localVariables;

    /**
     * 任务结果或意见
     */
    private String comment;
}
