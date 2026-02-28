package com.basebackend.album.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 上传照片请求 DTO
 *
 * @param albumId     所属相册ID
 * @param description 描述
 * @param tags        标签(逗号分隔)
 * @author BearTeam
 */
public record UploadPhotoDTO(
        @NotNull(message = "相册ID不能为空")
        Long albumId,

        @Size(max = 1000, message = "描述不能超过1000字符")
        String description,

        @Size(max = 500, message = "标签不能超过500字符")
        String tags
) {
}
