package com.basebackend.album.vo;

import java.time.LocalDateTime;

/**
 * 家庭组响应 VO
 *
 * @param id              家庭ID
 * @param name            家庭名称
 * @param description     描述
 * @param avatar          头像URL
 * @param ownerId         创建者ID
 * @param ownerName       创建者名称
 * @param inviteCode      邀请码
 * @param memberCount     当前成员数
 * @param maxMembers      最大成员数
 * @param maxStorageGb    最大存储空间(GB)
 * @param usedStorageBytes 已用存储(字节)
 * @param currentUserRole 当前用户在该家庭中的角色
 * @param createTime      创建时间
 * @author BearTeam
 */
public record FamilyGroupVO(
        Long id,
        String name,
        String description,
        String avatar,
        Long ownerId,
        String ownerName,
        String inviteCode,
        Integer memberCount,
        Integer maxMembers,
        Integer maxStorageGb,
        Long usedStorageBytes,
        Integer currentUserRole,
        LocalDateTime createTime
) {
}
