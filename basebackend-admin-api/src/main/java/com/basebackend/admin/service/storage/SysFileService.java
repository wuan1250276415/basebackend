package com.basebackend.admin.service.storage;

import com.baomidou.mybatisplus.extension.service.IService;
import com.basebackend.admin.entity.storage.SysFileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件管理服务
 *
 * @author BaseBackend
 */
public interface SysFileService extends IService<SysFileInfo> {

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件信息ID
     */
    Long uploadFile(MultipartFile file);

    /**
     * 上传图片
     *
     * @param image 图片
     * @return 文件信息ID
     */
    Long uploadImage(MultipartFile image);

    /**
     * 上传大文件
     *
     * @param largeFile 大文件
     * @return 文件信息ID
     */
    Long uploadLargeFile(MultipartFile largeFile);

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件流
     */
    InputStream downloadFile(Long fileId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 是否成功
     */
    boolean deleteFile(Long fileId);

    /**
     * 批量删除文件
     *
     * @param fileIds 文件ID列表
     * @return 删除数量
     */
    int batchDeleteFiles(List<Long> fileIds);

    /**
     * 获取文件列表
     *
     * @param fileCategory 文件分类
     * @param uploadUserId 上传人ID
     * @return 文件列表
     */
    List<SysFileInfo> listFiles(String fileCategory, Long uploadUserId);

    /**
     * 获取文件访问URL
     *
     * @param fileId 文件ID
     * @param expirySeconds 有效期（秒）
     * @return 文件URL
     */
    String getFileUrl(Long fileId, int expirySeconds);
}
