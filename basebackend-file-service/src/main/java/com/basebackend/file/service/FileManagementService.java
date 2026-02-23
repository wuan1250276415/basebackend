package com.basebackend.file.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.dto.PageResult;
import com.basebackend.file.antivirus.AntivirusService;
import com.basebackend.file.antivirus.ScanResult;
import com.basebackend.file.config.FileProperties;
import com.basebackend.file.entity.*;
import com.basebackend.file.mapper.FileMetadataMapper;
import com.basebackend.file.mapper.FileOperationLogMapper;
import com.basebackend.file.mapper.FileRecycleBinMapper;
import com.basebackend.file.model.FileStatistics;
import com.basebackend.file.model.FileStatistics.FileTypeDistribution;
import com.basebackend.file.model.StorageUsageSummary;
import com.basebackend.file.security.FileSecurityValidator;
import com.basebackend.file.storage.StorageService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 文件管理门面服务
 * <p>
 * 保留核心 CRUD 和统计逻辑，将版本、分享、回收站、标签、权限等职责委派到专属子服务。
 * </p>
 */
@Slf4j
@Service
public class FileManagementService {

    private final StorageService storageService;
    private final FileProperties fileProperties;
    private final FileMetadataMapper fileMetadataMapper;
    private final FileRecycleBinMapper fileRecycleBinMapper;
    private final FileOperationLogMapper fileOperationLogMapper;
    private final FileSecurityValidator fileSecurityValidator;

    /** 病毒扫描服务（可选依赖，无 ClamAV 时为 null） */
    private final AntivirusService antivirusService;

    // ========== 子服务 ==========
    private final FilePermissionService filePermissionService;
    private final FileVersionService fileVersionService;
    private final FileShareService fileShareService;
    private final FileTagService fileTagService;
    private final FileRecycleBinService fileRecycleBinService;

    @Autowired
    public FileManagementService(
            StorageService storageService,
            FileProperties fileProperties,
            FileMetadataMapper fileMetadataMapper,
            FileRecycleBinMapper fileRecycleBinMapper,
            FileOperationLogMapper fileOperationLogMapper,
            FileSecurityValidator fileSecurityValidator,
            @Autowired(required = false) AntivirusService antivirusService,
            FilePermissionService filePermissionService,
            FileVersionService fileVersionService,
            FileShareService fileShareService,
            FileTagService fileTagService,
            FileRecycleBinService fileRecycleBinService) {
        this.storageService = storageService;
        this.fileProperties = fileProperties;
        this.fileMetadataMapper = fileMetadataMapper;
        this.fileRecycleBinMapper = fileRecycleBinMapper;
        this.fileOperationLogMapper = fileOperationLogMapper;
        this.fileSecurityValidator = fileSecurityValidator;
        this.antivirusService = antivirusService;
        this.filePermissionService = filePermissionService;
        this.fileVersionService = fileVersionService;
        this.fileShareService = fileShareService;
        this.fileTagService = fileTagService;
        this.fileRecycleBinService = fileRecycleBinService;
    }

    // ========================== 核心 CRUD ==========================

