package com.basebackend.backup.infrastructure.reliability;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 校验和结果对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Checksum {
    /**
     * MD5校验和
     */
    private String md5;

    /**
     * SHA256校验和
     */
    private String sha256;

    /**
     * 计算时间
     */
    private LocalDateTime calculatedAt;

    /**
     * 使用的算法列表
     */
    private String[] algorithms;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 校验是否通过
     */
    private boolean verified;

    /**
     * 验证指定的校验和
     *
     * @param expectedMd5 期望的MD5
     * @param expectedSha256 期望的SHA256
     * @return 验证结果
     */
    public boolean verify(String expectedMd5, String expectedSha256) {
        boolean md5Match = expectedMd5 == null || expectedMd5.equals(md5);
        boolean sha256Match = expectedSha256 == null || expectedSha256.equals(sha256);
        this.verified = md5Match && sha256Match;
        return this.verified;
    }
}
