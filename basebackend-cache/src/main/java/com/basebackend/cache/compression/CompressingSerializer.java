package com.basebackend.cache.compression;

import com.basebackend.cache.serializer.CacheSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * 压缩装饰器序列化器
 * 在底层序列化器的基础上增加压缩/解压缩能力
 *
 * 协议：
 * - 第一个字节为标志位：0x01 = 未压缩，0x02 = GZIP 压缩
 * - 后续字节为实际数据（压缩或未压缩）
 *
 * 只有当序列化后的数据大小超过阈值时才进行压缩
 */
@Slf4j
public class CompressingSerializer implements CacheSerializer {

    private static final byte FLAG_UNCOMPRESSED = 0x01;
    private static final byte FLAG_GZIP = 0x02;

    private final CacheSerializer delegate;
    private final CacheCompressor compressor;
    private final int thresholdBytes;

    public CompressingSerializer(CacheSerializer delegate, CacheCompressor compressor, int thresholdBytes) {
        this.delegate = delegate;
        this.compressor = compressor;
        this.thresholdBytes = thresholdBytes;
    }

    @Override
    public byte[] serialize(Object obj) {
        byte[] raw = delegate.serialize(obj);
        if (raw == null) {
            return null;
        }

        if (raw.length < thresholdBytes) {
            return prepend(FLAG_UNCOMPRESSED, raw);
        }

        byte[] compressed = compressor.compress(raw);
        // 只在压缩确实减小了体积时使用压缩结果
        if (compressed.length < raw.length) {
            log.debug("Compressed {} -> {} bytes (ratio {}%)",
                    raw.length, compressed.length,
                    Math.round((1.0 - (double) compressed.length / raw.length) * 1000) / 10.0);
            return prepend(FLAG_GZIP, compressed);
        }

        return prepend(FLAG_UNCOMPRESSED, raw);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> type) {
        if (data == null || data.length == 0) {
            return null;
        }

        byte flag = data[0];
        byte[] payload = stripFlag(data);

        byte[] raw;
        if (flag == FLAG_GZIP) {
            raw = compressor.decompress(payload);
        } else {
            raw = payload;
        }

        return delegate.deserialize(raw, type);
    }

    @Override
    public String getType() {
        return delegate.getType() + "+compression";
    }

    private static byte[] prepend(byte flag, byte[] data) {
        byte[] result = new byte[data.length + 1];
        result[0] = flag;
        System.arraycopy(data, 0, result, 1, data.length);
        return result;
    }

    private static byte[] stripFlag(byte[] data) {
        byte[] result = new byte[data.length - 1];
        System.arraycopy(data, 1, result, 0, result.length);
        return result;
    }
}