    /**
     * 上传文件
     */
    @Transactional(rollbackFor = Exception.class)
    public FileMetadata uploadFile(MultipartFile file, Long folderId, Long userId, String userName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 1. 安全验证：文件类型 + MIME检测 + 文件名安全性
        fileSecurityValidator.validateFile(file, fileProperties.getAllowedTypes());

        // 2. 病毒扫描（可选，无杀毒服务时跳过）
        if (antivirusService != null && antivirusService.isAvailable()) {
            try {
                ScanResult scanResult = antivirusService.scan(file.getInputStream(), file.getOriginalFilename());
                if (!scanResult.isSafe()) {
                    log.warn("文件病毒扫描未通过: filename={}, threat={}", file.getOriginalFilename(), scanResult.getThreatName());
                    throw new BusinessException("文件安全检测未通过: " + scanResult.getMessage());
                }
                log.debug("文件病毒扫描通过: filename={}, engine={}, cost={}ms",
                    file.getOriginalFilename(), scanResult.getEngineName(), scanResult.getScanTimeMs());
            } catch (IOException e) {
                log.error("病毒扫描IO异常", e);
                throw new BusinessException("文件安全检测失败: " + e.getMessage());
            }
        }

        try {
            // 3. 生成文件标识
            String fileId = IdUtil.simpleUUID();
            String originalName = file.getOriginalFilename();
            String extension = FileUtil.extName(originalName);

            // 4. 计算MD5
            InputStream inputStream = file.getInputStream();
            String md5 = DigestUtils.md5Hex(inputStream);
            inputStream.close();

            // 5. 检查文件是否已存在（去重）
            FileMetadata existingFile = fileMetadataMapper.selectOne(
                new LambdaQueryWrapper<FileMetadata>()
                    .eq(FileMetadata::getMd5, md5)
                    .eq(FileMetadata::getIsDeleted, false)
                    .last("LIMIT 1")
            );

            if (existingFile != null) {
                log.info("文件已存在，返回已有文件: fileId={}", existingFile.getFileId());
                return existingFile;
            }

            // 6. 生成存储路径
            String storagePath = generateStoragePath(fileId, extension);

            // 7. 上传到存储后端
            String url = storageService.upload(
                file.getInputStream(),
                storagePath,
                file.getContentType(),
                file.getSize()
            );

            // 8. 创建文件元数据
            FileMetadata metadata = new FileMetadata();
            metadata.setFileId(fileId);
            metadata.setFileName(fileId + "." + extension);
            metadata.setOriginalName(originalName);
            metadata.setFilePath(storagePath);
            metadata.setFileSize(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setFileExtension(extension);
            metadata.setMd5(md5);
            metadata.setStorageType(storageService.getStorageType().name());
            metadata.setFolderId(folderId);
            metadata.setOwnerId(userId);
            metadata.setOwnerName(userName);
            metadata.setIsPublic(false);
            metadata.setIsFolder(false);
            metadata.setVersion(1);
            metadata.setDownloadCount(0);
            metadata.setViewCount(0);

            fileMetadataMapper.insert(metadata);

            // 9. 创建初始版本
            fileVersionService.createVersion(fileId, storagePath, file.getSize(), md5, "初始版本", userId, userName);

            // 10. 记录操作日志
            logOperation(fileId, "UPLOAD", userId, userName, "上传文件: " + originalName);

            log.info("文件上传成功: fileId={}, path={}", fileId, storagePath);
            return metadata;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    public FileDownloadInfo downloadFile(String fileId, Long userId) {
        FileMetadata metadata = getFileMetadata(fileId);

        if (!filePermissionService.hasPermission(fileId, userId, FilePermissionService.PermissionType.READ)) {
            throw new BusinessException("无权限下载该文件");
        }

        InputStream inputStream = storageService.download(metadata.getFilePath());

        // 原子更新下载次数（避免并发丢失计数）
        fileMetadataMapper.update(null,
            new LambdaUpdateWrapper<FileMetadata>()
                .eq(FileMetadata::getFileId, fileId)
                .setSql("download_count = download_count + 1"));

        logOperation(fileId, "DOWNLOAD", userId, null, "下载文件");

        FileDownloadInfo downloadInfo = new FileDownloadInfo();
        downloadInfo.setInputStream(inputStream);
        downloadInfo.setFileName(metadata.getOriginalName());
        downloadInfo.setContentType(metadata.getContentType());
        downloadInfo.setFileSize(metadata.getFileSize());
        return downloadInfo;
    }

    /**
     * 删除文件（移入回收站）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String fileId, Long userId, String userName) {
        FileMetadata metadata = getFileMetadata(fileId);

        if (!filePermissionService.hasPermission(fileId, userId, FilePermissionService.PermissionType.DELETE)) {
            throw new BusinessException("无权限删除该文件");
        }

        // 移入回收站
        FileRecycleBin recycleBin = new FileRecycleBin();
        recycleBin.setFileId(fileId);
        recycleBin.setFileName(metadata.getOriginalName());
        recycleBin.setFilePath(metadata.getFilePath());
        recycleBin.setFileSize(metadata.getFileSize());
        recycleBin.setDeletedBy(userId);
        recycleBin.setDeletedByName(userName);
        recycleBin.setDeletedAt(LocalDateTime.now());
        recycleBin.setExpireAt(LocalDateTime.now().plusDays(30));
        recycleBin.setOriginalMetadata(JSONUtil.toJsonStr(metadata));
        fileRecycleBinMapper.insert(recycleBin);

        // 软删除文件元数据
        metadata.setIsDeleted(true);
        metadata.setDeletedAt(LocalDateTime.now());
        metadata.setDeletedBy(userId);
        fileMetadataMapper.updateById(metadata);

        logOperation(fileId, "DELETE", userId, userName, "删除文件");
        log.info("文件已移入回收站: fileId={}", fileId);
    }

    /**
     * 获取文件列表
     */
    public PageResult<FileMetadata> listFiles(
            String fileName,
            String fileExtension,
            Long folderId,
            Long ownerId,
            Boolean isPublic,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long current,
            long size) {
        Page<FileMetadata> page = new Page<>(current, size);
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getIsDeleted, false)
            .isNull(FileMetadata::getDeletedBy)
            .like(StringUtils.hasText(fileName), FileMetadata::getOriginalName, fileName)
            .eq(StringUtils.hasText(fileExtension), FileMetadata::getFileExtension, fileExtension)
            .eq(folderId != null, FileMetadata::getFolderId, folderId)
            .eq(ownerId != null, FileMetadata::getOwnerId, ownerId)
            .eq(isPublic != null, FileMetadata::getIsPublic, isPublic)
            .ge(startTime != null, FileMetadata::getCreateTime, startTime)
            .le(endTime != null, FileMetadata::getCreateTime, endTime)
            .orderByDesc(FileMetadata::getUpdateTime);

        Page<FileMetadata> result = fileMetadataMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取文件详情
     */
    public FileMetadata getFileDetail(String fileId) {
        return getFileMetadata(fileId);
    }

    /**
     * 批量删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteFiles(List<String> fileIds, Long userId, String userName) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return;
        }
        for (String fileId : fileIds) {
            deleteFile(fileId, userId, userName);
        }
    }

    /**
     * 重命名文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void renameFile(String fileId, String newName, Long userId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (!filePermissionService.hasPermission(fileId, userId, FilePermissionService.PermissionType.WRITE)) {
            throw new BusinessException("无权限重命名该文件");
        }
        metadata.setOriginalName(newName);
        fileMetadataMapper.updateById(metadata);
        logOperation(fileId, "RENAME", userId, null, "重命名为: " + newName);
    }

    /**
     * 移动文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveFile(String fileId, Long targetFolderId, Long userId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (!filePermissionService.hasPermission(fileId, userId, FilePermissionService.PermissionType.WRITE)) {
            throw new BusinessException("无权限移动该文件");
        }
        metadata.setFolderId(targetFolderId);
        fileMetadataMapper.updateById(metadata);
        logOperation(fileId, "MOVE", userId, null, "移动到文件夹: " + targetFolderId);
    }

    /**
     * 复制文件
     */
    @Transactional(rollbackFor = Exception.class)
    public FileMetadata copyFile(String fileId, Long targetFolderId, Long userId, String userName) {
        FileMetadata source = getFileMetadata(fileId);
        if (!filePermissionService.hasPermission(fileId, userId, FilePermissionService.PermissionType.READ)) {
            throw new BusinessException("无权限复制该文件");
        }

        String newFileId = IdUtil.simpleUUID();
        String extension = source.getFileExtension();
        String newStoragePath = generateStoragePath(newFileId, extension);
        storageService.copy(source.getFilePath(), newStoragePath);

        FileMetadata copy = new FileMetadata();
        copy.setFileId(newFileId);
        copy.setFileName(newFileId + "." + extension);
        copy.setOriginalName(source.getOriginalName());
        copy.setFilePath(newStoragePath);
        copy.setFileSize(source.getFileSize());
        copy.setContentType(source.getContentType());
        copy.setFileExtension(extension);
        copy.setMd5(source.getMd5());
        copy.setSha256(source.getSha256());
        copy.setStorageType(source.getStorageType());
        copy.setBucketName(source.getBucketName());
        copy.setFolderId(targetFolderId != null ? targetFolderId : source.getFolderId());
        copy.setFolderPath(source.getFolderPath());
        copy.setIsFolder(false);
        copy.setOwnerId(userId);
        copy.setOwnerName(userName);
        copy.setIsPublic(false);
        copy.setIsDeleted(false);
        copy.setVersion(1);
        copy.setDownloadCount(0);
        copy.setViewCount(0);
        copy.setThumbnailPath(source.getThumbnailPath());
        copy.setTags(source.getTags());
        copy.setDescription(source.getDescription());
        copy.setMetadata(source.getMetadata());

        fileMetadataMapper.insert(copy);
        fileVersionService.createVersion(newFileId, newStoragePath, source.getFileSize(), source.getMd5(),
            "复制自文件: " + fileId, userId, userName);

        logOperation(newFileId, "COPY", userId, userName, "从文件复制: " + fileId);
        return copy;
    }

    /**
     * 获取文件预览URL
     */
    public String getFilePreviewUrl(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        return storageService.getPresignedUrl(metadata.getFilePath(), 604800);
    }

    /**
     * 获取文件缩略图URL
     */
    public String getThumbnailUrl(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (StringUtils.hasText(metadata.getThumbnailPath())) {
            return storageService.getPresignedUrl(metadata.getThumbnailPath(), 604800);
        }
        return storageService.getPresignedUrl(metadata.getFilePath(), 604800);
    }

    // ========================== 统计 / 日志 ==========================

    /**
     * 获取文件操作日志
     */
    public PageResult<FileOperationLog> getFileOperationLogs(String fileId, long current, long size) {
        Page<FileOperationLog> page = new Page<>(current, size);
        LambdaQueryWrapper<FileOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileOperationLog::getFileId, fileId)
            .orderByDesc(FileOperationLog::getOperationTime);
        Page<FileOperationLog> result = fileOperationLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取文件统计信息（SQL聚合，避免全表加载到内存）
     */
    public FileStatistics getFileStatistics() {
        Map<String, Object> summary = fileMetadataMapper.selectFileSummary();
        long totalFiles = ((Number) summary.get("total_files")).longValue();
        long totalSize = ((Number) summary.get("total_size")).longValue();

        FileStatistics statistics = new FileStatistics();
        statistics.setTotalFiles(totalFiles);
        statistics.setTotalSize(totalSize);

        List<Map<String, Object>> storageStats = fileMetadataMapper.selectStorageStatistics();
        Map<String, Long> storageSizeMap = new HashMap<>();
        for (Map<String, Object> row : storageStats) {
            String storageType = (String) row.get("storage_type");
            long sz = ((Number) row.get("total_size")).longValue();
            storageSizeMap.put(storageType, sz);
        }

        FileStatistics.StorageUsage usage = new FileStatistics.StorageUsage();
        usage.setLocal(storageSizeMap.getOrDefault(StorageService.StorageType.LOCAL.name(), 0L));
        usage.setMinio(storageSizeMap.getOrDefault(StorageService.StorageType.MINIO.name(), 0L));
        usage.setOss(storageSizeMap.getOrDefault(StorageService.StorageType.ALIYUN_OSS.name(), 0L));
        usage.setS3(storageSizeMap.getOrDefault(StorageService.StorageType.AWS_S3.name(), 0L));
        statistics.setStorageUsage(usage);

        List<Map<String, Object>> typeStats = fileMetadataMapper.selectFileTypeDistribution();
        List<FileTypeDistribution> distributions = new ArrayList<>();
        for (Map<String, Object> row : typeStats) {
            FileTypeDistribution dist = new FileTypeDistribution();
            dist.setType((String) row.get("file_type"));
            dist.setCount(((Number) row.get("file_count")).longValue());
            dist.setSize(((Number) row.get("total_size")).longValue());
            distributions.add(dist);
        }
        statistics.setFileTypeDistribution(distributions);
        return statistics;
    }

    /**
     * 获取存储使用情况
     */
    public StorageUsageSummary getStorageUsage() {
        Path uploadPath = Paths.get(fileProperties.getUploadPath());
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                log.warn("创建上传目录失败: {}", uploadPath, e);
            }
        }

        File store = uploadPath.toFile();
        long totalSpace = store.getTotalSpace();
        long usableSpace = store.getUsableSpace();
        long usedSpace = Math.max(0L, totalSpace - usableSpace);

        if (totalSpace <= 0) {
            totalSpace = Math.max(usedSpace, 1L);
        }

        StorageUsageSummary storageSummary = new StorageUsageSummary();
        storageSummary.setUsed(usedSpace);
        storageSummary.setTotal(totalSpace);
        storageSummary.setPercentage(totalSpace == 0 ? 0D : Math.round(usedSpace * 10000D / totalSpace) / 100D);
        return storageSummary;
    }

    // ========================== 门面委派方法 ==========================

    // --- 权限 ---

    public boolean hasPermission(String fileId, Long userId, PermissionType type) {
        return filePermissionService.hasPermission(fileId, userId, toInternalPermission(type));
    }

    public List<FilePermission> getFilePermissions(String fileId) {
        return filePermissionService.getFilePermissions(fileId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFilePermission(String fileId, FilePermission permission, Long userId, String userName) {
        filePermissionService.addFilePermission(fileId, permission, userId, userName);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFilePermission(String fileId, Long permissionId, Long userId) {
        filePermissionService.deleteFilePermission(fileId, permissionId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setFilePublic(String fileId, Boolean isPublic, Long userId) {
        filePermissionService.setFilePublic(fileId, isPublic, userId);
    }

    // --- 版本 ---

    @Transactional(rollbackFor = Exception.class)
    public FileVersion createFileVersion(String fileId, MultipartFile file, String description, Long userId, String userName) {
        return fileVersionService.createFileVersion(fileId, file, description, userId, userName);
    }

    @Transactional(rollbackFor = Exception.class)
    public void revertToVersion(String fileId, Long versionId, Long userId) {
        fileVersionService.revertToVersion(fileId, versionId, userId);
    }

    public List<FileVersion> getFileVersions(String fileId) {
        return fileVersionService.getFileVersions(fileId);
    }

    public FileDownloadInfo downloadVersion(String fileId, Long versionId, Long userId) {
        return fileVersionService.downloadVersion(fileId, versionId, userId);
    }

    // --- 分享 ---

    @Transactional(rollbackFor = Exception.class)
    public FileShare createFileShare(
            String fileId, String sharePassword, LocalDateTime expireTime,
            Integer downloadLimit, Boolean allowDownload, Boolean allowPreview,
            Long userId, String userName) {
        return fileShareService.createFileShare(fileId, sharePassword, expireTime,
            downloadLimit, allowDownload, allowPreview, userId, userName);
    }

    public FileShare getShareInfo(String shareCode, String password) {
        return fileShareService.getShareInfo(shareCode, password);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(Long shareId, Long userId) {
        fileShareService.cancelShare(shareId, userId);
    }

    public PageResult<FileShare> getMyShares(Long userId, long current, long size) {
        return fileShareService.getMyShares(userId, current, size);
    }

    // --- 标签 ---

    public List<FileTag> getAllTags() {
        return fileTagService.getAllTags();
    }

    @Transactional(rollbackFor = Exception.class)
    public FileTag createTag(FileTag tag, Long userId, String userName) {
        return fileTagService.createTag(tag, userId, userName);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFileTag(String fileId, Long tagId, Long userId, String userName) {
        fileTagService.addFileTag(fileId, tagId, userId, userName);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFileTag(String fileId, Long tagId, Long userId) {
        fileTagService.removeFileTag(fileId, tagId, userId);
    }

    public List<FileTag> getFileTags(String fileId) {
        return fileTagService.getFileTags(fileId);
    }

    // --- 回收站 ---

    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(String fileId, Long userId) {
        fileRecycleBinService.restoreFile(fileId, userId);
    }

    public PageResult<FileRecycleBin> getRecycleBinList(long current, long size) {
        return fileRecycleBinService.getRecycleBinList(current, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public void permanentDeleteFile(String fileId, Long userId) {
        fileRecycleBinService.permanentDeleteFile(fileId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchRestoreFiles(List<String> fileIds, Long userId) {
        fileRecycleBinService.batchRestoreFiles(fileIds, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchPermanentDelete(List<String> fileIds, Long userId) {
        fileRecycleBinService.batchPermanentDelete(fileIds, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void emptyRecycleBin(Long userId) {
        fileRecycleBinService.emptyRecycleBin(userId);
    }

    // ========================== 私有辅助方法 ==========================

    private FileMetadata getFileMetadata(String fileId) {
        FileMetadata metadata = fileMetadataMapper.selectOne(
            new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getFileId, fileId)
                .eq(FileMetadata::getIsDeleted, false)
        );
        if (metadata == null) {
            throw new BusinessException("文件不存在");
        }
        return metadata;
    }

    private void logOperation(String fileId, String operationType, Long operatorId,
                               String operatorName, String detail) {
        FileOperationLog operationLog = new FileOperationLog();
        operationLog.setFileId(fileId);
        operationLog.setOperationType(operationType);
        operationLog.setOperatorId(operatorId);
        operationLog.setOperatorName(operatorName);
        operationLog.setOperationDetail(detail);
        operationLog.setOperationTime(LocalDateTime.now());
        fileOperationLogMapper.insert(operationLog);
    }

    private String generateStoragePath(String fileId, String extension) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%d/%02d/%02d/%s.%s",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
            fileId, extension);
    }

    private FilePermissionService.PermissionType toInternalPermission(PermissionType type) {
        return FilePermissionService.PermissionType.valueOf(type.name());
    }

    // ========================== 内部类型 ==========================

    /**
     * 权限类型枚举（保持向后兼容，映射到 FilePermissionService.PermissionType）
     */
    public enum PermissionType {
        READ, WRITE, DELETE, SHARE
    }

    /**
     * 文件下载信息
     */
    @Data
    public static class FileDownloadInfo {
        private InputStream inputStream;
        private String fileName;
        private String contentType;
        private Long fileSize;
    }
}
