package com.basebackend.album.dto;

/**
 * 访问分享请求 DTO
 *
 * @param password 访问密码（可选）
 * @author BearTeam
 */
public record ShareAccessDTO(
        String password
) {
}
