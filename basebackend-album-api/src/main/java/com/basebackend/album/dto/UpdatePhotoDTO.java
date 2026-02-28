package com.basebackend.album.dto;

import jakarta.validation.constraints.Size;

/**
 * 编辑照片信息请求 DTO
 *
 * @param description 描述
 * @param tags        标签(逗号分隔)
 * @author BearTeam
 */
public record UpdatePhotoDTO(
        @Size(max = 1000, message = "描述不能超过1000字符")
        String description,

        @Size(max = 500, message = "标签不能超过500字符")
        String tags
) {
}
