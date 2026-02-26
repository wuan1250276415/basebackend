package com.basebackend.service.client.fallback;

import com.basebackend.api.model.file.FileMetadataDTO;
import com.basebackend.api.model.file.FileVersionDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.FileServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@Component
public class FileServiceClientFallback implements FileServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FileServiceClientFallback.class);

    @Override
    public Result<String> uploadFile(MultipartFile file) {
        log.error("[服务降级] 文件上传失败: fileName={}", getFileName(file));
        return Result.error("文件服务不可用，上传失败");
    }

    @Override
    public Result<FileMetadataDTO> uploadFileV2(MultipartFile file, Long folderId) {
        log.error("[服务降级] 文件上传V2失败: fileName={}, folderId={}", getFileName(file), folderId);
        return Result.error("文件服务不可用，上传失败");
    }

    @Override
    public Result<FileVersionDTO> createVersion(String fileId, MultipartFile file, String description) {
        log.error("[服务降级] 创建文件版本失败: fileId={}", fileId);
        return Result.error("文件服务不可用，创建版本失败");
    }

    @Override
    public Result<String> getFilePreviewUrl(String fileId) {
        log.error("[服务降级] 获取文件预览URL失败: fileId={}", fileId);
        return Result.error("文件服务不可用，获取预览URL失败");
    }

    @Override
    public Result<String> getThumbnailUrl(String fileId) {
        log.error("[服务降级] 获取缩略图URL失败: fileId={}", fileId);
        return Result.error("文件服务不可用，获取缩略图URL失败");
    }

    private String getFileName(MultipartFile file) {
        return file != null ? file.getOriginalFilename() : "unknown";
    }
}
