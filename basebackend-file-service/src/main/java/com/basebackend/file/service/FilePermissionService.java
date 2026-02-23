package com.basebackend.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.entity.FileOperationLog;
import com.basebackend.file.entity.FilePermission;
import com.basebackend.file.mapper.FileMetadataMapper;
import com.basebackend.file.mapper.FileOperationLogMapper;
import com.basebackend.file.mapper.FilePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件权限服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilePermissionService {

    private final FilePermissionMapper filePermissionMapper;
    private final FileMetadataMapper fileMetadataMapper;
    private final FileOperationLogMapper fileOperationLogMapper;

    /**
     * 权限类型枚举
     */
    public enum PermissionType {
        READ, WRITE, DELETE, SHARE
    }

    /**
     * 检查权限
     */
    public boolean hasPermission(String fileId, Long userId, PermissionType type) {
        FileMetadata metadata = fileMetadataMapper.selectOne(
            new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getFileId, fileId)
        );

        if (metadata == null) {
            return false;
        }

        // 所有者拥有所有权限
        if (metadata.getOwnerId().equals(userId)) {
            return true;
        }

        // 公开文件允许读权限
        if (metadata.getIsPublic() && type == PermissionType.READ) {
            return true;
        }

        // 检查显式权限
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

    /**
     * 获取文件权限列表
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
}
