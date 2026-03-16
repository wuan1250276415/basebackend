package com.basebackend.album.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 移动照片请求 DTO
 *
 * @param targetAlbumId 目标相册ID
 * @author BearTeam
 */
public record MovePhotoDTO(
        @NotNull(message = "目标相册ID不能为空")
        Long targetAlbumId
) {
}
