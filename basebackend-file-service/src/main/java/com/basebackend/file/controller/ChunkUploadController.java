package com.basebackend.file.controller;

import com.basebackend.common.model.Result;
import com.basebackend.file.chunk.ChunkUploadInfo;
import com.basebackend.file.chunk.ChunkUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 分块上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files/chunk")
@RequiredArgsConstructor
@Validated
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    /**
     * 初始化分块上传
     */
    @PostMapping("/init")
    public Result<ChunkUploadInfo> initUpload(
        @RequestParam("filename") String filename,
        @RequestParam("fileSize") long fileSize,
        @RequestParam(value = "fileMd5", required = false) String fileMd5,
        @RequestParam(value = "contentType", required = false) String contentType,
        @RequestParam("targetPath") String targetPath,
        @RequestParam(value = "chunkSize", required = false) Integer chunkSize
    ) {
        ChunkUploadInfo info;
        if (chunkSize != null && chunkSize > 0) {
            info = chunkUploadService.initUpload(filename, fileSize, fileMd5, contentType, targetPath, chunkSize);
        } else {
            info = chunkUploadService.initUpload(filename, fileSize, fileMd5, contentType, targetPath);
        }
        return Result.success("分块上传初始化成功", info);
    }

    /**
     * 上传单个分块
     */
    @PostMapping("/{uploadId}/{chunkIndex}")
    public Result<ChunkUploadInfo> uploadChunk(
        @PathVariable String uploadId,
        @PathVariable int chunkIndex,
        @RequestParam("file") MultipartFile file
    ) {
        ChunkUploadInfo info = chunkUploadService.uploadChunk(uploadId, chunkIndex, file);
        return Result.success(info);
    }

    /**
     * 合并分块完成上传
     */
    @PostMapping("/{uploadId}/complete")
    public Result<String> completeUpload(@PathVariable String uploadId) {
        String storagePath = chunkUploadService.completeUpload(uploadId);
        return Result.success("分块上传完成", storagePath);
    }

    /**
     * 取消上传
     */
    @DeleteMapping("/{uploadId}")
    public Result<Void> cancelUpload(@PathVariable String uploadId) {
        chunkUploadService.cancelUpload(uploadId);
        return Result.success("上传已取消", null);
    }

    /**
     * 查询上传进度
     */
    @GetMapping("/{uploadId}")
    public Result<ChunkUploadInfo> getUploadInfo(@PathVariable String uploadId) {
        ChunkUploadInfo info = chunkUploadService.getUploadInfo(uploadId);
        if (info == null) {
            return Result.error("上传任务不存在或已过期");
        }
        return Result.success(info);
    }

    /**
     * 查询已上传的分块列表（用于断点续传）
     */
    @GetMapping("/{uploadId}/chunks")
    public Result<List<Integer>> getUploadedChunks(@PathVariable String uploadId) {
        List<Integer> chunks = chunkUploadService.getUploadedChunks(uploadId);
        return Result.success(chunks);
    }
}
