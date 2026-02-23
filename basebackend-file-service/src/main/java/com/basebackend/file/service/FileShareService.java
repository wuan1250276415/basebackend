package com.basebackend.file.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.PageResult;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.entity.FileOperationLog;
import com.basebackend.file.entity.FileShare;
import com.basebackend.file.mapper.FileMetadataMapper;
import com.basebackend.file.mapper.FileOperationLogMapper;
import com.basebackend.file.mapper.FileShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * 文件分享服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileShareService {

    private final FileShareMapper fileShareMapper;
    private final FileMetadataMapper fileMetadataMapper;
    private final FileOperationLogMapper fileOperationLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final FilePermissionService filePermissionService;

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
            String userName) {
        if (!filePermissionService.hasPermission(fileId, userId,
                FilePermissionService.PermissionType.SHARE)) {
            throw new BusinessException("无权限分享该文件");
        }
        FileMetadata metadata = getFileMetadata(fileId);

        String shareCode = IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase(Locale.ROOT);
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setShareCode(shareCode);
        // 密码使用 BCrypt 哈希存储，禁止明文落库
        if (StringUtils.hasText(sharePassword)) {
            share.setSharePassword(passwordEncoder.encode(sharePassword));
        }
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
        if (share.getExpireTime() != null
                && share.getExpireTime().isBefore(LocalDateTime.now())) {
            share.setStatus("EXPIRED");
            fileShareMapper.updateById(share);
            throw new BusinessException("分享已过期");
        }
        if (StringUtils.hasText(share.getSharePassword())
                && !StringUtils.hasText(password)) {
            throw new BusinessException("需要分享密码");
        }
        if (StringUtils.hasText(share.getSharePassword())
                && !passwordEncoder.matches(password, share.getSharePassword())) {
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
        return PageResult.of(result.getRecords(), result.getTotal(),
            result.getCurrent(), result.getSize());
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
