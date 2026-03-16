package com.basebackend.album.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.album.dto.CreateFamilyDTO;
import com.basebackend.album.dto.JoinFamilyDTO;
import com.basebackend.album.dto.UpdateFamilyDTO;
import com.basebackend.album.dto.UpdateMemberRoleDTO;
import com.basebackend.album.entity.FamilyGroup;
import com.basebackend.album.entity.FamilyMember;
import com.basebackend.album.enums.FamilyRole;
import com.basebackend.album.mapper.FamilyGroupMapper;
import com.basebackend.album.mapper.FamilyMemberMapper;
import com.basebackend.album.service.FamilyGroupService;
import com.basebackend.album.vo.FamilyGroupVO;
import com.basebackend.album.vo.FamilyMemberVO;
import com.basebackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 家庭组服务实现
 *
 * @author BearTeam
 */
@Service
@RequiredArgsConstructor
public class FamilyGroupServiceImpl implements FamilyGroupService {

    private final FamilyGroupMapper familyGroupMapper;
    private final FamilyMemberMapper familyMemberMapper;

    /**
     * 生成8位邀请码
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 查询当前用户在家庭中的成员记录
     */
    private FamilyMember getMember(Long familyId, Long userId) {
        return familyMemberMapper.selectOne(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .eq(FamilyMember::getUserId, userId));
    }

    /**
     * 校验当前用户是否为管理员+
     */
    private FamilyMember requireAdmin(Long familyId, Long userId) {
        FamilyMember member = getMember(familyId, userId);
        if (member == null) {
            throw BusinessException.forbidden("您不是该家庭成员");
        }
        if (member.getRole() < FamilyRole.ADMIN.getCode()) {
            throw BusinessException.forbidden("需要管理员及以上权限");
        }
        return member;
    }

    /**
     * 将实体转为 VO
     */
    private FamilyGroupVO toVO(FamilyGroup group, Long userId) {
        Long memberCount = familyMemberMapper.selectCount(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, group.getId()));
        FamilyMember currentMember = getMember(group.getId(), userId);
        Integer currentRole = currentMember != null ? currentMember.getRole() : null;

