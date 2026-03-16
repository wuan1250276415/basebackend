package com.basebackend.album.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 设置相册封面请求 DTO
 *
 * @param photoId 照片ID
 * @author BearTeam
 */
public record SetCoverDTO(
        @NotNull(message = "照片ID不能为空")
        Long photoId
) {
}
