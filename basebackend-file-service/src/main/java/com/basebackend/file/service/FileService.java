package com.basebackend.file.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 文件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileProperties fileProperties;

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件访问路径
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > fileProperties.getMaxSize()) {
            throw new BusinessException("文件大小超过限制");
        }

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("文件名不能为空");
        }

        // 验证文件类型
        String extension = FilenameUtils.getExtension(originalFilename);
        if (!isAllowedFileType(extension)) {
            throw new BusinessException("不支持的文件类型: " + extension);
        }

        try {
            // 生成文件名
            String fileName = generateFileName(extension);

            // 生成存储路径
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = datePath + "/" + fileName;

            // 确保目录存在
            Path uploadPath = Paths.get(fileProperties.getUploadPath(), datePath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            log.info("文件上传成功: {}", filePath);

            // 返回访问路径
            return fileProperties.getAccessPrefix() + "/" + relativePath;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     */
    public void deleteFile(String filePath) {
        try {
            // 移除访问前缀
            String realPath = filePath.replace(fileProperties.getAccessPrefix() + "/", "");
            Path path = Paths.get(fileProperties.getUploadPath(), realPath);

            if (Files.exists(path)) {
                Files.delete(path);
                log.info("文件删除成功: {}", path);
            } else {
                log.warn("文件不存在: {}", path);
            }
        } catch (IOException e) {
            log.error("文件删除失败", e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件
     *
     * @param filePath 文件路径
     * @return 文件
     */
    public File getFile(String filePath) {
        // 移除访问前缀
        String realPath = filePath.replace(fileProperties.getAccessPrefix() + "/", "");
        Path path = Paths.get(fileProperties.getUploadPath(), realPath);

        if (!Files.exists(path)) {
            throw new BusinessException("文件不存在");
        }

        return path.toFile();
    }

    /**
     * 验证文件类型是否允许
     */
    private boolean isAllowedFileType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        return Arrays.asList(fileProperties.getAllowedTypes())
                .contains(extension.toLowerCase());
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String extension) {
        return IdUtil.simpleUUID() + "." + extension;
    }
}
