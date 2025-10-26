package com.basebackend.generator.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 代码生成结果DTO
 */
@Data
@Builder
public class GenerateResult {

    /**
     * 生成状态：SUCCESS/FAILED/PARTIAL
     */
    private String status;

    /**
     * 生成的文件Map（文件路径 -> 文件内容）
     */
    private Map<String, String> files;

    /**
     * ZIP文件字节数组（下载模式）
     */
    private byte[] zipData;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 生成的文件数量
     */
    private Integer fileCount;

    /**
     * 失败的表列表
     */
    private List<String> failedTables;
}
