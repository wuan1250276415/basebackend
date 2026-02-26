package com.basebackend.generator.dto;

import java.util.List;
import java.util.Map;

/**
 * 代码生成结果DTO
 *
 * @param status       生成状态：SUCCESS/FAILED/PARTIAL
 * @param files        生成的文件Map（文件路径 -> 文件内容）
 * @param zipData      ZIP文件字节数组（下载模式）
 * @param errorMessage 错误消息
 * @param fileCount    生成的文件数量
 * @param failedTables 失败的表列表
 */
public record GenerateResult(
        String status,
        Map<String, String> files,
        byte[] zipData,
        String errorMessage,
        Integer fileCount,
        List<String> failedTables
) {
}
