package com.basebackend.logging.model;

import com.basebackend.logging.annotation.OperationLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志信息
 */
@Data
public class OperationLogInfo {

    /**
     * 操作名称
     */
    private String operation;

    /**
     * 业务类型
     */
    private OperationLog.BusinessType businessType;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 返回结果
     */
    private String result;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 操作状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 执行时间（毫秒）
     */
    private Long time;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;
}
