package com.basebackend.security.mtls.ssl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * SSLContext工厂
 *
 * 创建和管理SSLContext实例
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SSLContextFactory {

    /**
     * 创建客户端SSLContext
     *
     * @param keyStore 客户端证书KeyStore
     * @param keyPassword 私钥密码
     * @param trustStore 信任证书KeyStore
     * @return SSLContext
     * @throws Exception SSLContext创建异常
     */
    public static SSLContext createClientSSLContext(KeyStore keyStore,
                                                   String keyPassword,
                                                   KeyStore trustStore) throws Exception {
        log.debug("创建客户端SSLContext");

        try {
            // 创建KeyManagerFactory
            javax.net.ssl.KeyManagerFactory keyManagerFactory =
                javax.net.ssl.KeyManagerFactory.getInstance(
                    javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassword.toCharArray());

            // 创建TrustManagerFactory
            TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom()
            );

            log.info("客户端SSLContext创建成功");
            return sslContext;

        } catch (Exception e) {
            log.error("客户端SSLContext创建失败", e);
            throw new RuntimeException("客户端SSLContext创建失败", e);
        }
    }

    /**
     * 创建服务端SSLContext
     *
     * @param keyStore 服务端证书KeyStore
     * @param keyPassword 私钥密码
     * @return SSLContext
     * @throws Exception SSLContext创建异常
     */
    public static SSLContext createServerSSLContext(KeyStore keyStore,
                                                   String keyPassword) throws Exception {
        log.debug("创建服务端SSLContext");

        try {
            // 创建KeyManagerFactory
            javax.net.ssl.KeyManagerFactory keyManagerFactory =
                javax.net.ssl.KeyManagerFactory.getInstance(
                    javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassword.toCharArray());

            // 创建SSLContext（服务端不需要TrustManager）
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                keyManagerFactory.getKeyManagers(),
                null, // 不验证客户端证书
                new SecureRandom()
            );

            log.info("服务端SSLContext创建成功");
            return sslContext;

        } catch (Exception e) {
            log.error("服务端SSLContext创建失败", e);
            throw new RuntimeException("服务端SSLContext创建失败", e);
        }
    }

    /**
     * 创建自定义SSLContext（用于测试或特殊需求）
     *
     * @param enabledProtocols 启用的TLS协议
     * @param enabledCipherSuites 启用的加密套件
     * @return SSLContext
     * @throws NoSuchAlgorithmException 算法不存在异常
     * @throws KeyManagementException 密钥管理异常
     */
    public static SSLContext createCustomSSLContext(String[] enabledProtocols,
                                                   String[] enabledCipherSuites) throws NoSuchAlgorithmException, KeyManagementException {
        log.debug("创建自定义SSLContext - Protocols: {}, CipherSuites: {}",
            enabledProtocols, enabledCipherSuites);

        SSLContext sslContext = SSLContext.getInstance("TLS");

        // 创建自定义SSLContext
        sslContext.init(null, null, new SecureRandom());

        // 设置系统属性以影响默认SSL连接
        if (enabledProtocols != null) {
            String protocols = String.join(",", java.util.Arrays.asList(enabledProtocols));
            System.setProperty("https.protocols", protocols);
            log.debug("已设置HTTPS协议: {}", protocols);
        }

        if (enabledCipherSuites != null) {
            String cipherSuites = String.join(",", java.util.Arrays.asList(enabledCipherSuites));
            System.setProperty("https.cipherSuites", cipherSuites);
            log.debug("已设置加密套件: {}", cipherSuites);
        }

        log.info("自定义SSLContext创建成功");
        return sslContext;
    }
}
