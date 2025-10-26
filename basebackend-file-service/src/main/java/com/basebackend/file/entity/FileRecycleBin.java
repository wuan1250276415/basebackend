package com.basebackend.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收站实体
 */
@Data
@TableName("file_recycle_bin")
public class FileRecycleBin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 删除人ID
     */
    private Long deletedBy;

    /**
     * 删除人名称
     */
    private String deletedByName;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 过期时间(自动清理时间)
     */
    private LocalDateTime expireAt;

    /**
     * 原始元数据(JSON格式)
     */
    private String originalMetadata;

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
