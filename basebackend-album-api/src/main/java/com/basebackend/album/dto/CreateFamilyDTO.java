package com.basebackend.album.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建家庭组请求 DTO
 *
 * @param name        家庭名称
 * @param description 描述
 * @param avatar      家庭头像URL
 * @author BearTeam
 */
public record CreateFamilyDTO(
        @NotBlank(message = "家庭名称不能为空")
        @Size(max = 100, message = "家庭名称不能超过100字符")
        String name,

        @Size(max = 500, message = "描述不能超过500字符")
        String description,

        String avatar
) {
}
