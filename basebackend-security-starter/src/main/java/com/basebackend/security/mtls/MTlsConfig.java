package com.basebackend.security.mtls;

import com.basebackend.security.mtls.certificate.CertificateManager;
import com.basebackend.security.mtls.config.MTLSProperties;
import com.basebackend.security.mtls.ssl.SSLContextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * mTLS（双向TLS）配置
 *
 * 配置服务间通信的双向TLS认证，包括：
 * - SSLContext配置
 * - 证书管理
 * - 客户端信任管理器
 * - 服务端证书验证
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MTLSProperties.class)
@ConditionalOnProperty(name = "basebackend.security.mtls.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MTlsConfig {

    private final MTLSProperties mtlsProperties;

    /**
     * 创建SSLContext用于客户端mTLS认证
     *
     * @return SSLContext
     * @throws Exception SSLContext创建异常
     */
    @Bean("clientSSLContext")
    @ConditionalOnMissingBean(name = "clientSSLContext")
    public SSLContext clientSSLContext() throws Exception {
        log.info("初始化mTLS客户端SSLContext");

        try {
            // 加载客户端证书
            KeyStore clientKeyStore = loadKeyStore(mtlsProperties.getClient().getKeyStorePath(),
                mtlsProperties.getClient().getKeyStorePassword(),
                mtlsProperties.getClient().getKeyStoreType());

            // 加载信任的CA证书（用于验证服务端证书）
            KeyStore trustStore = loadTrustStore(mtlsProperties.getClient().getTrustStorePath(),
                mtlsProperties.getClient().getTrustStorePassword());

            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                CertificateManager.createKeyManager(clientKeyStore, mtlsProperties.getClient().getKeyPassword()),
                CertificateManager.createTrustManager(trustStore),
                new SecureRandom()
            );

            log.info("mTLS客户端SSLContext初始化成功");
            log.info("Client Cert: {}", mtlsProperties.getClient().getKeyStorePath());
            log.info("Trust Store: {}", mtlsProperties.getClient().getTrustStorePath());

            return sslContext;

        } catch (Exception e) {
            log.error("mTLS客户端SSLContext初始化失败", e);
            throw new RuntimeException("SSLContext初始化失败", e);
        }
    }

    /**
     * 创建服务端SSLContext
     *
     * @return SSLContext
     * @throws Exception SSLContext创建异常
     */
    @Bean("serverSSLContext")
    @ConditionalOnMissingBean(name = "serverSSLContext")
    public SSLContext serverSSLContext() throws Exception {
        if (!mtlsProperties.getServer().isEnabled()) {
            log.debug("服务端mTLS未启用");
            return null;
        }

        log.info("初始化mTLS服务端SSLContext");

        try {
            // 加载服务端证书
            KeyStore serverKeyStore = loadKeyStore(mtlsProperties.getServer().getKeyStorePath(),
                mtlsProperties.getServer().getKeyStorePassword(),
                mtlsProperties.getServer().getKeyStoreType());

            // 加载信任的CA证书（用于验证客户端证书）
            KeyStore clientTrustStore = loadTrustStore(mtlsProperties.getServer().getTrustStorePath(),
                mtlsProperties.getServer().getTrustStorePassword());

            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                CertificateManager.createKeyManager(serverKeyStore, mtlsProperties.getServer().getKeyPassword()),
                CertificateManager.createTrustManager(clientTrustStore), // 验证客户端证书
                new SecureRandom()
            );

            log.info("mTLS服务端SSLContext初始化成功");
            log.info("Server Cert: {}", mtlsProperties.getServer().getKeyStorePath());

            return sslContext;

        } catch (Exception e) {
            log.error("mTLS服务端SSLContext初始化失败", e);
            throw new RuntimeException("服务端SSLContext初始化失败", e);
        }
    }

    /**
     * 创建信任管理器
     *
     * @return TrustManagerFactory
     * @throws Exception 信任管理器创建异常
     */
    @Bean
    @ConditionalOnMissingBean
    public TrustManagerFactory trustManagerFactory() throws Exception {
        log.debug("创建信任管理器");

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());

        // 加载信任证书库
        KeyStore trustStore = loadTrustStore(mtlsProperties.getClient().getTrustStorePath(),
            mtlsProperties.getClient().getTrustStorePassword());

        trustManagerFactory.init(trustStore);

        log.debug("信任管理器创建成功");
        return trustManagerFactory;
    }

    /**
     * 创建支持mTLS的RestTemplate
     *
     * @param sslContext SSL上下文（客户端）
     * @return RestTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate mTLsRestTemplate(@Qualifier("clientSSLContext") SSLContext sslContext) {
        log.debug("创建mTLS RestTemplate");

        try {
            // 创建 Apache HttpClient5 with mTLS
            CloseableHttpClient httpClient = createApacheHttpClient(sslContext);

            // 创建 HttpComponentsClientHttpRequestFactory
            HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

            // 创建 RestTemplate
            RestTemplate restTemplate = new RestTemplate(factory);

            log.info("mTLS RestTemplate创建成功");
            log.info("Connect Timeout: {}ms", mtlsProperties.getClient().getConnectTimeout());
            log.info("Read Timeout: {}ms", mtlsProperties.getClient().getReadTimeout());

            return restTemplate;

        } catch (Exception e) {
            log.error("创建mTLS RestTemplate失败", e);
            throw new RuntimeException("mTLS RestTemplate创建失败", e);
        }
    }

    /**
     * 创建Apache HttpClient5 with mTLS support
     *
     * @param sslContext SSL上下文
     * @return CloseableHttpClient
     * @throws Exception 客户端创建异常
     */
    private CloseableHttpClient createApacheHttpClient(SSLContext sslContext) throws Exception {
        // 创建 SSLConnectionSocketFactory with mTLS and hostname verification
        // 注意：开发环境可以临时禁用主机名验证，生产环境必须启用
        boolean enableHostnameVerification = mtlsProperties.getClient().isEnableHostnameVerification();

        SSLConnectionSocketFactory sslSocketFactory = enableHostnameVerification
            ? new SSLConnectionSocketFactory(sslContext)  // 使用默认主机名验证
            : new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);  // 仅开发环境使用

        log.debug("创建HTTP客户端 - 主机名验证: {}",
            enableHostnameVerification ? "启用" : "禁用（开发模式）");

        var timeouts = org.apache.hc.client5.http.config.RequestConfig.custom()
            .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(mtlsProperties.getClient().getConnectTimeout()))
            .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(mtlsProperties.getClient().getReadTimeout()))
            .build();

        return HttpClients.custom()
            .setDefaultRequestConfig(timeouts)
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build())
            .build();
    }

    /**
     * 加载KeyStore
     *
     * @param keyStorePath KeyStore路径
     * @param password KeyStore密码
     * @param type KeyStore类型
     * @return KeyStore
     * @throws Exception KeyStore加载异常
     */
    private KeyStore loadKeyStore(String keyStorePath, String password, String type) throws Exception {
        log.debug("加载KeyStore: {}, 类型: {}", keyStorePath, type);

        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream = new FileInputStream(keyStorePath)) {
            keyStore.load(inputStream, password.toCharArray());
        }

        log.debug("KeyStore加载成功");
        return keyStore;
    }

    /**
     * 加载TrustStore
     *
     * @param trustStorePath TrustStore路径
     * @param password TrustStore密码
     * @return KeyStore
     * @throws Exception TrustStore加载异常
     */
    private KeyStore loadTrustStore(String trustStorePath, String password) throws Exception {
        log.debug("加载TrustStore: {}", trustStorePath);

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream inputStream = new FileInputStream(trustStorePath)) {
            trustStore.load(inputStream, password.toCharArray());
        }

        log.debug("TrustStore加载成功");
        return trustStore;
    }

    /**
     * 初始化自签名证书（用于开发环境）
     *
     * @throws Exception 证书生成异常
     */
    @Bean
    @ConditionalOnProperty(name = "basebackend.security.mtls.client.generate-self-signed", havingValue = "true")
    public void generateSelfSignedCertificates() throws Exception {
        if (!mtlsProperties.getClient().isGenerateSelfSigned()) {
            return;
        }

        log.info("生成自签名证书（开发环境）");

        try {
            // 生成服务端证书
            String serverKeyStorePath = mtlsProperties.getServer().getKeyStorePath();
            String serverKeyStorePassword = mtlsProperties.getServer().getKeyStorePassword();
            String serverKeyPassword = mtlsProperties.getServer().getKeyPassword();

            CertificateManager.generateSelfSignedCertificate(
                serverKeyStorePath,
                serverKeyStorePassword,
                serverKeyPassword,
                "CN=" + mtlsProperties.getServer().getCommonName(),
                "BaseBackend Server"
            );

            // 生成客户端证书
            String clientKeyStorePath = mtlsProperties.getClient().getKeyStorePath();
            String clientKeyStorePassword = mtlsProperties.getClient().getKeyStorePassword();
            String clientKeyPassword = mtlsProperties.getClient().getKeyPassword();

            CertificateManager.generateSelfSignedCertificate(
                clientKeyStorePath,
                clientKeyStorePassword,
                clientKeyPassword,
                "CN=" + mtlsProperties.getClient().getCommonName(),
                "BaseBackend Client"
            );

            log.info("自签名证书生成完成");
            log.info("Server Cert: {}", serverKeyStorePath);
            log.info("Client Cert: {}", clientKeyStorePath);

        } catch (Exception e) {
            log.error("自签名证书生成失败", e);
            throw e;
        }
    }

    /**
     * 验证证书有效期
     */
    @Bean
    @ConditionalOnMissingBean
    public void validateCertificates() throws Exception {
        log.info("验证mTLS证书有效期");

        try {
            // 验证服务端证书
            if (mtlsProperties.getServer().isEnabled()) {
                String serverCertPath = mtlsProperties.getServer().getKeyStorePath();
                CertificateManager.validateCertificateValidity(serverCertPath, mtlsProperties.getServer().getKeyStorePassword());
                log.info("服务端证书有效期验证通过");
            }

            // 验证客户端证书
            String clientCertPath = mtlsProperties.getClient().getKeyStorePath();
            CertificateManager.validateCertificateValidity(clientCertPath, mtlsProperties.getClient().getKeyStorePassword());
            log.info("客户端证书有效期验证通过");

        } catch (Exception e) {
            log.error("证书有效性验证失败", e);
            throw e;
        }
    }
}
