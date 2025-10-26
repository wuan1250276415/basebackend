package com.basebackend.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分享实体
 */
@Data
@TableName("file_share")
public class FileShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 分享码
     */
    private String shareCode;

    /**
     * 分享密码
     */
    private String sharePassword;

    /**
     * 分享人ID
     */
    private Long sharedBy;

    /**
     * 分享人名称
     */
    private String sharedByName;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 下载次数限制
     */
    private Integer downloadLimit;

    /**
     * 已下载次数
     */
    private Integer downloadCount;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 是否允许下载
     */
    private Boolean allowDownload;

    /**
     * 是否允许预览
     */
    private Boolean allowPreview;

    /**
     * 分享状态(ACTIVE/EXPIRED/CANCELLED)
     */
    private String status;

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
