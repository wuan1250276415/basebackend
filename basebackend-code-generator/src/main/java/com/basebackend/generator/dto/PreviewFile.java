package com.basebackend.generator.dto;

/**
 * 预览文件DTO
 *
 * @param fileName 文件名
 * @param filePath 文件路径
 * @param content  文件内容
 * @param language 语言类型（用于语法高亮）
 */
public record PreviewFile(
        String fileName,
        String filePath,
        String content,
        String language
) {
}
