package com.basebackend.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.entity.FileOperationLog;
import com.basebackend.file.entity.FileVersion;
import com.basebackend.file.mapper.FileMetadataMapper;
import com.basebackend.file.mapper.FileOperationLogMapper;
import com.basebackend.file.mapper.FileVersionMapper;
import com.basebackend.file.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件版本管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileVersionService {

    private final StorageService storageService;
    private final FileVersionMapper fileVersionMapper;
    private final FileMetadataMapper fileMetadataMapper;
    private final FileOperationLogMapper fileOperationLogMapper;
    private final FilePermissionService filePermissionService;

    /**
     * 创建文件版本
     */
    @Transactional(rollbackFor = Exception.class)
    public FileVersion createFileVersion(String fileId, MultipartFile file,
                                          String description, Long userId, String userName) {
        try {
            FileMetadata metadata = getFileMetadata(fileId);

            InputStream inputStream = file.getInputStream();
            String md5 = DigestUtils.md5Hex(inputStream);
            inputStream.close();

            if (md5.equals(metadata.getMd5())) {
                throw new BusinessException("文件内容未发生变化");
            }

            String versionPath = generateVersionPath(fileId,
                metadata.getVersion() + 1, metadata.getFileExtension());
            storageService.upload(file.getInputStream(), versionPath,
                file.getContentType(), file.getSize());

            FileVersion version = createVersion(
                fileId, versionPath, file.getSize(), md5, description, userId, userName
            );

            metadata.setVersion(metadata.getVersion() + 1);
            metadata.setLatestVersionId(version.getId());
            metadata.setFilePath(versionPath);
            metadata.setFileSize(file.getSize());
            metadata.setMd5(md5);
            fileMetadataMapper.updateById(metadata);

            logOperation(fileId, "CREATE_VERSION", userId, userName,
                "创建版本: v" + version.getVersionNumber());
            return version;

        } catch (IOException e) {
            log.error("创建文件版本失败", e);
            throw new BusinessException("创建文件版本失败: " + e.getMessage());
        }
    }

    /**
     * 版本回退
     */
    @Transactional(rollbackFor = Exception.class)
    public void revertToVersion(String fileId, Long versionId, Long userId) {
        FileVersion targetVersion = fileVersionMapper.selectById(versionId);
        if (targetVersion == null || !targetVersion.getFileId().equals(fileId)) {
            throw new BusinessException("版本不存在");
        }

        FileMetadata metadata = getFileMetadata(fileId);

        String newPath = generateStoragePath(fileId, metadata.getFileExtension());
        storageService.copy(targetVersion.getFilePath(), newPath);

        metadata.setFilePath(newPath);
        metadata.setFileSize(targetVersion.getFileSize());
        metadata.setMd5(targetVersion.getMd5());
        fileMetadataMapper.updateById(metadata);

        logOperation(fileId, "REVERT_VERSION", userId, null,
            "回退到版本: v" + targetVersion.getVersionNumber());
        log.info("文件已回退到版本: fileId={}, version={}", fileId, targetVersion.getVersionNumber());
    }

    /**
     * 获取文件版本列表
     */
    public List<FileVersion> getFileVersions(String fileId) {
        return fileVersionMapper.selectList(
            new LambdaQueryWrapper<FileVersion>()
                .eq(FileVersion::getFileId, fileId)
                .orderByDesc(FileVersion::getVersionNumber)
        );
    }

    /**
     * 下载指定版本
     */
    public FileManagementService.FileDownloadInfo downloadVersion(String fileId,
                                                                   Long versionId, Long userId) {
        FileVersion version = fileVersionMapper.selectById(versionId);
        if (version == null || !fileId.equals(version.getFileId())) {
            throw new BusinessException("版本不存在");
        }
        if (!filePermissionService.hasPermission(fileId, userId,
                FilePermissionService.PermissionType.READ)) {
            throw new BusinessException("无权限下载该版本");
        }
        FileMetadata metadata = getFileMetadata(fileId);
        InputStream inputStream = storageService.download(version.getFilePath());

        FileManagementService.FileDownloadInfo info = new FileManagementService.FileDownloadInfo();
        info.setInputStream(inputStream);
        info.setFileName(metadata.getOriginalName() + "_v" + version.getVersionNumber());
        info.setContentType(metadata.getContentType());
        info.setFileSize(version.getFileSize());

        logOperation(fileId, "DOWNLOAD_VERSION", userId, null,
            "下载版本: v" + version.getVersionNumber());
        return info;
    }

    /**
     * 创建版本记录
     * <p>
     * 供内部方法和 {@link FileManagementService} 调用（uploadFile / copyFile）。
     */
    public FileVersion createVersion(String fileId, String filePath, Long fileSize,
                                      String md5, String description,
                                      Long userId, String userName) {
        FileVersion latestVersion = fileVersionMapper.selectOne(
            new LambdaQueryWrapper<FileVersion>()
                .eq(FileVersion::getFileId, fileId)
                .orderByDesc(FileVersion::getVersionNumber)
                .last("LIMIT 1")
        );

        int versionNumber = latestVersion != null
            ? latestVersion.getVersionNumber() + 1 : 1;

        FileVersion version = new FileVersion();
        version.setFileId(fileId);
        version.setVersionNumber(versionNumber);
        version.setFilePath(filePath);
        version.setFileSize(fileSize);
        version.setMd5(md5);
        version.setChangeDescription(description);
        version.setCreatedBy(userId);
        version.setCreatedByName(userName);
        version.setIsCurrent(true);

        fileVersionMapper.update(
            null,
            new LambdaUpdateWrapper<FileVersion>()
                .eq(FileVersion::getFileId, fileId)
                .set(FileVersion::getIsCurrent, false)
        );

        fileVersionMapper.insert(version);
        return version;
    }

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

    private String generateVersionPath(String fileId, int version, String extension) {
        return String.format("versions/%s/v%d.%s", fileId, version, extension);
    }

    private String generateStoragePath(String fileId, String extension) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%d/%02d/%02d/%s.%s",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
            fileId, extension);
    }
}
