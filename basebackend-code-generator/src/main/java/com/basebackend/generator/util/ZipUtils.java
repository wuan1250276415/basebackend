package com.basebackend.generator.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * ZIP压缩工具类
 */
@Slf4j
public class ZipUtils {

    /**
     * 将文件打包成ZIP
     *
     * @param files 文件Map，key为文件路径，value为文件内容
     * @return ZIP字节数组
     */
    public static byte[] createZip(Map<String, String> files) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos)) {

            zos.setEncoding(StandardCharsets.UTF_8.name());

            for (Map.Entry<String, String> entry : files.entrySet()) {
                String filePath = entry.getKey();
                String content = entry.getValue();

                ZipArchiveEntry zipEntry = new ZipArchiveEntry(filePath);
                zipEntry.setSize(content.getBytes(StandardCharsets.UTF_8).length);
                zos.putArchiveEntry(zipEntry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeArchiveEntry();
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("创建ZIP文件失败", e);
            throw new RuntimeException("创建ZIP文件失败: " + e.getMessage(), e);
        }
    }
}
