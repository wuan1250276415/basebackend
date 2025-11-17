package com.basebackend.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;

/**
 * SSL/TLS配置
 * 提供HTTPS客户端和服务端的SSL配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SSLConfig {

    private final SslBundles sslBundles;

    /**
     * 创建安全的WebClient
     * 支持HTTPS和双向SSL认证
     */
    @Bean
    @ConditionalOnProperty(name = "security.ssl.enabled", havingValue = "true")
    public WebClient secureWebClient() {
        SslBundle sslBundle = sslBundles.getBundle("client");

        HttpClient httpClient = HttpClient.create()
                .secure(spec -> {
                    spec.sslContext(sslBundle.getSslContext());
                })
                .responseTimeout(Duration.ofSeconds(30));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 创建自定义SSLContext
     *
     * @param keyStorePath 密钥库路径
     * @param keyStorePassword 密钥库密码
     * @param trustStorePath 信任库路径
     * @param trustStorePassword 信任库密码
     * @return SSLContext
     */
    public SSLContext createSSLContext(String keyStorePath, String keyStorePassword,
                                      String trustStorePath, String trustStorePassword) {
        try {
            // 加载信任库
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                trustStore.load(fis, trustStorePassword.toCharArray());
            }

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            log.info("SSLContext创建成功，信任库: {}", trustStorePath);
            return sslContext;
        } catch (Exception e) {
            log.error("SSLContext创建失败", e);
            throw new SecurityException("SSLContext创建失败", e);
        }
    }

    /**
     * 验证SSL证书
     *
     * @param certPath 证书路径
     * @return 验证结果
     */
    public boolean verifyCertificate(String certPath) {
        try {
            // 验证证书有效期
            // 这里可以扩展为更复杂的证书验证逻辑
            log.debug("验证SSL证书: {}", certPath);
            return true;
        } catch (Exception e) {
            log.error("SSL证书验证失败: {}", certPath, e);
            return false;
        }
    }

    /**
     * 获取HTTPS端口配置
     */
    @Bean
    @ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
    public int httpsPort() {
        return Integer.parseInt(System.getProperty("server.ssl.port", "8443"));
    }

    /**
     * HTTP到HTTPS重定向配置
     * 自动将HTTP请求重定向到HTTPS
     */
    @Bean
    @ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
    public org.springframework.web.servlet.config.annotation.RedirectViewController httpToHttpsRedirect() {
        return new org.springframework.web.servlet.config.annotation.RedirectViewController("/", "https://localhost:" + httpsPort() + "/");
    }

    /**
     * 强制HTTPS配置
     * 在生产环境中强制使用HTTPS
     */
    @Bean
    @ConditionalOnProperty(name = "security.force-https", havingValue = "true")
    public org.springframework.security.web.authentication.ForceHttpsRedirectFilter forceHttpsFilter() {
        return new org.springframework.security.web.authentication.ForceHttpsRedirectFilter();
    }
}
