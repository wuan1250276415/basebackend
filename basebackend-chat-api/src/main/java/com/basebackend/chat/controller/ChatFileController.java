package com.basebackend.chat.controller;

import com.basebackend.chat.enums.ChatErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天文件上传控制器
 * <p>
 * MVP 阶段将文件保存到本地目录，后续集成 MinIO / 文件微服务替换存储后端。
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/files")
@Validated
@Tag(name = "聊天文件", description = "聊天消息的文件上传接口")
public class ChatFileController {

    @Value("${basebackend.chat.upload-dir:${java.io.tmpdir}/chat-files}")
    private String uploadDir;

    @Value("${basebackend.chat.upload-url-prefix:/api/chat/files/}")
    private String uploadUrlPrefix;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传聊天中使用的图片、文件、语音、视频等")
    public Result<Map<String, Object>> upload(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "0") Long tenantId,
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new BusinessException(ChatErrorCode.FILE_UPLOAD_FAILED, "文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path tenantDir = Path.of(uploadDir, tenantId.toString());
        try {
            Files.createDirectories(tenantDir);
            Files.copy(file.getInputStream(), tenantDir.resolve(storedName));
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(ChatErrorCode.FILE_UPLOAD_FAILED);
        }

        log.info("文件上传成功: userId={}, tenant={}, file={}, stored={}",
                currentUserId, tenantId, originalName, storedName);

        return Result.success("上传成功", Map.of(
                "fileName", originalName != null ? originalName : storedName,
                "storedName", storedName,
                "fileSize", file.getSize(),
                "contentType", file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                "url", uploadUrlPrefix + storedName
        ));
    }
}
