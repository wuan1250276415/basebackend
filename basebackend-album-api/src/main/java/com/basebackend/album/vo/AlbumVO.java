package com.basebackend.album.vo;

import java.time.LocalDateTime;

/**
 * 相册响应 VO
 *
 * @param id           相册ID
 * @param name         相册名称
 * @param description  描述
 * @param coverUrl     封面图片URL
 * @param familyId     所属家庭ID
 * @param familyName   所属家庭名称
 * @param ownerId      创建者ID
 * @param ownerName    创建者名称
 * @param type         类型: 0=普通 1=时间轴 2=智能
 * @param visibility   可见性: 0=私有 1=家庭 2=公开
 * @param photoCount   照片数量
 * @param createTime   创建时间
 * @param updateTime   更新时间
 * @author BearTeam
 */
public record AlbumVO(
        Long id,
        String name,
        String description,
        String coverUrl,
        Long familyId,
        String familyName,
        Long ownerId,
        String ownerName,
        Integer type,
        Integer visibility,
        Integer photoCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
