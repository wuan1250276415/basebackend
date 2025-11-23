package com.basebackend.scheduler.camunda.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 二进制数据负载
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
public class BinaryPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据
     */
    private byte[] data;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * MIME 类型
     */
    private String mimeType;

    /**
     * 构造函数
     */
    public BinaryPayload() {
    }

    /**
     * 构造函数
     *
     * @param data     数据
     * @param fileName 文件名
     */
    public BinaryPayload(byte[] data, String fileName) {
        this.data = data;
        this.fileName = fileName;
    }

    /**
     * 构造函数
     *
     * @param data     数据
     * @param fileName 文件名
     * @param mimeType MIME 类型
     */
    public BinaryPayload(byte[] data, String fileName, String mimeType) {
        this.data = data;
        this.fileName = fileName;
        this.mimeType = mimeType;
    }
}
