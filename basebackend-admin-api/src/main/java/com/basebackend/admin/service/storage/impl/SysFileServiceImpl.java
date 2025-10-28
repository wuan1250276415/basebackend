//package com.basebackend.admin.service.storage.impl;
//
//import cn.hutool.core.util.IdUtil;
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.basebackend.admin.entity.storage.SysFileInfo;
//import com.basebackend.admin.mapper.storage.SysFileInfoMapper;
//import com.basebackend.admin.service.storage.SysFileService;
//import com.basebackend.file.entity.FileUploadResult;
//import com.basebackend.file.service.MinioStorageService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.BeanUtils;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.InputStream;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * 文件管理服务实现
// *
// * @author BaseBackend
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class SysFileServiceImpl extends ServiceImpl<SysFileInfoMapper, SysFileInfo> implements SysFileService {
//
//    private final MinioStorageService minioStorageService;
//
//    @Override
//    public Long uploadFile(MultipartFile file) {
//        log.info("上传文件: {}", file.getOriginalFilename());
//
//        // 调用MinIO服务上传
//        FileUploadResult result = minioStorageService.uploadFile(file);
//
//        // 保存文件信息到数据库
//        SysFileInfo fileInfo = convertToSysFileInfo(result, "file");
//        save(fileInfo);
//
//        return fileInfo.getId();
//    }
//
//    @Override
//    public Long uploadImage(MultipartFile image) {
//        log.info("上传图片: {}", image.getOriginalFilename());
//
//        // 调用MinIO服务上传图片
//        FileUploadResult result = minioStorageService.uploadImage(image);
//
//        // 保存文件信息到数据库
//        SysFileInfo fileInfo = convertToSysFileInfo(result, "image");
//        fileInfo.setThumbnailUrl(result.getThumbnailUrl());
//        save(fileInfo);
//
//        return fileInfo.getId();
//    }
//
//    @Override
//    public Long uploadLargeFile(MultipartFile largeFile) {
//        log.info("上传大文件: {}", largeFile.getOriginalFilename());
//
//        // 调用MinIO服务上传大文件
//        FileUploadResult result = minioStorageService.uploadLargeFile(largeFile);
//
//        // 保存文件信息到数据库
//        SysFileInfo fileInfo = convertToSysFileInfo(result, "large");
//        save(fileInfo);
//
//        return fileInfo.getId();
//    }
//
//    @Override
//    public InputStream downloadFile(Long fileId) {
//        log.info("下载文件，ID: {}", fileId);
//
//        SysFileInfo fileInfo = getById(fileId);
//        if (fileInfo == null || fileInfo.getDeleted() == 1) {
//            log.error("文件不存在或已删除");
//            return null;
//        }
//
//        return minioStorageService.downloadFile(fileInfo.getFilePath());
//    }
//
//    @Override
//    public boolean deleteFile(Long fileId) {
//        log.info("删除文件，ID: {}", fileId);
//
//        SysFileInfo fileInfo = getById(fileId);
//        if (fileInfo == null) {
//            return false;
//        }
//
//        // 调用MinIO服务删除文件
//        boolean success = minioStorageService.deleteFile(fileInfo.getFilePath());
//
//        if (success) {
//            // 软删除数据库记录
//            fileInfo.setDeleted(1);
//            updateById(fileInfo);
//
//            // 如果有缩略图，也删除
//            if (fileInfo.getThumbnailUrl() != null) {
//                // 从URL提取路径
//                // TODO: 可以改进提取逻辑
//            }
//        }
//
//        return success;
//    }
//
//    @Override
//    public int batchDeleteFiles(List<Long> fileIds) {
//        log.info("批量删除文件，数量: {}", fileIds.size());
//
//        int count = 0;
//        for (Long fileId : fileIds) {
//            if (deleteFile(fileId)) {
//                count++;
//            }
//        }
//
//        return count;
//    }
//
//    @Override
//    public List<SysFileInfo> listFiles(String fileCategory, Long uploadUserId) {
//        LambdaQueryWrapper<SysFileInfo> wrapper = new LambdaQueryWrapper<>();
//
//        wrapper.eq(SysFileInfo::getDeleted, 0);
//
//        if (fileCategory != null && !fileCategory.isEmpty()) {
//            wrapper.eq(SysFileInfo::getFileCategory, fileCategory);
//        }
//
//        if (uploadUserId != null) {
//            wrapper.eq(SysFileInfo::getUploadUserId, uploadUserId);
//        }
//
//        wrapper.orderByDesc(SysFileInfo::getCreateTime);
//
//        return list(wrapper);
//    }
//
//    @Override
//    public String getFileUrl(Long fileId, int expirySeconds) {
//        SysFileInfo fileInfo = getById(fileId);
//        if (fileInfo == null || fileInfo.getDeleted() == 1) {
//            return null;
//        }
//
//        return minioStorageService.getFileUrl(fileInfo.getFilePath(), expirySeconds);
//    }
//
//    /**
//     * 转换文件上传结果
//     */
//    private SysFileInfo convertToSysFileInfo(FileUploadResult result, String fileCategory) {
//        SysFileInfo fileInfo = new SysFileInfo();
//        BeanUtils.copyProperties(result, fileInfo);
//
//        fileInfo.setFileCode(result.getFileId());
//        fileInfo.setFileCategory(fileCategory);
//        fileInfo.setDeleted(0);
//        fileInfo.setCreateTime(LocalDateTime.now());
//        // TODO: 从Security Context获取当前用户信息
//        // fileInfo.setUploadUserId(currentUserId);
//        // fileInfo.setUploadUsername(currentUsername);
//
//        return fileInfo;
//    }
//}
