package com.basebackend.album.controller;

import com.basebackend.album.dto.CreateFamilyDTO;
import com.basebackend.album.dto.JoinFamilyDTO;
import com.basebackend.album.dto.UpdateFamilyDTO;
import com.basebackend.album.dto.UpdateMemberRoleDTO;
import com.basebackend.album.service.FamilyGroupService;
import com.basebackend.album.vo.FamilyGroupVO;
import com.basebackend.album.vo.FamilyMemberVO;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家庭组控制器
 *
 * @author BearTeam
 */
@RestController
@RequestMapping("/api/album/families")
@RequiredArgsConstructor
public class FamilyGroupController {

    private final FamilyGroupService familyGroupService;

    /** 创建家庭 */
    @PostMapping
    public Result<FamilyGroupVO> createFamily(@Valid @RequestBody CreateFamilyDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(familyGroupService.createFamily(dto, userId));
    }

    /** 我的家庭列表 */
    @GetMapping
    public Result<List<FamilyGroupVO>> myFamilies() {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(familyGroupService.myFamilies(userId));
    }

    /** 家庭详情 */
    @GetMapping("/{id}")
    public Result<FamilyGroupVO> getFamilyDetail(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(familyGroupService.getFamilyDetail(id, userId));
    }

    /** 编辑家庭 */
    @PutMapping("/{id}")
    public Result<Void> updateFamily(@PathVariable Long id, @Valid @RequestBody UpdateFamilyDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        familyGroupService.updateFamily(id, dto, userId);
        return Result.success();
    }

    /** 解散家庭 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFamily(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        familyGroupService.deleteFamily(id, userId);
        return Result.success();
    }

    /** 生成/刷新邀请码 */
    @PostMapping("/{id}/invite")
    public Result<String> refreshInviteCode(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(familyGroupService.refreshInviteCode(id, userId));
    }

    /** 加入家庭(邀请码) */
    @PostMapping("/join")
    public Result<Void> joinFamily(@Valid @RequestBody JoinFamilyDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        familyGroupService.joinFamily(dto, userId);
        return Result.success();
    }

    /** 成员列表 */
    @GetMapping("/{id}/members")
    public Result<List<FamilyMemberVO>> listMembers(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(familyGroupService.listMembers(id, userId));
    }

    /** 修改成员角色 */
    @PutMapping("/{id}/members/{targetUserId}")
    public Result<Void> updateMemberRole(@PathVariable Long id,
                                         @PathVariable Long targetUserId,
                                         @Valid @RequestBody UpdateMemberRoleDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        familyGroupService.updateMemberRole(id, targetUserId, dto, userId);
        return Result.success();
    }

    /** 移除成员 */
    @DeleteMapping("/{id}/members/{targetUserId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long targetUserId) {
        Long userId = UserContextHolder.requireUserId();
        familyGroupService.removeMember(id, targetUserId, userId);
        return Result.success();
    }

    /** 退出家庭 */
    @PostMapping("/{id}/leave")
    public Result<Void> leaveFamily(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        familyGroupService.leaveFamily(id, userId);
        return Result.success();
    }
}
