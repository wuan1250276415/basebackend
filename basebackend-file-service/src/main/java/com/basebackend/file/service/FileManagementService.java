package com.basebackend.file.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.PageResult;
import com.basebackend.file.config.FileProperties;
import com.basebackend.file.entity.*;
import com.basebackend.file.mapper.*;
import com.basebackend.file.model.FileStatistics;
import com.basebackend.file.model.FileStatistics.FileTypeDistribution;
import com.basebackend.file.model.StorageUsageSummary;
import com.basebackend.file.storage.StorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.stream.Collectors;

/**
 * 文件管理核心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileManagementService {

    private final StorageService storageService;
    private final FileProperties fileProperties;
    private final FileMetadataMapper fileMetadataMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FilePermissionMapper filePermissionMapper;
    private final FileRecycleBinMapper fileRecycleBinMapper;
    private final FileOperationLogMapper fileOperationLogMapper;
    private final FileShareMapper fileShareMapper;
    private final FileTagMapper fileTagMapper;
    private final FileTagRelationMapper fileTagRelationMapper;

    /**
     * 上传文件
     */
    @Transactional(rollbackFor = Exception.class)
    public FileMetadata uploadFile(MultipartFile file, Long folderId, Long userId, String userName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        try {
            // 1. 生成文件标识
            String fileId = IdUtil.simpleUUID();
            String originalName = file.getOriginalFilename();
            String extension = FileUtil.extName(originalName);

            // 2. 计算MD5
            InputStream inputStream = file.getInputStream();
            String md5 = DigestUtils.md5Hex(inputStream);
            inputStream.close();

            // 3. 检查文件是否已存在（去重）
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

            // 4. 生成存储路径
            String storagePath = generateStoragePath(fileId, extension);

            // 5. 上传到存储后端
            String url = storageService.upload(
                file.getInputStream(),
                storagePath,
                file.getContentType(),
                file.getSize()
            );

            // 6. 创建文件元数据
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

            // 7. 创建初始版本
            createVersion(fileId, storagePath, file.getSize(), md5, "初始版本", userId, userName);

            // 8. 记录操作日志
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
        // 1. 检查文件存在性
        FileMetadata metadata = getFileMetadata(fileId);

        // 2. 检查权限
        if (!hasPermission(fileId, userId, PermissionType.READ)) {
            throw new BusinessException("无权限下载该文件");
        }

        // 3. 获取文件流
        InputStream inputStream = storageService.download(metadata.getFilePath());

        // 4. 更新下载次数
        metadata.setDownloadCount(metadata.getDownloadCount() + 1);
        fileMetadataMapper.updateById(metadata);

        // 5. 记录操作日志
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
        // 1. 检查文件存在性
        FileMetadata metadata = getFileMetadata(fileId);

        // 2. 检查权限
        if (!hasPermission(fileId, userId, PermissionType.DELETE)) {
            throw new BusinessException("无权限删除该文件");
        }

        // 3. 移入回收站
        FileRecycleBin recycleBin = new FileRecycleBin();
        recycleBin.setFileId(fileId);
        recycleBin.setFileName(metadata.getOriginalName());
        recycleBin.setFilePath(metadata.getFilePath());
        recycleBin.setFileSize(metadata.getFileSize());
        recycleBin.setDeletedBy(userId);
        recycleBin.setDeletedByName(userName);
        recycleBin.setDeletedAt(LocalDateTime.now());
        recycleBin.setExpireAt(LocalDateTime.now().plusDays(30)); // 30天后自动删除
        recycleBin.setOriginalMetadata(JSONUtil.toJsonStr(metadata));

        fileRecycleBinMapper.insert(recycleBin);

        // 4. 软删除文件元数据
        metadata.setIsDeleted(true);
        metadata.setDeletedAt(LocalDateTime.now());
        metadata.setDeletedBy(userId);
        fileMetadataMapper.updateById(metadata);

        // 5. 记录操作日志
        logOperation(fileId, "DELETE", userId, userName, "删除文件");

        log.info("文件已移入回收站: fileId={}", fileId);
    }

    /**
     * 恢复文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(String fileId, Long userId) {
        // 1. 检查回收站记录
        FileRecycleBin recycleBin = fileRecycleBinMapper.selectOne(
            new LambdaQueryWrapper<FileRecycleBin>()
                .eq(FileRecycleBin::getFileId, fileId)
        );

        if (recycleBin == null) {
            throw new BusinessException("回收站中未找到该文件");
        }

        // 2. 恢复文件元数据
        FileMetadata metadata = fileMetadataMapper.selectOne(
            new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getFileId, fileId)
        );

        if (metadata != null) {
            metadata.setIsDeleted(false);
            metadata.setDeletedAt(null);
            metadata.setDeletedBy(null);
            fileMetadataMapper.updateById(metadata);
        }

        // 3. 删除回收站记录
        fileRecycleBinMapper.deleteById(recycleBin.getId());

        // 4. 记录操作日志
        logOperation(fileId, "RECOVER", userId, null, "恢复文件");

        log.info("文件已恢复: fileId={}", fileId);
    }

    /**
     * 创建文件版本
     */
    @Transactional(rollbackFor = Exception.class)
    public FileVersion createFileVersion(String fileId, MultipartFile file, String description, Long userId, String userName) {
        try {
            // 1. 获取原文件元数据
            FileMetadata metadata = getFileMetadata(fileId);

            // 2. 计算MD5
            InputStream inputStream = file.getInputStream();
            String md5 = DigestUtils.md5Hex(inputStream);
            inputStream.close();

            // 3. 检查是否有变化
            if (md5.equals(metadata.getMd5())) {
                throw new BusinessException("文件内容未发生变化");
            }

            // 4. 上传新版本文件
            String versionPath = generateVersionPath(fileId, metadata.getVersion() + 1, metadata.getFileExtension());
            storageService.upload(file.getInputStream(), versionPath, file.getContentType(), file.getSize());

            // 5. 创建版本记录
            FileVersion version = createVersion(
                fileId,
                versionPath,
                file.getSize(),
                md5,
                description,
                userId,
                userName
            );

            // 6. 更新文件元数据
            metadata.setVersion(metadata.getVersion() + 1);
            metadata.setLatestVersionId(version.getId());
            metadata.setFilePath(versionPath);
            metadata.setFileSize(file.getSize());
            metadata.setMd5(md5);
            fileMetadataMapper.updateById(metadata);

            // 7. 记录操作日志
            logOperation(fileId, "CREATE_VERSION", userId, userName, "创建版本: v" + version.getVersionNumber());

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
        // 1. 获取目标版本
        FileVersion targetVersion = fileVersionMapper.selectById(versionId);
        if (targetVersion == null || !targetVersion.getFileId().equals(fileId)) {
            throw new BusinessException("版本不存在");
        }

        // 2. 获取文件元数据
        FileMetadata metadata = getFileMetadata(fileId);

        // 3. 复制目标版本文件
        String newPath = generateStoragePath(fileId, metadata.getFileExtension());
        storageService.copy(targetVersion.getFilePath(), newPath);

        // 4. 更新文件元数据
        metadata.setFilePath(newPath);
        metadata.setFileSize(targetVersion.getFileSize());
        metadata.setMd5(targetVersion.getMd5());
        fileMetadataMapper.updateById(metadata);

        // 5. 记录操作日志
        logOperation(fileId, "REVERT_VERSION", userId, null,
            "回退到版本: v" + targetVersion.getVersionNumber());

        log.info("文件已回退到版本: fileId={}, version={}", fileId, targetVersion.getVersionNumber());
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
        long size
    ) {
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
        if (!hasPermission(fileId, userId, PermissionType.WRITE)) {
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
        if (!hasPermission(fileId, userId, PermissionType.WRITE)) {
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
        if (!hasPermission(fileId, userId, PermissionType.READ)) {
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
        createVersion(newFileId, newStoragePath, source.getFileSize(), source.getMd5(),
            "复制自文件: " + fileId, userId, userName);

        logOperation(newFileId, "COPY", userId, userName, "从文件复制: " + fileId);
        return copy;
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
    public FileDownloadInfo downloadVersion(String fileId, Long versionId, Long userId) {
        FileVersion version = fileVersionMapper.selectById(versionId);
        if (version == null || !fileId.equals(version.getFileId())) {
            throw new BusinessException("版本不存在");
        }
        if (!hasPermission(fileId, userId, PermissionType.READ)) {
            throw new BusinessException("无权限下载该版本");
        }
        FileMetadata metadata = getFileMetadata(fileId);
        InputStream inputStream = storageService.download(version.getFilePath());

        FileDownloadInfo info = new FileDownloadInfo();
        info.setInputStream(inputStream);
        info.setFileName(metadata.getOriginalName() + "_v" + version.getVersionNumber());
        info.setContentType(metadata.getContentType());
        info.setFileSize(version.getFileSize());

        logOperation(fileId, "DOWNLOAD_VERSION", userId, null,
            "下载版本: v" + version.getVersionNumber());
        return info;
    }

    /**
     * 获取回收站列表
     */
    public PageResult<FileRecycleBin> getRecycleBinList(long current, long size) {
        Page<FileRecycleBin> page = new Page<>(current, size);
        LambdaQueryWrapper<FileRecycleBin> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FileRecycleBin::getDeletedAt);
        Page<FileRecycleBin> result = fileRecycleBinMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 彻底删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void permanentDeleteFile(String fileId, Long userId) {
        FileMetadata metadata = fileMetadataMapper.selectOne(
            new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getFileId, fileId)
        );

        if (metadata != null) {
            if (!hasPermission(fileId, userId, PermissionType.DELETE)) {
                throw new BusinessException("无权限删除该文件");
            }
            storageService.delete(metadata.getFilePath());
            fileMetadataMapper.deleteById(metadata.getId());
        }

        fileVersionMapper.delete(
            new LambdaQueryWrapper<FileVersion>()
                .eq(FileVersion::getFileId, fileId)
        );
        filePermissionMapper.delete(
            new LambdaQueryWrapper<FilePermission>()
                .eq(FilePermission::getFileId, fileId)
        );
        fileTagRelationMapper.delete(
            new LambdaQueryWrapper<FileTagRelation>()
                .eq(FileTagRelation::getFileId, fileId)
        );
        fileShareMapper.delete(
            new LambdaQueryWrapper<FileShare>()
                .eq(FileShare::getFileId, fileId)
        );
        fileRecycleBinMapper.delete(
            new LambdaQueryWrapper<FileRecycleBin>()
                .eq(FileRecycleBin::getFileId, fileId)
        );

        logOperation(fileId, "PERMANENT_DELETE", userId, null, "彻底删除文件");
    }

    /**
     * 批量恢复文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchRestoreFiles(List<String> fileIds, Long userId) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return;
        }
        for (String fileId : fileIds) {
            restoreFile(fileId, userId);
        }
    }

    /**
     * 批量彻底删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchPermanentDelete(List<String> fileIds, Long userId) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return;
        }
        for (String fileId : fileIds) {
            permanentDeleteFile(fileId, userId);
        }
    }

    /**
     * 清空回收站
     */
    @Transactional(rollbackFor = Exception.class)
    public void emptyRecycleBin(Long userId) {
        List<FileRecycleBin> recycleList = fileRecycleBinMapper.selectList(new LambdaQueryWrapper<>());
        if (CollectionUtils.isEmpty(recycleList)) {
            return;
        }
        for (FileRecycleBin record : recycleList) {
            permanentDeleteFile(record.getFileId(), userId);
        }
    }

    /**
     * 获取文件权限
     */
    public List<FilePermission> getFilePermissions(String fileId) {
        return filePermissionMapper.selectList(
            new LambdaQueryWrapper<FilePermission>()
                .eq(FilePermission::getFileId, fileId)
                .orderByDesc(FilePermission::getCreateTime)
        );
    }

    /**
     * 添加文件权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFilePermission(String fileId, FilePermission permission, Long userId, String userName) {
        if (permission == null) {
            throw new BusinessException("权限信息不能为空");
        }
        if (!hasPermission(fileId, userId, PermissionType.SHARE)) {
            throw new BusinessException("无权限分配该文件权限");
        }
        permission.setId(null);
        permission.setFileId(fileId);
        permission.setGrantedBy(userId);
        permission.setGrantedByName(userName);
        filePermissionMapper.insert(permission);

        logOperation(fileId, "GRANT_PERMISSION", userId, userName,
            "授予权限: " + permission.getPermissionType());
    }

    /**
     * 删除文件权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFilePermission(String fileId, Long permissionId, Long userId) {
        FilePermission permission = filePermissionMapper.selectById(permissionId);
        if (permission == null || !fileId.equals(permission.getFileId())) {
            throw new BusinessException("权限记录不存在");
        }
        if (!hasPermission(fileId, userId, PermissionType.SHARE)) {
            throw new BusinessException("无权限删除该文件权限");
        }
        filePermissionMapper.deleteById(permissionId);
        logOperation(fileId, "REMOVE_PERMISSION", userId, null,
            "删除权限: " + permission.getPermissionType());
    }

    /**
     * 设置文件公开状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void setFilePublic(String fileId, Boolean isPublic, Long userId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (!hasPermission(fileId, userId, PermissionType.SHARE)) {
            throw new BusinessException("无权限修改文件公开状态");
        }
        metadata.setIsPublic(Boolean.TRUE.equals(isPublic));
        fileMetadataMapper.updateById(metadata);

        logOperation(fileId, Boolean.TRUE.equals(isPublic) ? "SET_PUBLIC" : "SET_PRIVATE",
            userId, null, "设置公开状态: " + isPublic);
    }

    /**
     * 创建文件分享
     */
    @Transactional(rollbackFor = Exception.class)
    public FileShare createFileShare(
        String fileId,
        String sharePassword,
        LocalDateTime expireTime,
        Integer downloadLimit,
        Boolean allowDownload,
        Boolean allowPreview,
        Long userId,
        String userName
    ) {
        if (!hasPermission(fileId, userId, PermissionType.SHARE)) {
            throw new BusinessException("无权限分享该文件");
        }
        FileMetadata metadata = getFileMetadata(fileId);

        String shareCode = IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase(Locale.ROOT);
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setShareCode(shareCode);
        share.setSharePassword(sharePassword);
        share.setSharedBy(userId);
        share.setSharedByName(userName);
        share.setExpireTime(expireTime);
        share.setDownloadLimit(downloadLimit);
        share.setDownloadCount(0);
        share.setViewCount(0);
        share.setAllowDownload(allowDownload == null || allowDownload);
        share.setAllowPreview(allowPreview == null || allowPreview);
        share.setStatus("ACTIVE");
        fileShareMapper.insert(share);

        logOperation(fileId, "CREATE_SHARE", userId, userName,
            "创建分享，分享码: " + shareCode + "，文件: " + metadata.getOriginalName());
        return share;
    }

    /**
     * 获取分享信息
     */
    public FileShare getShareInfo(String shareCode, String password) {
        FileShare share = fileShareMapper.selectOne(
            new LambdaQueryWrapper<FileShare>()
                .eq(FileShare::getShareCode, shareCode)
                .last("LIMIT 1")
        );
        if (share == null) {
            throw new BusinessException("分享不存在");
        }
        if (!"ACTIVE".equals(share.getStatus())) {
            throw new BusinessException("分享已失效");
        }
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            share.setStatus("EXPIRED");
            fileShareMapper.updateById(share);
            throw new BusinessException("分享已过期");
        }
        if (StringUtils.hasText(share.getSharePassword())
            && !StringUtils.hasText(password)) {
            throw new BusinessException("需要分享密码");
        }
        if (StringUtils.hasText(share.getSharePassword())
            && !share.getSharePassword().equals(password)) {
            throw new BusinessException("分享密码不正确");
        }

        share.setViewCount(share.getViewCount() == null ? 1 : share.getViewCount() + 1);
        fileShareMapper.updateById(share);
        return share;
    }

    /**
     * 取消分享
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(Long shareId, Long userId) {
        FileShare share = fileShareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException("分享不存在");
        }
        if (!Objects.equals(share.getSharedBy(), userId)) {
            throw new BusinessException("无权限取消该分享");
        }
        share.setStatus("CANCELLED");
        fileShareMapper.updateById(share);

        logOperation(share.getFileId(), "CANCEL_SHARE", userId, null,
            "取消分享，分享码: " + share.getShareCode());
    }

    /**
     * 获取我的分享列表
     */
    public PageResult<FileShare> getMyShares(Long userId, long current, long size) {
        Page<FileShare> page = new Page<>(current, size);
        LambdaQueryWrapper<FileShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileShare::getSharedBy, userId)
            .orderByDesc(FileShare::getCreateTime);
        Page<FileShare> result = fileShareMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取所有标签
     */
    public List<FileTag> getAllTags() {
        return fileTagMapper.selectList(
            new LambdaQueryWrapper<FileTag>()
                .orderByAsc(FileTag::getTagName)
        );
    }

    /**
     * 创建标签
     */
    @Transactional(rollbackFor = Exception.class)
    public FileTag createTag(FileTag tag, Long userId, String userName) {
        tag.setId(null);
        tag.setCreatedBy(userId);
        tag.setCreatedByName(userName);
        tag.setUsageCount(0);
        fileTagMapper.insert(tag);
        return tag;
    }

    /**
     * 为文件添加标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFileTag(String fileId, Long tagId, Long userId, String userName) {
        FileTagRelation existing = fileTagRelationMapper.selectOne(
            new LambdaQueryWrapper<FileTagRelation>()
                .eq(FileTagRelation::getFileId, fileId)
                .eq(FileTagRelation::getTagId, tagId)
                .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        FileTagRelation relation = new FileTagRelation();
        relation.setFileId(fileId);
        relation.setTagId(tagId);
        relation.setCreatedBy(userId);
        relation.setCreatedByName(userName);
        fileTagRelationMapper.insert(relation);

        FileTag tag = fileTagMapper.selectById(tagId);
        if (tag != null) {
            tag.setUsageCount((tag.getUsageCount() == null ? 0 : tag.getUsageCount()) + 1);
            fileTagMapper.updateById(tag);
        }

        logOperation(fileId, "ADD_TAG", userId, userName, "添加标签: " + tagId);
    }

    /**
     * 为文件移除标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeFileTag(String fileId, Long tagId, Long userId) {
        fileTagRelationMapper.delete(
            new LambdaQueryWrapper<FileTagRelation>()
                .eq(FileTagRelation::getFileId, fileId)
                .eq(FileTagRelation::getTagId, tagId)
        );

        FileTag tag = fileTagMapper.selectById(tagId);
        if (tag != null && tag.getUsageCount() != null && tag.getUsageCount() > 0) {
            tag.setUsageCount(tag.getUsageCount() - 1);
            fileTagMapper.updateById(tag);
        }

        logOperation(fileId, "REMOVE_TAG", userId, null, "移除标签: " + tagId);
    }

    /**
     * 获取文件标签
     */
    public List<FileTag> getFileTags(String fileId) {
        List<FileTagRelation> relations = fileTagRelationMapper.selectList(
            new LambdaQueryWrapper<FileTagRelation>()
                .eq(FileTagRelation::getFileId, fileId)
        );
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }
        List<Long> tagIds = relations.stream()
            .map(FileTagRelation::getTagId)
            .collect(Collectors.toList());
        return fileTagMapper.selectBatchIds(tagIds);
    }

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
     * 获取文件统计信息
     */
    public FileStatistics getFileStatistics() {
        List<FileMetadata> files = fileMetadataMapper.selectList(
            new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getIsDeleted, false)
        );
        long totalFiles = files.size();
        long totalSize = files.stream()
            .mapToLong(file -> file.getFileSize() == null ? 0L : file.getFileSize())
            .sum();

        Map<String, Long> storageSizeMap = files.stream()
            .collect(Collectors.groupingBy(
                file -> Optional.ofNullable(file.getStorageType()).orElse("LOCAL"),
                Collectors.summingLong(file -> Optional.ofNullable(file.getFileSize()).orElse(0L))
            ));

        FileStatistics statistics = new FileStatistics();
        statistics.setTotalFiles(totalFiles);
        statistics.setTotalSize(totalSize);

        FileStatistics.StorageUsage usage = new FileStatistics.StorageUsage();
        usage.setLocal(storageSizeMap.getOrDefault(StorageService.StorageType.LOCAL.name(), 0L));
        usage.setMinio(storageSizeMap.getOrDefault(StorageService.StorageType.MINIO.name(), 0L));
        usage.setOss(storageSizeMap.getOrDefault(StorageService.StorageType.ALIYUN_OSS.name(), 0L));
        usage.setS3(storageSizeMap.getOrDefault(StorageService.StorageType.AWS_S3.name(), 0L));
        statistics.setStorageUsage(usage);

        Map<String, List<FileMetadata>> typeMap = files.stream()
            .collect(Collectors.groupingBy(file ->
                Optional.ofNullable(file.getFileExtension())
                    .map(String::toLowerCase)
                    .filter(StringUtils::hasText)
                    .orElse("unknown")
            ));

        List<FileTypeDistribution> distributions = typeMap.entrySet().stream()
            .map(entry -> {
                FileTypeDistribution dist = new FileTypeDistribution();
                dist.setType(entry.getKey());
                dist.setCount(entry.getValue().size());
                long sizeSum = entry.getValue().stream()
                    .mapToLong(file -> Optional.ofNullable(file.getFileSize()).orElse(0L))
                    .sum();
                dist.setSize(sizeSum);
                return dist;
            })
            .sorted(Comparator.comparingLong(FileTypeDistribution::getSize).reversed())
            .collect(Collectors.toList());
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

        StorageUsageSummary summary = new StorageUsageSummary();
        summary.setUsed(usedSpace);
        summary.setTotal(totalSpace);
        summary.setPercentage(totalSpace == 0 ? 0D : Math.round(usedSpace * 10000D / totalSpace) / 100D);
        return summary;
    }

    /**
     * 获取文件预览URL
     */
    public String getFilePreviewUrl(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        return storageService.getUrl(metadata.getFilePath());
    }

    /**
     * 获取文件缩略图URL
     */
    public String getThumbnailUrl(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (StringUtils.hasText(metadata.getThumbnailPath())) {
            return storageService.getUrl(metadata.getThumbnailPath());
        }
        return storageService.getUrl(metadata.getFilePath());
    }

    /**
     * 检查权限
     */
    public boolean hasPermission(String fileId, Long userId, PermissionType type) {
        // 1. 获取文件元数据
        FileMetadata metadata = fileMetadataMapper.selectOne(
            new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getFileId, fileId)
        );

        if (metadata == null) {
            return false;
        }

        // 2. 所有者拥有所有权限
        if (metadata.getOwnerId().equals(userId)) {
            return true;
        }

        // 3. 公开文件允许读权限
        if (metadata.getIsPublic() && type == PermissionType.READ) {
            return true;
        }

        // 4. 检查显式权限
        FilePermission permission = filePermissionMapper.selectOne(
            new LambdaQueryWrapper<FilePermission>()
                .eq(FilePermission::getFileId, fileId)
                .eq(FilePermission::getUserId, userId)
                .eq(FilePermission::getPermissionType, type.name())
                .and(wrapper -> wrapper
                    .isNull(FilePermission::getExpireTime)
                    .or()
                    .gt(FilePermission::getExpireTime, LocalDateTime.now())
                )
        );

        return permission != null;
    }

    // ========== 私有辅助方法 ==========

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

    private FileVersion createVersion(String fileId, String filePath, Long fileSize,
                                       String md5, String description, Long userId, String userName) {
        // 获取当前最大版本号
        FileVersion latestVersion = fileVersionMapper.selectOne(
            new LambdaQueryWrapper<FileVersion>()
                .eq(FileVersion::getFileId, fileId)
                .orderByDesc(FileVersion::getVersionNumber)
                .last("LIMIT 1")
        );

        int versionNumber = latestVersion != null ? latestVersion.getVersionNumber() + 1 : 1;

        // 创建版本记录
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

        // 将其他版本设为非当前版本
        fileVersionMapper.update(
            null,
            new LambdaUpdateWrapper<FileVersion>()
                .eq(FileVersion::getFileId, fileId)
                .set(FileVersion::getIsCurrent, false)
        );

        fileVersionMapper.insert(version);
        return version;
    }

    private void logOperation(String fileId, String operationType, Long operatorId,
                               String operatorName, String detail) {
        FileOperationLog log = new FileOperationLog();
        log.setFileId(fileId);
        log.setOperationType(operationType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperationDetail(detail);
        log.setOperationTime(LocalDateTime.now());

        fileOperationLogMapper.insert(log);
    }

    private String generateStoragePath(String fileId, String extension) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%d/%02d/%02d/%s.%s",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
            fileId, extension);
    }

    private String generateVersionPath(String fileId, int version, String extension) {
        return String.format("versions/%s/v%d.%s", fileId, version, extension);
    }

    /**
     * 权限类型枚举
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
