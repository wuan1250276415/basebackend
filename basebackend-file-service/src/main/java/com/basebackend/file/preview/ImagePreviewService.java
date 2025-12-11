package com.basebackend.file.preview;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 图片预览服务
 *
 * 支持图片缩略图生成和多级缓存
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImagePreviewService {

    private static final long MAX_INPUT_SIZE = 50 * 1024 * 1024; // 50MB最大输入限制
    private static final String CACHE_PREFIX = "img";

    /**
     * 生成图片预览（缩略图）
     *
     * @param imageData 图片数据
     * @param width 目标宽度
     * @param height 目标高度
     * @param quality 图片质量（0.0-1.0）
     * @return 预览图片数据
     */
    @Cacheable(value = "imagePreview", key = "#root.methodName + ':' + T(java.util.Arrays).hashCode(#imageData) + ':' + #width + ':' + #height + ':' + #quality")
    public byte[] generatePreview(byte[] imageData, int width, int height, float quality) throws IOException {
        log.debug("生成图片预览: {}x{}, 质量: {}, 数据大小: {} bytes",
                width, height, quality, imageData.length);

        // 验证输入
        validateImageInput(imageData);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 使用Thumbnailator生成缩略图
            Thumbnails.of(inputStream)
                    .size(width, height)
                    .outputQuality(Math.max(0.1f, Math.min(1.0f, quality))) // 限制质量范围
                    .outputFormat("JPEG")
                    .toOutputStream(outputStream);

            byte[] previewData = outputStream.toByteArray();
            log.debug("图片预览生成成功，原始大小: {}, 预览大小: {}",
                    imageData.length, previewData.length);

            return previewData;

        } catch (Exception e) {
            log.error("图片预览生成失败: {}", e.getMessage(), e);
            throw new IOException("图片预览生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成Base64编码的图片预览
     *
     * @param imageData 图片数据
     * @param width 目标宽度
     * @param height 目标高度
     * @param quality 图片质量
     * @return Base64编码的图片预览
     */
    public String generatePreviewAsBase64(byte[] imageData, int width, int height, float quality) throws IOException {
        byte[] previewData = generatePreview(imageData, width, height, quality);
        return Base64.getEncoder().encodeToString(previewData);
    }

    /**
     * 从文件生成预览
     *
     * @param inputStream 文件输入流
     * @param width 目标宽度
     * @param height 目标高度
     * @param quality 图片质量
     * @return 预览图片数据
     */
    public byte[] generatePreviewFromFile(InputStream inputStream, int width, int height, float quality) throws IOException {
        // 验证输入流
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为null");
        }

        try {
            // 先读取图片数据（带大小限制）
            byte[] imageData = readInputStreamWithLimit(inputStream, MAX_INPUT_SIZE);
            return generatePreview(imageData, width, height, quality);
        } finally {
            // 确保关闭输入流
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("关闭输入流失败", e);
            }
        }
    }

    /**
     * 压缩图片
     *
     * @param imageData 图片数据
     * @param quality 质量（0.0-1.0）
     * @return 压缩后的图片数据
     */
    public byte[] compressImage(byte[] imageData, float quality) throws IOException {
        log.debug("压缩图片，质量: {}, 数据大小: {} bytes", quality, imageData.length);

        // 验证输入
        validateImageInput(imageData);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 压缩但不改变尺寸
            Thumbnails.of(inputStream)
                    .outputQuality(Math.max(0.1f, Math.min(1.0f, quality)))
                    .outputFormat("JPEG")
                    .toOutputStream(outputStream);

            byte[] compressedData = outputStream.toByteArray();
            log.debug("图片压缩完成，原始大小: {}, 压缩后大小: {}",
                    imageData.length, compressedData.length);

            return compressedData;

        } catch (Exception e) {
            log.error("图片压缩失败: {}", e.getMessage(), e);
            throw new IOException("图片压缩失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调整图片大小（保持宽高比）
     *
     * @param imageData 图片数据
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @param quality 图片质量
     * @return 调整后的图片数据
     */
    public byte[] resizeImage(byte[] imageData, int maxWidth, int maxHeight, float quality) throws IOException {
        log.debug("调整图片大小，最大尺寸: {}x{}, 数据大小: {} bytes",
                maxWidth, maxHeight, imageData.length);

        // 验证输入
        validateImageInput(imageData);

        // 验证尺寸参数
        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("尺寸参数必须大于0");
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 调整大小（自动保持宽高比）
            Thumbnails.of(inputStream)
                    .size(maxWidth, maxHeight)
                    .outputQuality(Math.max(0.1f, Math.min(1.0f, quality)))
                    .outputFormat("JPEG")
                    .toOutputStream(outputStream);

            byte[] resizedData = outputStream.toByteArray();
            log.debug("图片调整大小完成，原始大小: {}, 调整后大小: {}",
                    imageData.length, resizedData.length);

            return resizedData;

        } catch (Exception e) {
            log.error("图片调整大小失败: {}", e.getMessage(), e);
            throw new IOException("图片调整大小失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调整图片大小（指定宽高，可能变形）
     *
     * @param imageData 图片数据
     * @param width 目标宽度
     * @param height 目标高度
     * @param quality 图片质量
     * @return 调整后的图片数据
     */
    public byte[] resizeImageExact(byte[] imageData, int width, int height, float quality) throws IOException {
        log.debug("调整图片大小为精确尺寸: {}x{}, 数据大小: {} bytes",
                width, height, imageData.length);

        // 验证输入
        validateImageInput(imageData);

        // 验证尺寸参数
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("尺寸参数必须大于0");
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 调整为精确尺寸（可能变形）
            Thumbnails.of(inputStream)
                    .size(width, height)
                    .outputQuality(Math.max(0.1f, Math.min(1.0f, quality)))
                    .outputFormat("JPEG")
                    .toOutputStream(outputStream);

            byte[] resizedData = outputStream.toByteArray();
            log.debug("图片调整大小完成，原始大小: {}, 调整后大小: {}",
                    imageData.length, resizedData.length);

            return resizedData;

        } catch (Exception e) {
            log.error("图片调整大小失败: {}", e.getMessage(), e);
            throw new IOException("图片调整大小失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取图片信息
     *
     * @param imageData 图片数据
     * @return 图片信息
     */
    public ImageInfo getImageInfo(byte[] imageData) throws IOException {
        try {
            validateImageInput(imageData);

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    throw new IllegalArgumentException("无法读取图片数据");
                }

                return new ImageInfo(image.getWidth(), image.getHeight(), imageData.length);
            }

        } catch (Exception e) {
            log.error("获取图片信息失败: {}", e.getMessage(), e);
            throw new IOException("获取图片信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算图片内容的MD5哈希值
     *
     * @param imageData 图片数据
     * @return MD5哈希值
     */
    private String calculateImageHash(byte[] imageData) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(imageData);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.warn("计算图片哈希失败", e);
            return String.valueOf(imageData.length); // 降级方案
        }
    }

    /**
     * 验证图片输入
     *
     * @param imageData 图片数据
     */
    private void validateImageInput(byte[] imageData) {
        if (imageData == null) {
            throw new IllegalArgumentException("图片数据不能为null");
        }
        if (imageData.length == 0) {
            throw new IllegalArgumentException("图片数据不能为空");
        }
        if (imageData.length > MAX_INPUT_SIZE) {
            throw new IllegalArgumentException(
                    "图片数据过大: " + (imageData.length / 1024 / 1024) + " MB，最大允许 " +
                    (MAX_INPUT_SIZE / 1024 / 1024) + " MB");
        }
    }

    /**
     * 读取输入流数据（带大小限制）
     *
     * @param inputStream 输入流
     * @param maxSize 最大大小
     * @return 字节数据
     */
    private byte[] readInputStreamWithLimit(InputStream inputStream, long maxSize) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为null");
        }

        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > maxSize) {
                    throw new IllegalArgumentException(
                            "数据大小超过限制: " + (totalBytes / 1024 / 1024) + " MB，最大允许 " +
                            (maxSize / 1024 / 1024) + " MB");
                }
                buffer.write(data, 0, bytesRead);
            }

            return buffer.toByteArray();
        }
    }

    /**
     * 图片信息
     */
    public static class ImageInfo {
        private final int width;
        private final int height;
        private final long size;

        public ImageInfo(int width, int height, long size) {
            this.width = width;
            this.height = height;
            this.size = size;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public long getSize() { return size; }

        @Override
        public String toString() {
            return String.format("ImageInfo{width=%d, height=%d, size=%d bytes}", width, height, size);
        }
    }
}
