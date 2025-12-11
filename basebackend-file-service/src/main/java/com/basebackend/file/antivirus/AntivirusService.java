package com.basebackend.file.antivirus;

import java.io.InputStream;

/**
 * 病毒扫描服务接口
 * <p>
 * 定义病毒扫描的统一接口，支持多种杀毒引擎实现。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface AntivirusService {

    /**
     * 扫描文件流
     *
     * @param inputStream 文件输入流
     * @param filename    文件名
     * @return 扫描结果
     */
    ScanResult scan(InputStream inputStream, String filename);

    /**
     * 扫描字节数组
     *
     * @param data     文件数据
     * @param filename 文件名
     * @return 扫描结果
     */
    ScanResult scan(byte[] data, String filename);

    /**
     * 判断服务是否可用
     *
     * @return true 如果服务可用
     */
    boolean isAvailable();

    /**
     * 获取引擎名称
     *
     * @return 引擎名称
     */
    String getEngineName();
}
