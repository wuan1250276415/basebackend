package com.basebackend.album.controller;

import com.basebackend.album.service.PhotoService;
import com.basebackend.album.vo.PhotoVO;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 回收站控制器
 *
 * @author BearTeam
 */
@RestController
@RequestMapping("/api/album/trash")
@RequiredArgsConstructor
public class TrashController {

    private final PhotoService photoService;

    /** 回收站列表 */
    @GetMapping
    public Result<PageResult<PhotoVO>> listTrash(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(photoService.listTrash(page, size, userId));
    }

    /** 恢复照片 */
    @PostMapping("/{id}/restore")
    public Result<Void> restorePhoto(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        photoService.restorePhoto(id, userId);
        return Result.success();
    }

    /** 彻底删除 */
    @DeleteMapping("/{id}")
    public Result<Void> permanentDelete(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        photoService.permanentDelete(id, userId);
        return Result.success();
    }

    /** 清空回收站 */
    @DeleteMapping("/clear")
    public Result<Void> clearTrash() {
        Long userId = UserContextHolder.requireUserId();
        photoService.clearTrash(userId);
        return Result.success();
    }
}
