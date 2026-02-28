package com.basebackend.album.vo;

import java.time.LocalDateTime;

/**
 * 家庭成员响应 VO
 *
 * @param id       成员记录ID
 * @param userId   用户ID
 * @param nickname 家庭内昵称
 * @param userName 用户名称
 * @param role     角色: 0=成员 1=管理员 2=创建者
 * @param roleName 角色名称
 * @param joinTime 加入时间
 * @author BearTeam
 */
public record FamilyMemberVO(
        Long id,
        Long userId,
        String nickname,
        String userName,
        Integer role,
        String roleName,
        LocalDateTime joinTime
) {
}
