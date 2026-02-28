package com.basebackend.album.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享链接实体
 *
 * @author BearTeam
 */
@Data
@TableName("album_share_link")
public class ShareLink {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 相册ID */
    private Long albumId;

    /** 创建者ID */
    private Long creatorId;

    /** 分享码 */
    private String shareCode;

    /** 访问密码(加密) */
    private String password;

    /** 过期时间(NULL=永不过期) */
    private LocalDateTime expireTime;

    /** 最大查看次数 */
    private Integer maxViews;

    /** 已查看次数 */
    private Integer viewCount;

    /** 是否允许下载: 0=否 1=是 */
    private Integer allowDownload;

    /** 状态: 0=已失效 1=有效 */
    private Integer status;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
