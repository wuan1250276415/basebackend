package com.basebackend.file.chunk;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分块上传服务
 * <p>
 * 支持大文件的分块上传、断点续传。
 * 使用Redis存储上传状态，支持分布式环境。
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

    /** 本地临时分块缓存（内存模式，生产建议使用Redis） */
    private final ConcurrentHashMap<String, Map<Integer, byte[]>> chunkCache = new ConcurrentHashMap<>();

    /**
     * 初始化分块上传
     *
     * @param filename    文件名
     * @param fileSize    文件总大小
     * @param fileMd5     文件MD5（可选，用于校验）
     * @param contentType 文件类型
     * @param targetPath  目标存储路径
     * @return 分块上传信息
     */
    public ChunkUploadInfo initUpload(String filename, long fileSize, String fileMd5,
            String contentType, String targetPath) {
        return initUpload(filename, fileSize, fileMd5, contentType, targetPath, DEFAULT_CHUNK_SIZE);
    }

    /**
     * 初始化分块上传（自定义分块大小）
     *
     * @param filename    文件名
     * @param fileSize    文件总大小
     * @param fileMd5     文件MD5（可选）
     * @param contentType 文件类型
     * @param targetPath  目标存储路径
     * @param chunkSize   分块大小
     * @return 分块上传信息
     */
    public ChunkUploadInfo initUpload(String filename, long fileSize, String fileMd5,
            String contentType, String targetPath, int chunkSize) {
        // 生成上传ID
        String uploadId = UUID.randomUUID().toString().replace("-", "");

        // 计算分块数
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

        // 保存到Redis
        saveUploadInfo(info);

        log.info("初始化分块上传: uploadId={}, filename={}, fileSize={}, totalChunks={}",
                uploadId, filename, fileSize, totalChunks);

        return info;
    }

    /**
     * 上传单个分块
     *
     * @param uploadId   上传ID
     * @param chunkIndex 分块索引（从0开始）
     * @param chunk      分块数据
     * @return 更新后的上传信息
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

        // 检查分块是否已上传
        if (isChunkUploaded(uploadId, chunkIndex)) {
            log.info("分块已上传，跳过: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
            return info;
        }

        try {
            // 缓存分块数据
            cacheChunk(uploadId, chunkIndex, chunk.getBytes());

            // 标记分块已上传
            markChunkUploaded(uploadId, chunkIndex);

            // 更新上传信息
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
     *
     * @param uploadId 上传ID
     * @return 最终存储路径
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

        try {
            // 合并分块
            byte[] mergedData = mergeChunks(uploadId, info.getTotalChunks());

            // 校验文件MD5
            if (info.getFileMd5() != null && !info.getFileMd5().isEmpty()) {
                String actualMd5 = calculateMd5(mergedData);
                if (!info.getFileMd5().equalsIgnoreCase(actualMd5)) {
                    throw BusinessException.paramError("文件MD5校验失败");
                }
            }

            // 上传到最终存储
            String storagePath = storageService.upload(
                    new ByteArrayInputStream(mergedData),
                    info.getTargetPath(),
                    info.getContentType(),
                    mergedData.length);

            // 更新状态为完成
            info.setStatus(ChunkUploadInfo.UploadStatus.COMPLETED);
            info.setLastUpdateTime(System.currentTimeMillis());
            saveUploadInfo(info);

            // 清理临时数据
            cleanupUpload(uploadId);

            log.info("分块上传完成: uploadId={}, path={}, size={}", uploadId, storagePath, mergedData.length);

            return storagePath;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("合并分块失败: uploadId={}, error={}", uploadId, e.getMessage());
            info.setStatus(ChunkUploadInfo.UploadStatus.FAILED);
            saveUploadInfo(info);
            throw BusinessException.fileUploadFailed("合并分块失败: " + e.getMessage());
        }
    }

    /**
     * 取消上传
     *
     * @param uploadId 上传ID
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
     *
     * @param uploadId 上传ID
     * @return 上传信息，不存在返回null
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
     *
     * @param uploadId 上传ID
     * @return 已上传的分块索引列表
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

    private void cacheChunk(String uploadId, int chunkIndex, byte[] data) {
        // 简单内存缓存实现，生产环境可改为Redis或文件存储
        chunkCache.computeIfAbsent(uploadId, k -> new ConcurrentHashMap<>())
                .put(chunkIndex, data);
    }

    private byte[] mergeChunks(String uploadId, int totalChunks) throws IOException {
        Map<Integer, byte[]> chunks = chunkCache.get(uploadId);
        if (chunks == null) {
            throw new IOException("分块数据不存在");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunkData = chunks.get(i);
            if (chunkData == null) {
                throw new IOException("分块 " + i + " 数据丢失");
            }
            outputStream.write(chunkData);
        }

        return outputStream.toByteArray();
    }

    private void cleanupUpload(String uploadId) {
        try {
            // 清理分块缓存
            chunkCache.remove(uploadId);

            // 清理Redis中的分块状态
            redisTemplate.delete(CHUNK_KEY_PREFIX + uploadId);

            log.debug("清理上传临时数据: uploadId={}", uploadId);
        } catch (Exception e) {
            log.warn("清理上传临时数据失败: uploadId={}, error={}", uploadId, e.getMessage());
        }
    }

    private String calculateMd5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
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
