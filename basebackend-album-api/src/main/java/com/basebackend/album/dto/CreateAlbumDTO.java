package com.basebackend.album.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建相册请求 DTO
 *
 * @param name       相册名称
 * @param description 描述
 * @param familyId   所属家庭ID（NULL=个人相册）
 * @param type       类型: 0=普通 1=时间轴自动 2=智能
 * @param visibility 可见性: 0=私有 1=家庭 2=链接公开
 * @author BearTeam
 */
public record CreateAlbumDTO(
        @NotBlank(message = "相册名称不能为空")
        @Size(max = 200, message = "相册名称不能超过200字符")
        String name,

        @Size(max = 1000, message = "描述不能超过1000字符")
        String description,

        Long familyId,

        Integer type,

        Integer visibility
) {
}
