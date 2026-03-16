package com.basebackend.album.controller;

import com.basebackend.album.dto.CreateShareDTO;
import com.basebackend.album.dto.ShareAccessDTO;
import com.basebackend.album.service.ShareService;
import com.basebackend.album.vo.AlbumVO;
import com.basebackend.album.vo.ShareLinkVO;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分享控制器
 *
 * @author BearTeam
 */
@RestController
@RequestMapping("/api/album/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /** 创建分享链接 */
    @PostMapping
    public Result<ShareLinkVO> createShare(@Valid @RequestBody CreateShareDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(shareService.createShare(dto, userId));
    }

    /** 我的分享列表 */
    @GetMapping
    public Result<List<ShareLinkVO>> myShares() {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(shareService.myShares(userId));
    }

    /** 取消分享 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteShare(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        shareService.deleteShare(id, userId);
        return Result.success();
    }

    /** 访问分享（公开接口） */
    @GetMapping("/view/{shareCode}")
    public Result<AlbumVO> accessShare(@PathVariable String shareCode,
                                       @RequestBody(required = false) ShareAccessDTO dto) {
        return Result.success(shareService.accessShare(shareCode, dto));
    }
}
