# 文件管理服务 - 核心代码示例

本文档提供文件管理服务的核心代码实现示例，包括完整的服务层、控制层代码。

---

## 📦 核心服务实现

### 1. 文件管理服务（FileManagementService）

```java
package com.basebackend.file.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.entity.*;
import com.basebackend.file.mapper.*;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class FileManagementService {

    private final StorageService storageService;
    private final FileMetadataMapper fileMetadataMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FilePermissionMapper filePermissionMapper;
    private final FileRecycleBinMapper fileRecycleBinMapper;
    private final FileOperationLogMapper fileOperationLogMapper;

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
            inputStream.reset();

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
                inputStream,
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
        recycleBin.setOriginalMetadata(toJson(metadata));

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
        FileMetadata metadata = getFileMetadata(fileId);
        metadata.setIsDeleted(false);
        metadata.setDeletedAt(null);
        metadata.setDeletedBy(null);
        fileMetadataMapper.updateById(metadata);

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
            inputStream.reset();

            // 3. 检查是否有变化
            if (md5.equals(metadata.getMd5())) {
                throw new BusinessException("文件内容未发生变化");
            }

            // 4. 上传新版本文件
            String versionPath = generateVersionPath(fileId, metadata.getVersion() + 1, metadata.getFileExtension());
            storageService.upload(inputStream, versionPath, file.getContentType(), file.getSize());

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
            new LambdaQueryWrapper<FileVersion>()
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

    private String toJson(Object obj) {
        // 使用Jackson或Gson序列化
        return "{}"; // 简化示例
    }

    // 权限类型枚举
    public enum PermissionType {
        READ, WRITE, DELETE, SHARE
    }

    // 文件下载信息
    @Data
    public static class FileDownloadInfo {
        private InputStream inputStream;
        private String fileName;
        private String contentType;
        private Long fileSize;
    }
}
```

---

## 🎮 REST API 控制器

### FileController

```java
package com.basebackend.file.controller;

import com.basebackend.common.result.Result;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.service.FileManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileManagementService fileManagementService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<FileMetadata> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "folderId", required = false) Long folderId,
        HttpServletRequest request
    ) {
        // 从请求中获取用户信息（实际应从SecurityContext获取）
        Long userId = 1L; // 示例
        String userName = "admin";

        FileMetadata metadata = fileManagementService.uploadFile(file, folderId, userId, userName);
        return Result.success(metadata);
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> download(
        @PathVariable String fileId,
        HttpServletRequest request
    ) {
        Long userId = 1L; // 从SecurityContext获取

        FileManagementService.FileDownloadInfo downloadInfo =
            fileManagementService.downloadFile(fileId, userId);

        String encodedFileName = URLEncoder.encode(
            downloadInfo.getFileName(),
            StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                   "attachment; filename*=UTF-8''" + encodedFileName)
            .contentType(MediaType.parseMediaType(downloadInfo.getContentType()))
            .contentLength(downloadInfo.getFileSize())
            .body(new InputStreamResource(downloadInfo.getInputStream()));
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public Result<Void> delete(
        @PathVariable String fileId,
        HttpServletRequest request
    ) {
        Long userId = 1L;
        String userName = "admin";

        fileManagementService.deleteFile(fileId, userId, userName);
        return Result.success();
    }

    /**
     * 恢复文件
     */
    @PostMapping("/{fileId}/restore")
    public Result<Void> restore(
        @PathVariable String fileId,
        HttpServletRequest request
    ) {
        Long userId = 1L;
        fileManagementService.restoreFile(fileId, userId);
        return Result.success();
    }

    /**
     * 创建版本
     */
    @PostMapping("/{fileId}/version")
    public Result<FileVersion> createVersion(
        @PathVariable String fileId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "description", required = false) String description,
        HttpServletRequest request
    ) {
        Long userId = 1L;
        String userName = "admin";

        FileVersion version = fileManagementService.createFileVersion(
            fileId, file, description, userId, userName
        );
        return Result.success(version);
    }

    /**
     * 版本回退
     */
    @PostMapping("/{fileId}/revert/{versionId}")
    public Result<Void> revertVersion(
        @PathVariable String fileId,
        @PathVariable Long versionId,
        HttpServletRequest request
    ) {
        Long userId = 1L;
        fileManagementService.revertToVersion(fileId, versionId, userId);
        return Result.success();
    }
}
```

---

## 📝 使用示例

### 前端上传文件

```typescript
import { message } from 'antd';
import axios from 'axios';

const uploadFile = async (file: File, folderId?: number) => {
  const formData = new FormData();
  formData.append('file', file);
  if (folderId) {
    formData.append('folderId', folderId.toString());
  }

  try {
    const response = await axios.post('/api/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round(
          (progressEvent.loaded * 100) / (progressEvent.total || 1)
        );
        console.log(`Upload progress: ${percentCompleted}%`);
      },
    });

    if (response.data.success) {
      message.success('文件上传成功');
      return response.data.data;
    }
  } catch (error) {
    message.error('文件上传失败');
    console.error(error);
  }
};

// 使用
uploadFile(file, 123);
```

### 前端下载文件

```typescript
const downloadFile = async (fileId: string) => {
  try {
    const response = await axios.get(`/api/file/download/${fileId}`, {
      responseType: 'blob',
    });

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;

    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition'];
    const fileName = decodeURIComponent(
      contentDisposition.split("filename*=UTF-8''")[1]
    );

    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();

    message.success('下载成功');
  } catch (error) {
    message.error('下载失败');
    console.error(error);
  }
};

// 使用
downloadFile('abc123');
```

---

## ✅ 总结

已实现的核心功能：

1. **文件上传** - 支持MD5去重、多存储后端
2. **文件下载** - 支持权限检查、下载统计
3. **文件删除** - 软删除、回收站机制
4. **文件恢复** - 从回收站恢复
5. **版本管理** - 创建版本、版本回退
6. **权限控制** - 细粒度权限检查
7. **操作日志** - 完整的操作审计

下一步：
- 完善前端页面
- 添加文件预览功能
- 实现文件分享功能
- 优化性能和用户体验

---

**文档版本**：v1.0
**最后更新**：2025年1月
