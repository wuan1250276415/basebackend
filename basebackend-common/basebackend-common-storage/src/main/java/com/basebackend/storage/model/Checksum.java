package com.basebackend.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 校验和结果
 * 
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Checksum {
    
    /**
     * MD5 校验和
     */
    private String md5;
    
    /**
     * SHA256 校验和
     */
    private String sha256;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 使用的算法列表
     */
    private String[] algorithms;
    
    /**
     * 计算时间
     */
    private LocalDateTime calculatedAt;
    
    /**
     * 验证校验和
     *
     * @param expectedMd5    期望的MD5（可选）
     * @param expectedSha256 期望的SHA256（可选）
     * @return 验证是否通过
     */
    public boolean verify(String expectedMd5, String expectedSha256) {
        boolean md5Match = (expectedMd5 == null) || expectedMd5.equalsIgnoreCase(this.md5);
        boolean sha256Match = (expectedSha256 == null) || expectedSha256.equalsIgnoreCase(this.sha256);
        return md5Match && sha256Match;
    }
    
    /**
     * 创建一个只有MD5的校验和
     */
    public static Checksum ofMd5(String md5) {
        return Checksum.builder()
                .md5(md5)
                .algorithms(new String[]{"MD5"})
                .calculatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建一个只有SHA256的校验和
     */
    public static Checksum ofSha256(String sha256) {
        return Checksum.builder()
                .sha256(sha256)
                .algorithms(new String[]{"SHA256"})
                .calculatedAt(LocalDateTime.now())
                .build();
    }
}
