package com.basebackend.generator.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 预览文件DTO
 */
@Data
@Builder
public class PreviewFile {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件内容
     */
    private String content;

    /**
     * 语言类型（用于语法高亮）
     */
    private String language;
}
