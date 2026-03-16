package com.basebackend.album.controller;

import com.basebackend.album.dto.CreateAlbumDTO;
import com.basebackend.album.dto.SetCoverDTO;
import com.basebackend.album.dto.UpdateAlbumDTO;
import com.basebackend.album.service.AlbumService;
import com.basebackend.album.vo.AlbumVO;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 相册控制器
 *
 * @author BearTeam
 */
@RestController
@RequestMapping("/api/album/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    /** 创建相册 */
    @PostMapping
    public Result<AlbumVO> createAlbum(@Valid @RequestBody CreateAlbumDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(albumService.createAlbum(dto, userId));
    }

    /** 相册列表（个人+家庭） */
    @GetMapping
    public Result<PageResult<AlbumVO>> listAlbums(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(albumService.listAlbums(userId, page, size));
    }

    /** 相册详情 */
    @GetMapping("/{id}")
    public Result<AlbumVO> getAlbumDetail(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(albumService.getAlbumDetail(id, userId));
    }

    /** 编辑相册 */
    @PutMapping("/{id}")
    public Result<Void> updateAlbum(@PathVariable Long id, @Valid @RequestBody UpdateAlbumDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        albumService.updateAlbum(id, dto, userId);
        return Result.success();
    }

    /** 删除相册 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAlbum(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        albumService.deleteAlbum(id, userId);
        return Result.success();
    }

    /** 设置封面 */
    @PutMapping("/{id}/cover")
    public Result<Void> setCover(@PathVariable Long id, @Valid @RequestBody SetCoverDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        albumService.setCover(id, dto, userId);
        return Result.success();
    }

    /** 家庭相册列表 */
    @GetMapping("/family/{familyId}")
    public Result<List<AlbumVO>> listFamilyAlbums(@PathVariable Long familyId) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(albumService.listFamilyAlbums(familyId, userId));
    }
}
