package com.basebackend.album.vo;

import java.time.LocalDateTime;

/**
 * 分享链接响应 VO
 *
 * @param id            分享ID
 * @param albumId       相册ID
 * @param albumName     相册名称
 * @param shareCode     分享码
 * @param shareUrl      完整分享URL
 * @param hasPassword   是否有密码
 * @param expireTime    过期时间
 * @param maxViews      最大查看次数
 * @param viewCount     已查看次数
 * @param allowDownload 是否允许下载
 * @param status        状态: 0=已失效 1=有效
 * @param createTime    创建时间
 * @author BearTeam
 */
public record ShareLinkVO(
        Long id,
        Long albumId,
        String albumName,
        String shareCode,
        String shareUrl,
        Boolean hasPassword,
        LocalDateTime expireTime,
        Integer maxViews,
        Integer viewCount,
        Boolean allowDownload,
        Integer status,
        LocalDateTime createTime
) {
}
