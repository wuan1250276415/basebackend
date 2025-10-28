//package com.basebackend.file.service.impl;
//
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.util.IdUtil;
//import com.basebackend.file.config.MinioProperties;
//import com.basebackend.file.entity.FileUploadResult;
//import com.basebackend.file.service.MinioStorageService;
//import io.minio.*;
//import io.minio.http.Method;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.coobird.thumbnailator.Thumbnails;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import jakarta.annotation.PostConstruct;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
///**
// * MinIO存储服务实现
// *
// * @author BaseBackend
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class MinioStorageServiceImpl implements MinioStorageService {
//
//    private final MinioClient minioClient;
//    private final MinioProperties minioProperties;
//
//    @PostConstruct
//    public void init() {
//        try {
//            // 确保默认存储桶存在
//            if (!bucketExists(minioProperties.getBucketName())) {
//                createBucket(minioProperties.getBucketName());
//                log.info("创建默认存储桶: {}", minioProperties.getBucketName());
//            }
//        } catch (Exception e) {
//            log.error("初始化MinIO存储桶失败", e);
//        }
//    }
//
//    @Override
//    public FileUploadResult uploadFile(MultipartFile file) {
//        return uploadFile(file, minioProperties.getPathPrefix());
//    }
//
//    @Override
//    public FileUploadResult uploadFile(MultipartFile file, String path) {
//        try {
//            String originalFilename = file.getOriginalFilename();
//            String extension = FileUtil.extName(originalFilename);
//            String fileId = IdUtil.fastSimpleUUID();
//            String storedFilename = fileId + "." + extension;
//
//            // 构建文件路径: path/yyyy-MM-dd/filename
//            String datePath = DateUtil.format(DateUtil.date(), "yyyy-MM-dd");
//            String filePath = path + "/" + datePath + "/" + storedFilename;
//
//            // 上传文件
//            minioClient.putObject(
//                    PutObjectArgs.builder()
//                            .bucket(minioProperties.getBucketName())
//                            .object(filePath)
//                            .stream(file.getInputStream(), file.getSize(), -1)
//                            .contentType(file.getContentType())
//                            .build()
//            );
//
//            log.info("文件上传成功: {}", filePath);
//
//            // 构建返回结果
//            return FileUploadResult.builder()
//                    .fileId(fileId)
//                    .originalFilename(originalFilename)
//                    .storedFilename(storedFilename)
//                    .filePath(filePath)
//                    .fileUrl(getFileUrl(filePath))
//                    .fileSize(file.getSize())
//                    .contentType(file.getContentType())
//                    .bucketName(minioProperties.getBucketName())
//                    .uploadTime(LocalDateTime.now())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("文件上传失败", e);
//            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public FileUploadResult uploadImage(MultipartFile file) {
//        try {
//            // 先上传原图
//            FileUploadResult result = uploadFile(file, minioProperties.getImagePath());
//
//            // 判断是否为图片
//            String contentType = file.getContentType();
//            if (contentType != null && contentType.startsWith("image/")) {
//                // 生成缩略图
//                String thumbnailPath = generateThumbnail(file, result.getFileId());
//                result.setThumbnailUrl(getFileUrl(thumbnailPath));
//                log.info("缩略图生成成功: {}", thumbnailPath);
//            }
//
//            return result;
//
//        } catch (Exception e) {
//            log.error("图片上传失败", e);
//            throw new RuntimeException("图片上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public FileUploadResult uploadLargeFile(MultipartFile file) {
//        try {
//            String originalFilename = file.getOriginalFilename();
//            String extension = FileUtil.extName(originalFilename);
//            String fileId = IdUtil.fastSimpleUUID();
//            String storedFilename = fileId + "." + extension;
//
//            // 构建文件路径
//            String datePath = DateUtil.format(DateUtil.date(), "yyyy-MM-dd");
//            String filePath = minioProperties.getLargePath() + "/" + datePath + "/" + storedFilename;
//
//            // 使用分片上传
//            long partSize = minioProperties.getPartSize() * 1024 * 1024; // 转换为字节
//
//            minioClient.putObject(
//                    PutObjectArgs.builder()
//                            .bucket(minioProperties.getBucketName())
//                            .object(filePath)
//                            .stream(file.getInputStream(), file.getSize(), partSize)
//                            .contentType(file.getContentType())
//                            .build()
//            );
//
//            log.info("大文件上传成功: {} ({}MB)", filePath, file.getSize() / 1024 / 1024);
//
//            return FileUploadResult.builder()
//                    .fileId(fileId)
//                    .originalFilename(originalFilename)
//                    .storedFilename(storedFilename)
//                    .filePath(filePath)
//                    .fileUrl(getFileUrl(filePath))
//                    .fileSize(file.getSize())
//                    .contentType(file.getContentType())
//                    .bucketName(minioProperties.getBucketName())
//                    .uploadTime(LocalDateTime.now())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("大文件上传失败", e);
//            throw new RuntimeException("大文件上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public InputStream downloadFile(String filePath) {
//        try {
//            return minioClient.getObject(
//                    GetObjectArgs.builder()
//                            .bucket(minioProperties.getBucketName())
//                            .object(filePath)
//                            .build()
//            );
//        } catch (Exception e) {
//            log.error("文件下载失败: {}", filePath, e);
//            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public boolean deleteFile(String filePath) {
//        try {
//            minioClient.removeObject(
//                    RemoveObjectArgs.builder()
//                            .bucket(minioProperties.getBucketName())
//                            .object(filePath)
//                            .build()
//            );
//            log.info("文件删除成功: {}", filePath);
//            return true;
//        } catch (Exception e) {
//            log.error("文件删除失败: {}", filePath, e);
//            return false;
//        }
//    }
//
//    @Override
//    public int deleteFiles(List<String> filePaths) {
//        int count = 0;
//        for (String filePath : filePaths) {
//            if (deleteFile(filePath)) {
//                count++;
//            }
//        }
//        log.info("批量删除文件完成: {}/{}", count, filePaths.size());
//        return count;
//    }
//
//    @Override
//    public String getFileUrl(String filePath, int expirySeconds) {
//        try {
//            return minioClient.getPresignedObjectUrl(
//                    GetPresignedObjectUrlArgs.builder()
//                            .bucket(minioProperties.getBucketName())
//                            .object(filePath)
//                            .method(Method.GET)
//                            .expiry(expirySeconds, TimeUnit.SECONDS)
//                            .build()
//            );
//        } catch (Exception e) {
//            log.error("获取文件URL失败: {}", filePath, e);
//            throw new RuntimeException("获取文件URL失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public String getFileUrl(String filePath) {
//        // 默认7天有效期
//        return getFileUrl(filePath, 7 * 24 * 60 * 60);
//    }
//
//    @Override
//    public boolean fileExists(String filePath) {
//        try {
//            minioClient.statObject(
//                    StatObjectArgs.builder()
//                            .bucket(minioProperties.getBucketName())
//                            .object(filePath)
//                            .build()
//            );
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    @Override
//    public boolean createBucket(String bucketName) {
//        try {
//            minioClient.makeBucket(
//                    MakeBucketArgs.builder()
//                            .bucket(bucketName)
//                            .build()
//            );
//            log.info("存储桶创建成功: {}", bucketName);
//            return true;
//        } catch (Exception e) {
//            log.error("存储桶创建失败: {}", bucketName, e);
//            return false;
//        }
//    }
//
//    @Override
//    public boolean bucketExists(String bucketName) {
//        try {
//            return minioClient.bucketExists(
//                    BucketExistsArgs.builder()
//                            .bucket(bucketName)
//                            .build()
//            );
//        } catch (Exception e) {
//            log.error("检查存储桶失败: {}", bucketName, e);
//            return false;
//        }
//    }
//
//    /**
//     * 生成缩略图
//     */
//    private String generateThumbnail(MultipartFile file, String fileId) throws Exception {
//        // 生成缩略图
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        Thumbnails.of(file.getInputStream())
//                .size(minioProperties.getThumbnailWidth(), minioProperties.getThumbnailHeight())
//                .outputQuality(minioProperties.getImageQuality())
//                .toOutputStream(outputStream);
//
//        byte[] thumbnailBytes = outputStream.toByteArray();
//
//        // 构建缩略图路径
//        String extension = FileUtil.extName(file.getOriginalFilename());
//        String datePath = DateUtil.format(DateUtil.date(), "yyyy-MM-dd");
//        String thumbnailPath = minioProperties.getThumbnailPath() + "/" + datePath + "/" + fileId + "_thumb." + extension;
//
//        // 上传缩略图
//        minioClient.putObject(
//                PutObjectArgs.builder()
//                        .bucket(minioProperties.getBucketName())
//                        .object(thumbnailPath)
//                        .stream(new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length, -1)
//                        .contentType(file.getContentType())
//                        .build()
//        );
//
//        return thumbnailPath;
//    }
//}
