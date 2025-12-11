package com.basebackend.storage.reliability;

import com.basebackend.storage.model.Checksum;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 校验服务接口
 * 
 * @author BaseBackend
 */
public interface ChecksumProvider {
    
    /**
     * 计算文件的校验和
     *
     * @param filePath 文件路径
     * @return 校验和结果
     */
    Checksum computeChecksum(Path filePath);
    
    /**
     * 计算输入流的校验和
     *
     * @param inputStream 输入流
     * @param fileSize    文件大小（可选）
     * @return 校验和结果
     */
    Checksum computeChecksum(InputStream inputStream, Long fileSize);
    
    /**
     * 验证文件校验和
     *
     * @param filePath       文件路径
     * @param expectedMd5    期望的MD5（可选）
     * @param expectedSha256 期望的SHA256（可选）
     * @return 验证是否通过
     */
    boolean verifyChecksum(Path filePath, String expectedMd5, String expectedSha256);
    
    /**
     * 计算 MD5
     *
     * @param filePath 文件路径
     * @return MD5 字符串
     */
    String computeMD5(Path filePath);
    
    /**
     * 计算 SHA256
     *
     * @param filePath 文件路径
     * @return SHA256 字符串
     */
    String computeSHA256(Path filePath);
    
    /**
     * 从输入流计算 MD5
     *
     * @param inputStream 输入流
     * @return MD5 字符串
     */
    String computeMD5(InputStream inputStream);
    
    /**
     * 从输入流计算 SHA256
     *
     * @param inputStream 输入流
     * @return SHA256 字符串
     */
    String computeSHA256(InputStream inputStream);
}
