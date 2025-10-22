package com.basebackend.admin.controller.storage;

import com.basebackend.admin.entity.storage.SysFileInfo;
import com.basebackend.admin.service.storage.SysFileService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件管理Controller
 *
 * @author BaseBackend
 */
@Slf4j
@RestController
@RequestMapping("/api/storage/file")
@RequiredArgsConstructor
@Validated
@Tag(name = "文件管理", description = "MinIO文件管理接口")
public class AdminFileController {

    private final SysFileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<Long> uploadFile(@RequestParam("file") MultipartFile file) {
        Long fileId = fileService.uploadFile(file);
        return Result.success("文件上传成功", fileId);
    }

    @PostMapping("/upload/image")
    @Operation(summary = "上传图片")
    public Result<Long> uploadImage(@RequestParam("file") MultipartFile file) {
        Long fileId = fileService.uploadImage(file);
        return Result.success("图片上传成功", fileId);
    }

    @PostMapping("/upload/large")
    @Operation(summary = "上传大文件")
    public Result<Long> uploadLargeFile(@RequestParam("file") MultipartFile file) {
        Long fileId = fileService.uploadLargeFile(file);
        return Result.success("大文件上传成功", fileId);
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long fileId) {
        SysFileInfo fileInfo = fileService.getById(fileId);
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }

        InputStream inputStream = fileService.downloadFile(fileId);
        if (inputStream == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileInfo.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/url/{fileId}")
    @Operation(summary = "获取文件访问URL")
    public Result<String> getFileUrl(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "604800") int expirySeconds) {
        String url = fileService.getFileUrl(fileId, expirySeconds);
        return Result.success(url);
    }

    @GetMapping("/list")
    @Operation(summary = "获取文件列表")
    public Result<List<SysFileInfo>> listFiles(
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) Long uploadUserId) {
        List<SysFileInfo> files = fileService.listFiles(fileCategory, uploadUserId);
        return Result.success(files);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public Result<Boolean> deleteFile(@PathVariable Long fileId) {
        boolean success = fileService.deleteFile(fileId);
        return success ?
                Result.success("文件删除成功", true) :
                Result.error("文件删除失败");
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除文件")
    public Result<Integer> batchDeleteFiles(@RequestBody List<Long> fileIds) {
        int count = fileService.batchDeleteFiles(fileIds);
        return Result.success("删除了 " + count + " 个文件", count);
    }
}
