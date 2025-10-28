package com.basebackend.file.controller;

import com.basebackend.common.model.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.entity.FileOperationLog;
import com.basebackend.file.entity.FilePermission;
import com.basebackend.file.entity.FileRecycleBin;
import com.basebackend.file.entity.FileShare;
import com.basebackend.file.entity.FileTag;
import com.basebackend.file.entity.FileVersion;
import com.basebackend.file.model.FileShareRequest;
import com.basebackend.file.model.FileStatistics;
import com.basebackend.file.model.StorageUsageSummary;
import com.basebackend.file.service.FileManagementService;
import com.basebackend.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;
    private final FileManagementService fileManagementService;

    /**
     * 上传文件 (简单版本 - 兼容旧接口)
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String filePath = fileService.uploadFile(file);
        return Result.success("文件上传成功", filePath);
    }

    /**
     * 上传文件 (增强版本 - 支持权限控制和版本管理)
     */
    @PostMapping("/upload-v2")
    public Result<FileMetadata> uploadFileV2(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "folderId", required = false) Long folderId
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        FileMetadata metadata = fileManagementService.uploadFile(file, folderId, userId, userName);
        return Result.success("文件上传成功", metadata);
    }

    /**
     * 获取文件列表
     */
    @GetMapping("/list")
    public Result<PageResult<FileMetadata>> getFileList(
        @RequestParam(value = "fileName", required = false) String fileName,
        @RequestParam(value = "fileExtension", required = false) String fileExtension,
        @RequestParam(value = "folderId", required = false) Long folderId,
        @RequestParam(value = "ownerId", required = false) Long ownerId,
        @RequestParam(value = "isPublic", required = false) Boolean isPublic,
        @RequestParam(value = "startTime", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(value = "endTime", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(value = "current", defaultValue = "1") long current,
        @RequestParam(value = "size", defaultValue = "10") long size
    ) {
        PageResult<FileMetadata> page = fileManagementService.listFiles(
            fileName, fileExtension, folderId, ownerId, isPublic, startTime, endTime, current, size
        );
        return Result.success(page);
    }

    /**
     * 获取文件详情
     */
    @GetMapping("/{fileId}")
    public Result<FileMetadata> getFileDetail(@PathVariable String fileId) {
        FileMetadata metadata = fileManagementService.getFileDetail(fileId);
        return Result.success(metadata);
    }

    /**
     * 下载文件 (简单版本 - 兼容旧接口)
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("path") String path) {
        try {
            File file = fileService.getFile(path);
            byte[] content = Files.readAllBytes(file.toPath());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", file.getName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            log.error("文件下载失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 下载文件 (增强版本 - 支持权限控制)
     */
    @GetMapping("/download-v2/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFileV2(@PathVariable String fileId) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        FileManagementService.FileDownloadInfo downloadInfo =
            fileManagementService.downloadFile(fileId, userId);

        String encodedFileName = URLEncoder.encode(
            downloadInfo.getFileName(),
            StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                   "attachment; filename*=UTF-8''" + encodedFileName)
            .contentType(MediaType.parseMediaType(downloadInfo.getContentType()))
            .contentLength(downloadInfo.getFileSize())
            .body(new InputStreamResource(downloadInfo.getInputStream()));
    }

    /**
     * 删除文件 (简单版本 - 兼容旧接口)
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteFile(@RequestParam("path") String path) {
        fileService.deleteFile(path);
        return Result.success("文件删除成功", null);
    }

    /**
     * 删除文件 (增强版本 - 移入回收站)
     */
    @DeleteMapping("/{fileId}")
    public Result<Void> deleteFileV2(@PathVariable String fileId) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        fileManagementService.deleteFile(fileId, userId, userName);
        return Result.success("文件已移入回收站", null);
    }

    /**
     * 批量删除文件
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteFiles(@RequestBody List<String> fileIds) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        fileManagementService.batchDeleteFiles(fileIds, userId, userName);
        return Result.success("文件已批量移入回收站", null);
    }

    /**
     * 重命名文件
     */
    @PutMapping("/{fileId}/rename")
    public Result<Void> renameFile(
        @PathVariable String fileId,
        @RequestParam("newName") String newName
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.renameFile(fileId, newName, userId);
        return Result.success("文件重命名成功", null);
    }

    /**
     * 移动文件
     */
    @PutMapping("/{fileId}/move")
    public Result<Void> moveFile(
        @PathVariable String fileId,
        @RequestParam("targetFolderId") Long targetFolderId
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.moveFile(fileId, targetFolderId, userId);
        return Result.success("文件移动成功", null);
    }

    /**
     * 复制文件
     */
    @PostMapping("/{fileId}/copy")
    public Result<FileMetadata> copyFile(
        @PathVariable String fileId,
        @RequestParam(value = "targetFolderId", required = false) Long targetFolderId
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        FileMetadata copy = fileManagementService.copyFile(fileId, targetFolderId, userId, userName);
        return Result.success("文件复制成功", copy);
    }

    /**
     * 获取文件版本历史
     */
    @GetMapping("/{fileId}/versions")
    public Result<List<FileVersion>> getFileVersions(@PathVariable String fileId) {
        List<FileVersion> versions = fileManagementService.getFileVersions(fileId);
        return Result.success(versions);
    }

    /**
     * 下载指定版本
     */
    @GetMapping("/{fileId}/versions/{versionId}/download")
    public ResponseEntity<InputStreamResource> downloadVersion(
        @PathVariable String fileId,
        @PathVariable Long versionId
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        FileManagementService.FileDownloadInfo info =
            fileManagementService.downloadVersion(fileId, versionId, userId);

        String encodedFileName = URLEncoder.encode(
            info.getFileName(),
            StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName)
            .contentType(MediaType.parseMediaType(info.getContentType()))
            .contentLength(info.getFileSize())
            .body(new InputStreamResource(info.getInputStream()));
    }

    /**
     * 恢复文件
     */
    @PostMapping("/{fileId}/restore")
    public Result<Void> restoreFile(@PathVariable String fileId) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.restoreFile(fileId, userId);
        return Result.success("文件恢复成功", null);
    }

    /**
     * 获取回收站列表
     */
    @GetMapping("/recycle-bin")
    public Result<PageResult<FileRecycleBin>> getRecycleBinList(
        @RequestParam(value = "current", defaultValue = "1") long current,
        @RequestParam(value = "size", defaultValue = "10") long size
    ) {
        PageResult<FileRecycleBin> page = fileManagementService.getRecycleBinList(current, size);
        return Result.success(page);
    }

    /**
     * 彻底删除文件
     */
    @DeleteMapping("/{fileId}/permanent")
    public Result<Void> permanentDeleteFile(@PathVariable String fileId) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.permanentDeleteFile(fileId, userId);
        return Result.success("文件已彻底删除", null);
    }

    /**
     * 批量恢复文件
     */
    @PostMapping("/restore/batch")
    public Result<Void> batchRestoreFiles(@RequestBody List<String> fileIds) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.batchRestoreFiles(fileIds, userId);
        return Result.success("文件批量恢复成功", null);
    }

    /**
     * 批量彻底删除
     */
    @DeleteMapping("/permanent/batch")
    public Result<Void> batchPermanentDelete(@RequestBody List<String> fileIds) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.batchPermanentDelete(fileIds, userId);
        return Result.success("文件批量彻底删除成功", null);
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/recycle-bin/empty")
    public Result<Void> emptyRecycleBin() {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.emptyRecycleBin(userId);
        return Result.success("回收站已清空", null);
    }

    /**
     * 创建文件版本
     */
    @PostMapping("/{fileId}/version")
    public Result<FileVersion> createVersion(
        @PathVariable String fileId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "description", required = false) String description
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        FileVersion version = fileManagementService.createFileVersion(
            fileId, file, description, userId, userName
        );
        return Result.success("版本创建成功", version);
    }

    /**
     * 版本回退
     */
    @PostMapping("/{fileId}/revert/{versionId}")
    public Result<Void> revertVersion(
        @PathVariable String fileId,
        @PathVariable Long versionId
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.revertToVersion(fileId, versionId, userId);
        return Result.success("版本回退成功", null);
    }

    /**
     * 获取文件权限列表
     */
    @GetMapping("/{fileId}/permissions")
    public Result<List<FilePermission>> getFilePermissions(@PathVariable String fileId) {
        List<FilePermission> permissions = fileManagementService.getFilePermissions(fileId);
        return Result.success(permissions);
    }

    /**
     * 添加文件权限
     */
    @PostMapping("/{fileId}/permissions")
    public Result<Void> addFilePermission(
        @PathVariable String fileId,
        @RequestBody @Validated FilePermission permission
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        fileManagementService.addFilePermission(fileId, permission, userId, userName);
        return Result.success("权限添加成功", null);
    }

    /**
     * 删除文件权限
     */
    @DeleteMapping("/{fileId}/permissions/{permissionId}")
    public Result<Void> deleteFilePermission(
        @PathVariable String fileId,
        @PathVariable Long permissionId
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.deleteFilePermission(fileId, permissionId, userId);
        return Result.success("权限删除成功", null);
    }

    /**
     * 设置文件公开状态
     */
    @PutMapping("/{fileId}/public")
    public Result<Void> setFilePublic(
        @PathVariable String fileId,
        @RequestParam("isPublic") Boolean isPublic
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.setFilePublic(fileId, isPublic, userId);
        return Result.success("文件公开状态已更新", null);
    }

    /**
     * 创建文件分享
     */
    @PostMapping("/share")
    public Result<FileShare> createFileShare(@RequestBody @Validated FileShareRequest request) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        FileShare share = fileManagementService.createFileShare(
            request.getFileId(),
            request.getSharePassword(),
            request.resolveExpireTime(),
            request.getDownloadLimit(),
            request.getAllowDownload(),
            request.getAllowPreview(),
            userId,
            userName
        );
        return Result.success("文件分享创建成功", share);
    }

    /**
     * 获取分享信息
     */
    @GetMapping("/share/{shareCode}")
    public Result<FileShare> getShareInfo(
        @PathVariable String shareCode,
        @RequestParam(value = "password", required = false) String password
    ) {
        FileShare share = fileManagementService.getShareInfo(shareCode, password);
        return Result.success(share);
    }

    /**
     * 取消分享
     */
    @DeleteMapping("/share/{shareId}")
    public Result<Void> cancelShare(@PathVariable Long shareId) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.cancelShare(shareId, userId);
        return Result.success("分享已取消", null);
    }

    /**
     * 获取我的分享列表
     */
    @GetMapping("/share/my")
    public Result<PageResult<FileShare>> getMyShares(
        @RequestParam(value = "current", defaultValue = "1") long current,
        @RequestParam(value = "size", defaultValue = "10") long size
    ) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        PageResult<FileShare> page = fileManagementService.getMyShares(userId, current, size);
        return Result.success(page);
    }

    /**
     * 获取所有标签
     */
    @GetMapping("/tags")
    public Result<List<FileTag>> getAllTags() {
        List<FileTag> tags = fileManagementService.getAllTags();
        return Result.success(tags);
    }

    /**
     * 创建标签
     */
    @PostMapping("/tags")
    public Result<FileTag> createTag(@RequestBody @Validated FileTag tag) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        FileTag created = fileManagementService.createTag(tag, userId, userName);
        return Result.success("标签创建成功", created);
    }

    /**
     * 为文件添加标签
     */
    @PostMapping("/{fileId}/tags/{tagId}")
    public Result<Void> addFileTag(@PathVariable String fileId, @PathVariable Long tagId) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;
        String userName = "admin";

        fileManagementService.addFileTag(fileId, tagId, userId, userName);
        return Result.success("标签添加成功", null);
    }

    /**
     * 为文件移除标签
     */
    @DeleteMapping("/{fileId}/tags/{tagId}")
    public Result<Void> removeFileTag(@PathVariable String fileId, @PathVariable Long tagId) {
        // TODO: 从SecurityContext获取当前用户信息
        Long userId = 1L;

        fileManagementService.removeFileTag(fileId, tagId, userId);
        return Result.success("标签移除成功", null);
    }

    /**
     * 获取文件的标签
     */
    @GetMapping("/{fileId}/tags")
    public Result<List<FileTag>> getFileTags(@PathVariable String fileId) {
        List<FileTag> tags = fileManagementService.getFileTags(fileId);
        return Result.success(tags);
    }

    /**
     * 获取文件操作日志
     */
    @GetMapping("/{fileId}/logs")
    public Result<PageResult<FileOperationLog>> getFileOperationLogs(
        @PathVariable String fileId,
        @RequestParam(value = "current", defaultValue = "1") long current,
        @RequestParam(value = "size", defaultValue = "10") long size
    ) {
        PageResult<FileOperationLog> page = fileManagementService.getFileOperationLogs(fileId, current, size);
        return Result.success(page);
    }

    /**
     * 获取文件统计信息
     */
    @GetMapping("/statistics")
    public Result<FileStatistics> getFileStatistics() {
        FileStatistics statistics = fileManagementService.getFileStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取存储使用情况
     */
    @GetMapping("/storage/usage")
    public Result<StorageUsageSummary> getStorageUsage() {
        StorageUsageSummary summary = fileManagementService.getStorageUsage();
        return Result.success(summary);
    }

    /**
     * 获取文件预览地址
     */
    @GetMapping("/{fileId}/preview-url")
    public Result<String> getFilePreviewUrl(@PathVariable String fileId) {
        String url = fileManagementService.getFilePreviewUrl(fileId);
        return Result.success(url);
    }

    /**
     * 获取文件缩略图地址
     */
    @GetMapping("/{fileId}/thumbnail-url")
    public Result<String> getThumbnailUrl(@PathVariable String fileId) {
        String url = fileManagementService.getThumbnailUrl(fileId);
        return Result.success(url);
    }
}
