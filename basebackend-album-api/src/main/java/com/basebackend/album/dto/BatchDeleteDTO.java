package com.basebackend.album.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量删除请求 DTO
 *
 * @param ids 照片ID列表
 * @author BearTeam
 */
public record BatchDeleteDTO(
        @NotEmpty(message = "照片ID列表不能为空")
        List<Long> ids
) {
}
