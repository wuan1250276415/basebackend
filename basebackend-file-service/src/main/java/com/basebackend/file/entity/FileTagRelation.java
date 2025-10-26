package com.basebackend.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件标签关联实体
 */
@Data
@TableName("file_tag_relation")
public class FileTagRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人名称
     */
    private String createdByName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
