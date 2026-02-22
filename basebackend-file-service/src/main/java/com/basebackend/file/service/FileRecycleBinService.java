package com.basebackend.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.PageResult;
import com.basebackend.file.entity.*;
import com.basebackend.file.mapper.*;
import com.basebackend.file.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件回收站服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecycleBinService {

    private final FileRecycleBinMapper fileRecycleBinMapper;
    private final FileMetadataMapper fileMetadataMapper;
    private final StorageService storageService;
    private final FileVersionMapper fileVersionMapper;
    private final FilePermissionMapper filePermissionMapper;
    private final FileTagRelationMapper fileTagRelationMapper;
    private final FileOperationLogMapper fileOperationLogMapper;
    private final FilePermissionService filePermissionService;

    /**
     * 恢复文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(String fileId, Long userId) {
        FileRecycleBin recycleBin = fileRecycleBinMapper.selectOne(
            new LambdaQueryWrapper<FileRecycleBin>()
                .eq(FileRecycleBin::getFileId, fileId)
        );

        if (recycleBin == null) {
            throw new BusinessException("回收站中未找到该文件");
        }

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

        fileRecycleBinMapper.deleteById(recycleBin.getId());
        logOperation(fileId, "RECOVER", userId, null, "恢复文件");
        log.info("文件已恢复: fileId={}", fileId);
    }

    /**
     * 获取回收站列表
     */
    public PageResult<FileRecycleBin> getRecycleBinList(long current, long size) {
        Page<FileRecycleBin> page = new Page<>(current, size);
        LambdaQueryWrapper<FileRecycleBin> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FileRecycleBin::getDeletedAt);
        Page<FileRecycleBin> result = fileRecycleBinMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(),
            result.getCurrent(), result.getSize());
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
            if (!filePermissionService.hasPermission(fileId, userId,
                    FilePermissionService.PermissionType.DELETE)) {
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
        List<FileRecycleBin> recycleList = fileRecycleBinMapper.selectList(
            new LambdaQueryWrapper<>());
        if (CollectionUtils.isEmpty(recycleList)) {
            return;
        }
        for (FileRecycleBin record : recycleList) {
            permanentDeleteFile(record.getFileId(), userId);
        }
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
}
