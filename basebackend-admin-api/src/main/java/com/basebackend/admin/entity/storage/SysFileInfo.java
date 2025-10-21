package com.basebackend.admin.entity.storage;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息表
 *
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_file_info")
public class SysFileInfo {

    /**
     * 文件ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件编号
     */
    private String fileCode;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 存储文件名
     */
    private String storedFilename;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME）
     */
    private String contentType;

    /**
     * 文件分类（file/image/large）
     */
    private String fileCategory;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传人ID
     */
    private Long uploadUserId;

    /**
     * 上传人姓名
     */
    private String uploadUsername;

    /**
     * ETag
     */
    private String etag;

    /**
     * 删除标记（0-未删除，1-已删除）
     */
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    private String createBy;
}
