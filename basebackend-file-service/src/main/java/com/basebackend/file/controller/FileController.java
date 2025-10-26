package com.basebackend.file.controller;

import com.basebackend.common.model.Result;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.entity.FileVersion;
import com.basebackend.file.service.FileManagementService;
import com.basebackend.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
}
