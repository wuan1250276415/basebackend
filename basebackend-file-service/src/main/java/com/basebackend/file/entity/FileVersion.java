package com.basebackend.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件版本实体
 */
@Data
@TableName("file_version")
public class FileVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件MD5值
     */
    private String md5;

    /**
     * 变更说明
     */
    private String changeDescription;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人名称
     */
    private String createdByName;

    /**
     * 是否当前版本
     */
    private Boolean isCurrent;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
