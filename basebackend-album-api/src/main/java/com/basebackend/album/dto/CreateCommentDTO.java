package com.basebackend.album.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建评论请求 DTO
 *
 * @param photoId  照片ID
 * @param content  评论内容
 * @param parentId 父评论ID（回复时传入）
 * @author BearTeam
 */
public record CreateCommentDTO(
        @NotNull(message = "照片ID不能为空")
        Long photoId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论内容不能超过500字符")
        String content,

        Long parentId
) {
}
