package com.basebackend.cache.compression;

import com.basebackend.cache.exception.CacheException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 基于 JDK GZIP 的缓存压缩器
 */
public class GzipCacheCompressor implements CacheCompressor {

    private static final int BUFFER_SIZE = 1024;

    @Override
    public byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
             GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(data);
            gos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new CacheException("GZIP compression failed", e);
        }
    }

    @Override
    public byte[] decompress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new CacheException("GZIP decompression failed", e);
        }
    }

    @Override
    public String getAlgorithm() {
        return "GZIP";
    }
}
