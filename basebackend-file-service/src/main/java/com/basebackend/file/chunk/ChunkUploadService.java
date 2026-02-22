package com.basebackend.file.chunk;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/**
 * 分块上传服务
 * <p>
 * 支持大文件的分块上传、断点续传。
 * 使用Redis存储上传状态，磁盘临时目录存储分块数据，避免OOM。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadService {

    private final StorageService storageService;
    private final StringRedisTemplate redisTemplate;

    /** 分块上传信息缓存键前缀 */
    private static final String UPLOAD_KEY_PREFIX = "chunk_upload:";

    /** 分块状态键前缀 */
    private static final String CHUNK_KEY_PREFIX = "chunk_status:";

    /** 默认分块大小：5MB */
    private static final int DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;

    /** 上传过期时间：24小时 */
    private static final Duration UPLOAD_EXPIRE = Duration.ofHours(24);

    /** 分块临时目录根路径 */
    private static final Path CHUNK_TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"), "chunk_upload");

    /**
     * 初始化分块上传
     */
    public ChunkUploadInfo initUpload(String filename, long fileSize, String fileMd5,
            String contentType, String targetPath) {
        return initUpload(filename, fileSize, fileMd5, contentType, targetPath, DEFAULT_CHUNK_SIZE);
    }

    /**
     * 初始化分块上传（自定义分块大小）
     */
    public ChunkUploadInfo initUpload(String filename, long fileSize, String fileMd5,
            String contentType, String targetPath, int chunkSize) {
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        ChunkUploadInfo info = new ChunkUploadInfo();
        info.setUploadId(uploadId);
        info.setFilename(filename);
        info.setFileSize(fileSize);
        info.setChunkSize(chunkSize);
        info.setTotalChunks(totalChunks);
        info.setUploadedChunks(0);
        info.setFileMd5(fileMd5);
        info.setContentType(contentType);
        info.setTargetPath(targetPath);
        info.setCreateTime(System.currentTimeMillis());
        info.setLastUpdateTime(System.currentTimeMillis());
        info.setStatus(ChunkUploadInfo.UploadStatus.INITIALIZED);

        saveUploadInfo(info);

        log.info("初始化分块上传: uploadId={}, filename={}, fileSize={}, totalChunks={}",
                uploadId, filename, fileSize, totalChunks);

        return info;
    }

    /**
     * 上传单个分块
     */
    public ChunkUploadInfo uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) {
        ChunkUploadInfo info = getUploadInfo(uploadId);
        if (info == null) {
            throw BusinessException.notFound("上传任务不存在或已过期: " + uploadId);
        }

        if (!info.canContinue()) {
            throw BusinessException.paramError("上传任务状态异常: " + info.getStatus());
        }

        if (chunkIndex < 0 || chunkIndex >= info.getTotalChunks()) {
            throw BusinessException.paramError("分块索引超出范围: " + chunkIndex);
        }

        if (isChunkUploaded(uploadId, chunkIndex)) {
            log.info("分块已上传，跳过: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
            return info;
        }

        try {
            saveChunkToDisk(uploadId, chunkIndex, chunk);
            markChunkUploaded(uploadId, chunkIndex);

            info.setUploadedChunks(info.getUploadedChunks() + 1);
            info.setLastUpdateTime(System.currentTimeMillis());
            info.setStatus(ChunkUploadInfo.UploadStatus.UPLOADING);
            saveUploadInfo(info);

            log.debug("分块上传成功: uploadId={}, chunkIndex={}, progress={}%",
                    uploadId, chunkIndex, info.getProgress());

            return info;
        } catch (IOException e) {
            log.error("分块上传失败: uploadId={}, chunkIndex={}, error={}", uploadId, chunkIndex, e.getMessage());
            throw BusinessException.fileUploadFailed("分块上传失败: " + e.getMessage());
        }
    }

    /**
     * 合并分块并完成上传
     */
    public String completeUpload(String uploadId) {
        ChunkUploadInfo info = getUploadInfo(uploadId);
        if (info == null) {
            throw BusinessException.notFound("上传任务不存在或已过期: " + uploadId);
        }

        if (!info.isAllChunksUploaded()) {
            throw BusinessException.paramError(
                    String.format("分块上传未完成: %d/%d", info.getUploadedChunks(), info.getTotalChunks()));
        }

        Path mergedFile = null;
        try {
            mergedFile = mergeChunksToDisk(uploadId, info.getTotalChunks());

            if (info.getFileMd5() != null && !info.getFileMd5().isEmpty()) {
                String actualMd5 = calculateFileMd5(mergedFile);
                if (!info.getFileMd5().equalsIgnoreCase(actualMd5)) {
                    throw BusinessException.paramError("文件MD5校验失败");
                }
            }

            String storagePath;
            try (InputStream is = new BufferedInputStream(Files.newInputStream(mergedFile))) {
                storagePath = storageService.upload(
                        is,
                        info.getTargetPath(),
                        info.getContentType(),
                        Files.size(mergedFile));
            }

            info.setStatus(ChunkUploadInfo.UploadStatus.COMPLETED);
            info.setLastUpdateTime(System.currentTimeMillis());
            saveUploadInfo(info);

            cleanupUpload(uploadId);

            log.info("分块上传完成: uploadId={}, path={}, size={}", uploadId, storagePath, Files.size(mergedFile));

            return storagePath;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("合并分块失败: uploadId={}, error={}", uploadId, e.getMessage());
            info.setStatus(ChunkUploadInfo.UploadStatus.FAILED);
            saveUploadInfo(info);
            throw BusinessException.fileUploadFailed("合并分块失败: " + e.getMessage());
        } finally {
            if (mergedFile != null) {
                try {
                    Files.deleteIfExists(mergedFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 取消上传
     */
    public void cancelUpload(String uploadId) {
        ChunkUploadInfo info = getUploadInfo(uploadId);
        if (info != null) {
            info.setStatus(ChunkUploadInfo.UploadStatus.CANCELLED);
            info.setLastUpdateTime(System.currentTimeMillis());
            saveUploadInfo(info);
        }

        cleanupUpload(uploadId);
        log.info("取消分块上传: uploadId={}", uploadId);
    }

    /**
     * 获取上传信息
     */
    public ChunkUploadInfo getUploadInfo(String uploadId) {
        try {
            String key = UPLOAD_KEY_PREFIX + uploadId;
            Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

            if (data.isEmpty()) {
                return null;
            }

            ChunkUploadInfo info = new ChunkUploadInfo();
            info.setUploadId(uploadId);
            info.setFilename((String) data.get("filename"));
            info.setFileSize(Long.parseLong((String) data.get("fileSize")));
            info.setChunkSize(Integer.parseInt((String) data.get("chunkSize")));
            info.setTotalChunks(Integer.parseInt((String) data.get("totalChunks")));
            info.setUploadedChunks(Integer.parseInt((String) data.get("uploadedChunks")));
            info.setFileMd5((String) data.get("fileMd5"));
            info.setContentType((String) data.get("contentType"));
            info.setTargetPath((String) data.get("targetPath"));
            info.setCreateTime(Long.parseLong((String) data.get("createTime")));
            info.setLastUpdateTime(Long.parseLong((String) data.get("lastUpdateTime")));
            info.setStatus(ChunkUploadInfo.UploadStatus.valueOf((String) data.get("status")));

            return info;
        } catch (Exception e) {
            log.error("获取上传信息失败: uploadId={}, error={}", uploadId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取已上传的分块列表
     */
    public List<Integer> getUploadedChunks(String uploadId) {
        try {
            String key = CHUNK_KEY_PREFIX + uploadId;
            Set<String> members = redisTemplate.opsForSet().members(key);

            if (members == null || members.isEmpty()) {
                return Collections.emptyList();
            }

            return members.stream()
                    .map(Integer::parseInt)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            log.error("获取已上传分块列表失败: uploadId={}", uploadId);
            return Collections.emptyList();
        }
    }

    // ========== 私有方法 ==========

    private void saveUploadInfo(ChunkUploadInfo info) {
        String key = UPLOAD_KEY_PREFIX + info.getUploadId();
        Map<String, String> data = new HashMap<>();
        data.put("filename", info.getFilename());
        data.put("fileSize", String.valueOf(info.getFileSize()));
        data.put("chunkSize", String.valueOf(info.getChunkSize()));
        data.put("totalChunks", String.valueOf(info.getTotalChunks()));
        data.put("uploadedChunks", String.valueOf(info.getUploadedChunks()));
        data.put("fileMd5", info.getFileMd5() != null ? info.getFileMd5() : "");
        data.put("contentType", info.getContentType() != null ? info.getContentType() : "");
        data.put("targetPath", info.getTargetPath());
        data.put("createTime", String.valueOf(info.getCreateTime()));
        data.put("lastUpdateTime", String.valueOf(info.getLastUpdateTime()));
        data.put("status", info.getStatus().name());

        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, UPLOAD_EXPIRE);
    }

    private boolean isChunkUploaded(String uploadId, int chunkIndex) {
        String key = CHUNK_KEY_PREFIX + uploadId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, String.valueOf(chunkIndex));
        return Boolean.TRUE.equals(isMember);
    }

    private void markChunkUploaded(String uploadId, int chunkIndex) {
        String key = CHUNK_KEY_PREFIX + uploadId;
        redisTemplate.opsForSet().add(key, String.valueOf(chunkIndex));
        redisTemplate.expire(key, UPLOAD_EXPIRE);
    }

    private Path getChunkDir(String uploadId) {
        return CHUNK_TEMP_DIR.resolve(uploadId);
    }

    private Path getChunkFile(String uploadId, int chunkIndex) {
        return getChunkDir(uploadId).resolve(String.valueOf(chunkIndex));
    }

    private void saveChunkToDisk(String uploadId, int chunkIndex, MultipartFile chunk) throws IOException {
        Path chunkDir = getChunkDir(uploadId);
        Files.createDirectories(chunkDir);

        Path chunkFile = getChunkFile(uploadId, chunkIndex);
        chunk.transferTo(chunkFile);
    }

    private Path mergeChunksToDisk(String uploadId, int totalChunks) throws IOException {
        Path chunkDir = getChunkDir(uploadId);
        Path mergedFile = CHUNK_TEMP_DIR.resolve(uploadId + "_merged");

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(mergedFile))) {
            byte[] buffer = new byte[8192];
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = chunkDir.resolve(String.valueOf(i));
                if (!Files.exists(chunkFile)) {
                    throw new IOException("分块 " + i + " 数据丢失");
                }
                try (InputStream in = new BufferedInputStream(Files.newInputStream(chunkFile))) {
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }

        return mergedFile;
    }

    private void cleanupUpload(String uploadId) {
        try {
            // 清理磁盘临时分块文件
            Path chunkDir = getChunkDir(uploadId);
            if (Files.exists(chunkDir)) {
                try (var files = Files.walk(chunkDir)) {
                    files.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            });
                }
            }

            // 清理Redis中的分块状态
            redisTemplate.delete(CHUNK_KEY_PREFIX + uploadId);

            log.debug("清理上传临时数据: uploadId={}", uploadId);
        } catch (Exception e) {
            log.warn("清理上传临时数据失败: uploadId={}, error={}", uploadId, e.getMessage());
        }
    }

    private String calculateFileMd5(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = new DigestInputStream(
                    new BufferedInputStream(Files.newInputStream(file)), md)) {
                byte[] buffer = new byte[8192];
                while (is.read(buffer) != -1) {
                    // digest is updated automatically
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算MD5失败", e);
            return "";
        }
    }
}
