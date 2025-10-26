package com.basebackend.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件操作日志实体
 */
@Data
@TableName("file_operation_log")
public class FileOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 操作类型(UPLOAD/DOWNLOAD/DELETE/RECOVER/CREATE_VERSION/REVERT_VERSION)
     */
    private String operationType;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作详情
     */
    private String operationDetail;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
