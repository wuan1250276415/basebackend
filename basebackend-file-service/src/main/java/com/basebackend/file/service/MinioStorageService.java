package com.basebackend.file.service;

import com.basebackend.file.entity.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * MinIO存储服务接口
 *
 * @author BaseBackend
 */
public interface MinioStorageService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 上传结果
     */
    FileUploadResult uploadFile(MultipartFile file);

    /**
     * 上传文件到指定路径
     *
     * @param file 文件
     * @param path 路径
     * @return 上传结果
     */
    FileUploadResult uploadFile(MultipartFile file, String path);

    /**
     * 上传图片（自动生成缩略图）
     *
     * @param file 图片文件
     * @return 上传结果
     */
    FileUploadResult uploadImage(MultipartFile file);

    /**
     * 分片上传大文件
     *
     * @param file 文件
     * @return 上传结果
     */
    FileUploadResult uploadLargeFile(MultipartFile file);

    /**
     * 下载文件
     *
     * @param filePath 文件路径
     * @return 文件流
     */
    InputStream downloadFile(String filePath);

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否成功
     */
    boolean deleteFile(String filePath);

    /**
     * 批量删除文件
     *
     * @param filePaths 文件路径列表
     * @return 删除成功的数量
     */
    int deleteFiles(List<String> filePaths);

    /**
     * 获取文件URL
     *
     * @param filePath 文件路径
     * @param expirySeconds 有效期（秒）
     * @return 文件URL
     */
    String getFileUrl(String filePath, int expirySeconds);

    /**
     * 获取文件URL（默认7天有效期）
     *
     * @param filePath 文件路径
     * @return 文件URL
     */
    String getFileUrl(String filePath);

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    boolean fileExists(String filePath);

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     * @return 是否成功
     */
    boolean createBucket(String bucketName);

    /**
     * 检查存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    boolean bucketExists(String bucketName);
}
