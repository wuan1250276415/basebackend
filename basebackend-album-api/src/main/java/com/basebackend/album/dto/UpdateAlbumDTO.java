package com.basebackend.album.dto;

import jakarta.validation.constraints.Size;

/**
 * 编辑相册请求 DTO
 *
 * @param name        相册名称
 * @param description 描述
 * @param type        类型
 * @param visibility  可见性
 * @author BearTeam
 */
public record UpdateAlbumDTO(
        @Size(max = 200, message = "相册名称不能超过200字符")
        String name,

        @Size(max = 1000, message = "描述不能超过1000字符")
        String description,

        Integer type,

        Integer visibility
) {
}
