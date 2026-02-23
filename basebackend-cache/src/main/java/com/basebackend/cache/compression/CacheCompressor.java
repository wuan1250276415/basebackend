package com.basebackend.cache.compression;

/**
 * 缓存压缩器接口
 * 对序列化后的字节数组进行压缩/解压缩，减少 Redis 存储和网络传输开销
 */
public interface CacheCompressor {

    /**
     * 压缩数据
     *
     * @param data 原始字节数组
     * @return 压缩后的字节数组
     */
    byte[] compress(byte[] data);

    /**
     * 解压缩数据
     *
     * @param data 压缩的字节数组
     * @return 解压缩后的字节数组
     */
    byte[] decompress(byte[] data);

    /**
     * 获取压缩算法名称
     *
     * @return 算法标识
     */
    String getAlgorithm();
}
