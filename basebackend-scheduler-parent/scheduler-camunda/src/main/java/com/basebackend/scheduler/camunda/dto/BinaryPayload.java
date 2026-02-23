package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二进制文件载荷 DTO
 * 用于 BPMN XML 和流程图的下载
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinaryPayload {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 内容类型 (MIME type)
     */
    private String mimeType;

    /**
     * 二进制内容
     */
    private byte[] data;
}
