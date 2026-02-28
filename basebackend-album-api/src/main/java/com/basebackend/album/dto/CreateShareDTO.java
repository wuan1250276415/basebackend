package com.basebackend.album.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 创建分享链接请求 DTO
 *
 * @param albumId       相册ID
 * @param password      访问密码（可选）
 * @param expireTime    过期时间（NULL=永不过期）
 * @param maxViews      最大查看次数
 * @param allowDownload 是否允许下载
 * @author BearTeam
 */
public record CreateShareDTO(
        @NotNull(message = "相册ID不能为空")
        Long albumId,

        String password,

        LocalDateTime expireTime,

        Integer maxViews,

        Boolean allowDownload
) {
}
