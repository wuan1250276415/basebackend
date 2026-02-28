package com.basebackend.album.controller;

import com.basebackend.album.dto.BatchDeleteDTO;
import com.basebackend.album.dto.MovePhotoDTO;
import com.basebackend.album.dto.UpdatePhotoDTO;
import com.basebackend.album.dto.UploadPhotoDTO;
import com.basebackend.album.service.PhotoService;
import com.basebackend.album.service.TimelineService;
import com.basebackend.album.vo.PhotoVO;
import com.basebackend.album.vo.TimelineVO;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * 照片控制器
 *
 * @author BearTeam
 */
@RestController
@RequestMapping("/api/album/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;
    private final TimelineService timelineService;

    /** 上传照片 */
    @PostMapping("/upload")
    public Result<PhotoVO> uploadPhoto(@RequestParam("file") MultipartFile file,
                                       @Valid UploadPhotoDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(photoService.uploadPhoto(file, dto, userId));
    }

    /** 照片列表（分页） */
    @GetMapping
    public Result<PageResult<PhotoVO>> listPhotos(
            @RequestParam Long albumId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(photoService.listPhotos(albumId, page, size, userId));
    }

    /** 照片详情 */
    @GetMapping("/{id}")
    public Result<PhotoVO> getPhotoDetail(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(photoService.getPhotoDetail(id, userId));
    }

    /** 编辑照片信息 */
    @PutMapping("/{id}")
    public Result<Void> updatePhoto(@PathVariable Long id, @Valid @RequestBody UpdatePhotoDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        photoService.updatePhoto(id, dto, userId);
        return Result.success();
    }

    /** 删除照片（软删除） */
    @DeleteMapping("/{id}")
    public Result<Void> deletePhoto(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        photoService.deletePhoto(id, userId);
        return Result.success();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        photoService.batchDelete(dto, userId);
        return Result.success();
    }

    /** 移动到其他相册 */
    @PostMapping("/{id}/move")
    public Result<Void> movePhoto(@PathVariable Long id, @Valid @RequestBody MovePhotoDTO dto) {
        Long userId = UserContextHolder.requireUserId();
        photoService.movePhoto(id, dto, userId);
        return Result.success();
    }

    /** 时间轴（按日期分组） */
    @GetMapping("/timeline")
    public Result<List<TimelineVO>> timeline(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(timelineService.getTimeline(userId, page, size));
    }

    /** 搜索照片 */
    @GetMapping("/search")
    public Result<PageResult<PhotoVO>> searchPhotos(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContextHolder.requireUserId();
        return Result.success(photoService.searchPhotos(keyword, page, size, userId));
    }

    /** 点赞 */
    @PostMapping("/{id}/like")
    public Result<Void> likePhoto(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        photoService.likePhoto(id, userId);
        return Result.success();
    }

    /** 取消点赞 */
    @DeleteMapping("/{id}/like")
    public Result<Void> unlikePhoto(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        photoService.unlikePhoto(id, userId);
        return Result.success();
    }

    /** 下载原图 */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable Long id) {
        Long userId = UserContextHolder.requireUserId();
        String filePath = photoService.getDownloadPath(id, userId);
        File file = new File(filePath);
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
