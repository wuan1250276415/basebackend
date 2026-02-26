package com.basebackend.scheduler.camunda.dto;

/**
 * 二进制文件载荷 DTO
 * 用于 BPMN XML 和流程图的下载
 */
public record BinaryPayload(
        String fileName,
        String mimeType,
        byte[] data
) {
}
