package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import com.basebackend.feign.config.FileFeignConfig;
import com.basebackend.feign.constant.FeignServiceConstants;
import com.basebackend.feign.dto.file.FileMetadataDTO;
import com.basebackend.feign.dto.file.FileVersionDTO;
import com.basebackend.feign.fallback.FileFeignFallbackFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务 Feign 客户端
 * <p>
 * 提供文件上传相关的远程服务调用接口，支持：
 * <ul>
 *     <li>简单文件上传（兼容旧接口）</li>
 *     <li>增强文件上传（支持文件夹、权限、版本管理）</li>
 *     <li>文件版本创建</li>
 *     <li>获取文件预览地址</li>
 *     <li>获取文件缩略图地址</li>
 * </ul>
 * </p>
 *
 * @author Claude Code
 * @since 2025-01-07
 */
@FeignClient(
        name = FeignServiceConstants.SYS_SERVICE,
        contextId = "fileFeignClient",
        path = "/api/files",
        configuration = FileFeignConfig.class,
        fallbackFactory = FileFeignFallbackFactory.class
)
public interface FileFeignClient {

    /**
     * 上传文件（简单版本 - 兼容旧接口）
     * <p>
     * 仅上传文件并返回存储路径，不包含权限和版本管理功能。
     * </p>
     *
     * @param file 要上传的文件
     * @return 文件存储路径
     */
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "上传文件", description = "简单文件上传，兼容旧接口")
    Result<String> uploadFile(
            @Parameter(description = "上传的文件") @RequestPart("file") MultipartFile file
    );

    /**
     * 上传文件（增强版本）
     * <p>
     * 支持指定目标文件夹，自动记录文件元数据，
     * 包含权限控制和版本管理功能。
     * </p>
     *
     * @param file     要上传的文件
     * @param folderId 目标文件夹ID（可选，不指定则上传到根目录）
     * @return 文件元数据信息
     */
    @PostMapping(
            value = "/upload-v2",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "上传文件（增强版）", description = "支持文件夹、权限控制和版本管理")
    Result<FileMetadataDTO> uploadFileV2(
            @Parameter(description = "上传的文件") @RequestPart("file") MultipartFile file,
            @Parameter(description = "目标文件夹ID") @RequestParam(value = "folderId", required = false) Long folderId
    );

    /**
     * 创建文件版本
     * <p>
     * 基于已有文件创建新版本，支持添加版本变更说明。
     * </p>
     *
     * @param fileId      文件ID
     * @param file        新版本文件
     * @param description 版本变更说明（可选）
     * @return 文件版本信息
     */
    @PostMapping(
            value = "/{fileId}/version",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "创建文件版本", description = "基于已有文件上传新版本")
    Result<FileVersionDTO> createVersion(
            @Parameter(description = "文件ID") @PathVariable("fileId") String fileId,
            @Parameter(description = "新版本文件") @RequestPart("file") MultipartFile file,
            @Parameter(description = "版本说明") @RequestParam(value = "description", required = false) String description
    );

    /**
     * 获取文件预览地址
     * <p>
     * 返回可用于在线预览文件的 URL。
     * </p>
     *
     * @param fileId 文件ID
     * @return 文件预览地址
     */
    @GetMapping(
            value = "/{fileId}/preview-url",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "获取文件预览地址", description = "返回文件在线预览 URL")
    Result<String> getFilePreviewUrl(
            @Parameter(description = "文件ID") @PathVariable("fileId") String fileId
    );

    /**
     * 获取文件缩略图地址
     * <p>
     * 返回文件缩略图的 URL，适用于图片等媒体文件。
     * </p>
     *
     * @param fileId 文件ID
     * @return 文件缩略图地址
     */
    @GetMapping(
            value = "/{fileId}/thumbnail-url",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "获取文件缩略图地址", description = "返回文件缩略图 URL")
    Result<String> getThumbnailUrl(
            @Parameter(description = "文件ID") @PathVariable("fileId") String fileId
    );
}
