package com.basebackend.service.client;

import com.basebackend.api.model.file.FileMetadataDTO;
import com.basebackend.api.model.file.FileVersionDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 文件服务客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@HttpExchange("/api/files")
public interface FileServiceClient {

    @PostExchange(value = "/upload", contentType = "multipart/form-data")
    @Operation(summary = "上传文件")
    Result<String> uploadFile(@RequestPart("file") MultipartFile file);

    @PostExchange(value = "/upload-v2", contentType = "multipart/form-data")
    @Operation(summary = "上传文件V2")
    Result<FileMetadataDTO> uploadFileV2(@RequestPart("file") MultipartFile file,
                                          @RequestParam(value = "folderId", required = false) Long folderId);

    @PostExchange(value = "/{fileId}/version", contentType = "multipart/form-data")
    @Operation(summary = "创建文件版本")
    Result<FileVersionDTO> createVersion(@PathVariable("fileId") String fileId,
                                          @RequestPart("file") MultipartFile file,
                                          @RequestParam(value = "description", required = false) String description);

    @GetExchange("/{fileId}/preview-url")
    @Operation(summary = "获取文件预览URL")
    Result<String> getFilePreviewUrl(@PathVariable("fileId") String fileId);

    @GetExchange("/{fileId}/thumbnail-url")
    @Operation(summary = "获取缩略图URL")
    Result<String> getThumbnailUrl(@PathVariable("fileId") String fileId);
}
