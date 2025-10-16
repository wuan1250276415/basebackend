package com.basebackend.file.controller;

import com.basebackend.common.model.Result;
import com.basebackend.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String filePath = fileService.uploadFile(file);
        return Result.success("文件上传成功", filePath);
    }

    /**
     * 下载文件
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
     * 删除文件
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteFile(@RequestParam("path") String path) {
        fileService.deleteFile(path);
        return Result.success("文件删除成功", null);
    }
}
