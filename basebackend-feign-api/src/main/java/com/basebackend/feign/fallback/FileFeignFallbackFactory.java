package com.basebackend.feign.fallback;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.FileFeignClient;
import com.basebackend.feign.dto.file.FileMetadataDTO;
import com.basebackend.feign.dto.file.FileVersionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务 Feign 降级处理工厂
 * <p>
 * 当文件服务不可用时，提供降级处理逻辑。
 * 对于文件上传这类写操作，降级时直接返回错误，
 * 避免调用方误判上传成功。
 * </p>
 *
 * @author Claude Code
 * @since 2025-01-07
 */
@Slf4j
@Component
public class FileFeignFallbackFactory implements FallbackFactory<FileFeignClient> {

    @Override
    public FileFeignClient create(Throwable cause) {
        return new FileFeignClient() {

            @Override
            public Result<String> uploadFile(MultipartFile file) {
                log.error("[Feign降级] 简单文件上传失败: filename={}, error={}",
                        getFileName(file), cause.getMessage(), cause);
                return Result.error("文件服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<FileMetadataDTO> uploadFileV2(MultipartFile file, Long folderId) {
                log.error("[Feign降级] 增强文件上传失败: filename={}, folderId={}, error={}",
                        getFileName(file), folderId, cause.getMessage(), cause);
                return Result.error("文件服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<FileVersionDTO> createVersion(String fileId, MultipartFile file, String description) {
                log.error("[Feign降级] 创建文件版本失败: fileId={}, filename={}, description={}, error={}",
                        fileId, getFileName(file), description, cause.getMessage(), cause);
                return Result.error("文件服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<String> getFilePreviewUrl(String fileId) {
                log.error("[Feign降级] 创建文件版本失败: fileId={}",
                        fileId);
                return Result.error("文件服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<String> getThumbnailUrl(String fileId) {
                log.error("[Feign降级] 创建文件版本失败: fileId={}",
                        fileId);
                return Result.error("文件服务暂时不可用，请稍后重试");
            }

            /**
             * 安全获取文件名，处理空值情况
             */
            private String getFileName(MultipartFile file) {
                if (file == null) {
                    return "null";
                }
                String originalFilename = file.getOriginalFilename();
                return originalFilename != null ? originalFilename : "unknown";
            }
        };
    }
}