        return new FamilyGroupVO(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getAvatar(),
                group.getOwnerId(),
                null, // ownerName — 后续可扩展调用用户服务
                group.getInviteCode(),
                memberCount.intValue(),
                group.getMaxMembers(),
                group.getMaxStorageGb(),
                group.getUsedStorageBytes(),
                currentRole,
                group.getCreateTime()
        );
    }

    @Override
    @Transactional
    public FamilyGroupVO createFamily(CreateFamilyDTO dto, Long userId) {
        // 创建家庭
        FamilyGroup group = new FamilyGroup();
        group.setName(dto.name());
        group.setDescription(dto.description());
        group.setAvatar(dto.avatar());
        group.setOwnerId(userId);
        group.setInviteCode(generateInviteCode());
        group.setMaxMembers(20);
        group.setMaxStorageGb(50);
        group.setUsedStorageBytes(0L);
        familyGroupMapper.insert(group);

        // 创建者自动加入为创建者角色
        FamilyMember member = new FamilyMember();
        member.setFamilyId(group.getId());
        member.setUserId(userId);
        member.setRole(FamilyRole.CREATOR.getCode());
        member.setJoinTime(LocalDateTime.now());
        familyMemberMapper.insert(member);

        return toVO(group, userId);
    }

    @Override
    public List<FamilyGroupVO> myFamilies(Long userId) {
        // 通过 FamilyMember 关联查询
        List<FamilyMember> members = familyMemberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getUserId, userId));
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> familyIds = members.stream().map(FamilyMember::getFamilyId).toList();
        List<FamilyGroup> groups = familyGroupMapper.selectBatchIds(familyIds);
        return groups.stream().map(g -> toVO(g, userId)).toList();
    }

    @Override
    public FamilyGroupVO getFamilyDetail(Long familyId, Long userId) {
        FamilyGroup group = familyGroupMapper.selectById(familyId);
        if (group == null) {
            throw BusinessException.notFound("家庭不存在");
        }
        return toVO(group, userId);
    }

    @Override
    @Transactional
    public void updateFamily(Long familyId, UpdateFamilyDTO dto, Long userId) {
        requireAdmin(familyId, userId);
        FamilyGroup group = familyGroupMapper.selectById(familyId);
        if (group == null) {
            throw BusinessException.notFound("家庭不存在");
        }
        if (dto.name() != null) {
            group.setName(dto.name());
        }
        if (dto.description() != null) {
            group.setDescription(dto.description());
        }
        if (dto.avatar() != null) {
            group.setAvatar(dto.avatar());
        }
        familyGroupMapper.updateById(group);
    }

    @Override
    @Transactional
    public void deleteFamily(Long familyId, Long userId) {
        FamilyGroup group = familyGroupMapper.selectById(familyId);
        if (group == null) {
            throw BusinessException.notFound("家庭不存在");
        }
        if (!group.getOwnerId().equals(userId)) {
            throw BusinessException.forbidden("只有创建者才能解散家庭");
        }
        // 级联删除成员
        familyMemberMapper.delete(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId));
        familyGroupMapper.deleteById(familyId);
    }

    @Override
    @Transactional
    public String refreshInviteCode(Long familyId, Long userId) {
        requireAdmin(familyId, userId);
        FamilyGroup group = familyGroupMapper.selectById(familyId);
        if (group == null) {
            throw BusinessException.notFound("家庭不存在");
        }
        String newCode = generateInviteCode();
        group.setInviteCode(newCode);
        familyGroupMapper.updateById(group);
        return newCode;
    }

    @Override
    @Transactional
    public void joinFamily(JoinFamilyDTO dto, Long userId) {
        FamilyGroup group = familyGroupMapper.selectOne(
                new LambdaQueryWrapper<FamilyGroup>()
                        .eq(FamilyGroup::getInviteCode, dto.inviteCode()));
        if (group == null) {
            throw BusinessException.notFound("邀请码无效");
        }
        // 检查是否已经是成员
        FamilyMember existing = getMember(group.getId(), userId);
        if (existing != null) {
            throw BusinessException.conflict("您已是该家庭成员");
        }
        // 检查成员数量限制
        Long count = familyMemberMapper.selectCount(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, group.getId()));
        if (count >= group.getMaxMembers()) {
            throw BusinessException.conflict("家庭成员已满");
        }

        FamilyMember member = new FamilyMember();
        member.setFamilyId(group.getId());
        member.setUserId(userId);
        member.setNickname(dto.nickname());
        member.setRole(FamilyRole.MEMBER.getCode());
        member.setJoinTime(LocalDateTime.now());
        familyMemberMapper.insert(member);
    }

    @Override
    public List<FamilyMemberVO> listMembers(Long familyId, Long userId) {
        // 校验当前用户是否为该家庭成员
        FamilyMember current = getMember(familyId, userId);
        if (current == null) {
            throw BusinessException.forbidden("您不是该家庭成员");
        }
        List<FamilyMember> members = familyMemberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .orderByDesc(FamilyMember::getRole)
                        .orderByAsc(FamilyMember::getJoinTime));
        return members.stream().map(m -> {
            String roleName = switch (m.getRole()) {
                case 2 -> "创建者";
                case 1 -> "管理员";
                default -> "成员";
            };
            return new FamilyMemberVO(
                    m.getId(), m.getUserId(), m.getNickname(),
                    null, // userName 后续可扩展
                    m.getRole(), roleName, m.getJoinTime());
        }).toList();
    }

    @Override
    @Transactional
    public void updateMemberRole(Long familyId, Long targetUserId, UpdateMemberRoleDTO dto, Long userId) {
        FamilyMember operator = requireAdmin(familyId, userId);
        FamilyMember target = getMember(familyId, targetUserId);
        if (target == null) {
            throw BusinessException.notFound("目标成员不存在");
        }
        // 不能修改创建者角色
        if (target.getRole() == FamilyRole.CREATOR.getCode()) {
            throw BusinessException.forbidden("不能修改创建者角色");
        }
        // 管理员不能将他人设为创建者
        if (dto.role() == FamilyRole.CREATOR.getCode()) {
            throw BusinessException.forbidden("不能设置为创建者角色");
        }
        // 管理员不能修改其他管理员（只有创建者可以）
        if (operator.getRole() == FamilyRole.ADMIN.getCode()
                && target.getRole() == FamilyRole.ADMIN.getCode()) {
            throw BusinessException.forbidden("管理员不能修改其他管理员的角色");
        }
        target.setRole(dto.role());
        familyMemberMapper.updateById(target);
    }

    @Override
    @Transactional
    public void removeMember(Long familyId, Long targetUserId, Long userId) {
        FamilyMember operator = requireAdmin(familyId, userId);
        if (targetUserId.equals(userId)) {
            throw BusinessException.paramError("不能移除自己，请使用退出功能");
        }
        FamilyMember target = getMember(familyId, targetUserId);
        if (target == null) {
            throw BusinessException.notFound("目标成员不存在");
        }
        // 不能移除创建者
        if (target.getRole() == FamilyRole.CREATOR.getCode()) {
            throw BusinessException.forbidden("不能移除创建者");
        }
        // 管理员不能移除其他管理员
        if (operator.getRole() == FamilyRole.ADMIN.getCode()
                && target.getRole() >= FamilyRole.ADMIN.getCode()) {
            throw BusinessException.forbidden("管理员不能移除其他管理员");
        }
        familyMemberMapper.deleteById(target.getId());
    }

    @Override
    @Transactional
    public void leaveFamily(Long familyId, Long userId) {
        FamilyMember member = getMember(familyId, userId);
        if (member == null) {
            throw BusinessException.notFound("您不是该家庭成员");
        }
        // 创建者不能直接退出，需要先转让或解散
        if (member.getRole() == FamilyRole.CREATOR.getCode()) {
            throw BusinessException.forbidden("创建者不能退出，请先解散家庭或转让创建者角色");
        }
        familyMemberMapper.deleteById(member.getId());
    }
}
