package com.basebackend.album.service;

import com.basebackend.album.dto.CreateFamilyDTO;
import com.basebackend.album.dto.JoinFamilyDTO;
import com.basebackend.album.dto.UpdateFamilyDTO;
import com.basebackend.album.dto.UpdateMemberRoleDTO;
import com.basebackend.album.vo.FamilyGroupVO;
import com.basebackend.album.vo.FamilyMemberVO;

import java.util.List;

/**
 * 家庭组服务接口
 *
 * @author BearTeam
 */
public interface FamilyGroupService {

    /** 创建家庭 */
    FamilyGroupVO createFamily(CreateFamilyDTO dto, Long userId);

    /** 我的家庭列表 */
    List<FamilyGroupVO> myFamilies(Long userId);

    /** 家庭详情 */
    FamilyGroupVO getFamilyDetail(Long familyId, Long userId);

    /** 编辑家庭 */
    void updateFamily(Long familyId, UpdateFamilyDTO dto, Long userId);

    /** 解散家庭 */
    void deleteFamily(Long familyId, Long userId);

    /** 生成/刷新邀请码 */
    String refreshInviteCode(Long familyId, Long userId);

    /** 通过邀请码加入家庭 */
    void joinFamily(JoinFamilyDTO dto, Long userId);

    /** 成员列表 */
    List<FamilyMemberVO> listMembers(Long familyId, Long userId);

    /** 修改成员角色 */
    void updateMemberRole(Long familyId, Long targetUserId, UpdateMemberRoleDTO dto, Long userId);

    /** 移除成员 */
    void removeMember(Long familyId, Long targetUserId, Long userId);

    /** 退出家庭 */
    void leaveFamily(Long familyId, Long userId);
}
