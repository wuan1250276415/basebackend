package com.basebackend.security.mtls.certificate;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * mTLS证书管理器
 *
 * 负责：
 * - 生成自签名证书
 * - 加载和管理证书
 * - 验证证书有效期
 * - 创建KeyManager和TrustManager
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
public class CertificateManager {

    static {
        // 注册BouncyCastle Provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.debug("已注册BouncyCastle加密Provider");
        }
    }

    /**
     * 生成自签名证书
     *
     * @param keyStorePath KeyStore保存路径
     * @param keyStorePassword KeyStore密码
     * @param keyPassword 私钥密码
     * @param cn 通用名称（Common Name）
     * @param organization 组织名称（Organization）
     * @throws Exception 证书生成异常
     */
    public static void generateSelfSignedCertificate(String keyStorePath,
                                                   String keyStorePassword,
                                                   String keyPassword,
                                                   String cn,
                                                   String organization) throws Exception {
        log.info("生成自签名证书 - CN: {}, Org: {}, Path: {}", cn, organization, keyStorePath);

        try {
            // 1. 生成密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // 2. 生成证书
            X509Certificate cert = generateX509Certificate(keyPair, cn, organization);

            // 3. 创建KeyStore并存储证书和私钥
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null); // 初始化空KeyStore

            // 存储私钥和证书链
            keyStore.setKeyEntry(
                "mtls-cert", // 别名
                keyPair.getPrivate(),
                keyPassword.toCharArray(),
                new Certificate[]{cert}
            );

            // 4. 保存KeyStore到文件
            try (FileOutputStream fos = new FileOutputStream(keyStorePath)) {
                keyStore.store(fos, keyStorePassword.toCharArray());
            }

            // 5. 验证证书
            validateCertificateValidity(keyStorePath, keyStorePassword);

            log.info("自签名证书生成成功: {}", keyStorePath);
            log.info("证书序列号: {}", cert.getSerialNumber());
            log.info("证书有效期: {} - {}", cert.getNotBefore(), cert.getNotAfter());

        } catch (Exception e) {
            log.error("生成自签名证书失败", e);
            throw new RuntimeException("证书生成失败", e);
        }
    }

    /**
     * 生成X509证书
     *
     * @param keyPair 密钥对
     * @param cn 通用名称
     * @param org 组织名称
     * @return X509Certificate
     * @throws Exception 证书生成异常
     */
    private static X509Certificate generateX509Certificate(KeyPair keyPair, String cn, String organization) throws Exception {
        log.debug("生成X509证书 - CN: {}, Org: {}", cn, organization);

        // 证书主题
        X500Name subjectName = new X500Name(
            "CN=" + cn + ", " +
            "O=" + organization + ", " +
            "C=CN"
        );

        // 证书序列号
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        // 证书有效期（10年）
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // 提前1天避免时区问题
        Date notAfter = new Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000);

        // 证书颁发者（自签名证书，使用主题作为颁发者）
        X500Name issuerName = subjectName;

        // 创建证书构建器
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuerName, // 颁发者
            serialNumber, // 序列号
            notBefore, // 生效时间
            notAfter, // 失效时间
            subjectName, // 主题
            keyPair.getPublic() // 公钥
        );

        // 添加扩展
        // BasicConstraints扩展
        BasicConstraints basicConstraints = new BasicConstraints(false);
        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            basicConstraints
        );

        // SubjectKeyIdentifier扩展
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        SubjectKeyIdentifier subjectKeyIdentifier =
            new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKeyInfo);
        certBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false,
            subjectKeyIdentifier
        );

        // 创建内容签名者
        String signatureAlgorithm = "SHA256withRSA";
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
            .setProvider("BC")
            .build(keyPair.getPrivate());

        // 构建证书
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);

        // 转换为Java X509Certificate
        X509Certificate cert = new JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder);

        log.debug("X509证书生成成功");
        return cert;
    }

    /**
     * 验证证书有效性
     *
     * @param keyStorePath KeyStore路径
     * @param keyStorePassword KeyStore密码
     * @throws Exception 验证异常
     */
    public static void validateCertificateValidity(String keyStorePath, String keyStorePassword) throws Exception {
        log.debug("验证证书有效性: {}", keyStorePath);

        try {
            // 加载KeyStore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                keyStore.load(fis, keyStorePassword.toCharArray());
            }

            // 获取证书
            Certificate cert = keyStore.getCertificate("mtls-cert");
            if (cert == null) {
                throw new RuntimeException("未找到证书: mtls-cert");
            }

            // 验证证书有效期
            if (!(cert instanceof X509Certificate)) {
                throw new RuntimeException("证书不是X509类型");
            }

            X509Certificate x509Cert = (X509Certificate) cert;
            x509Cert.checkValidity();

            // 计算剩余有效期天数
            long remainingDays = (x509Cert.getNotAfter().getTime() - System.currentTimeMillis()) / (24 * 60 * 60 * 1000);

            log.info("证书有效性验证通过");
            log.info("证书序列号: {}", x509Cert.getSerialNumber());
            log.info("证书主题: {}", x509Cert.getSubjectX500Principal().getName());
            log.info("证书颁发者: {}", x509Cert.getIssuerX500Principal().getName());
            log.info("证书有效期: {} - {}", x509Cert.getNotBefore(), x509Cert.getNotAfter());
            log.info("剩余有效期: {} 天", remainingDays);

            // 检查是否即将过期（少于30天）
            if (remainingDays < 30) {
                log.warn("证书将在 {} 天后过期，请及时更新！", remainingDays);
            }

        } catch (CertificateException e) {
            log.error("证书验证失败: {}", e.getMessage(), e);
            throw new RuntimeException("证书验证失败", e);
        }
    }

    /**
     * 创建KeyManager
     *
     * @param keyStore KeyStore
     * @param keyPassword 私钥密码
     * @return KeyManager数组
     * @throws Exception KeyManager创建异常
     */
    public static KeyManager[] createKeyManager(KeyStore keyStore, String keyPassword) throws Exception {
        log.debug("创建KeyManager");

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyPassword.toCharArray());

        log.debug("KeyManager创建成功");
        return keyManagerFactory.getKeyManagers();
    }

    /**
     * 创建TrustManager
     *
     * @param trustStore TrustStore
     * @return TrustManager数组
     * @throws Exception TrustManager创建异常
     */
    public static TrustManager[] createTrustManager(KeyStore trustStore) throws Exception {
        log.debug("创建TrustManager");

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        log.debug("TrustManager创建成功");
        return trustManagerFactory.getTrustManagers();
    }

    /**
     * 从PEM格式文件加载证书
     *
     * @param pemPath PEM文件路径
     * @return X509Certificate
     * @throws Exception 加载异常
     */
    public static X509Certificate loadCertificateFromPEM(String pemPath) throws Exception {
        log.debug("从PEM文件加载证书: {}", pemPath);

        try (PEMParser pemParser = new PEMParser(new FileReader(pemPath))) {
            Object obj = pemParser.readObject();

            if (obj instanceof X509CertificateHolder) {
                X509CertificateHolder holder = (X509CertificateHolder) obj;
                return new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(holder);
            } else {
                throw new RuntimeException("PEM文件格式不正确");
            }
        }
    }

    /**
     * 从KeyStore中提取证书信息
     *
     * @param keyStorePath KeyStore路径
     * @param keyStorePassword KeyStore密码
     * @param alias 证书别名
     * @return 证书信息
     * @throws Exception 提取异常
     */
    public static CertificateInfo extractCertificateInfo(String keyStorePath,
                                                        String keyStorePassword,
                                                        String alias) throws Exception {
        log.debug("提取证书信息: {}, Alias: {}", keyStorePath, alias);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }

        Certificate cert = keyStore.getCertificate(alias);
        if (cert == null) {
            throw new RuntimeException("未找到证书: " + alias);
        }

        if (!(cert instanceof X509Certificate)) {
            throw new RuntimeException("证书不是X509类型");
        }

        X509Certificate x509Cert = (X509Certificate) cert;

        CertificateInfo info = new CertificateInfo();
        info.setSerialNumber(x509Cert.getSerialNumber().toString());
        info.setSubject(x509Cert.getSubjectX500Principal().getName());
        info.setIssuer(x509Cert.getIssuerX500Principal().getName());
        info.setValidFrom(x509Cert.getNotBefore());
        info.setValidTo(x509Cert.getNotAfter());
        info.setAlgorithm(x509Cert.getPublicKey().getAlgorithm());
        info.setKeySize(getKeySize(x509Cert.getPublicKey()));

        log.info("证书信息提取成功: {}", info.getSerialNumber());

        return info;
    }

    /**
     * 获取密钥长度
     *
     * @param publicKey 公钥
     * @return 密钥长度
     */
    private static int getKeySize(PublicKey publicKey) {
        try {
            if (publicKey instanceof java.security.interfaces.RSAPublicKey) {
                return ((java.security.interfaces.RSAPublicKey) publicKey).getModulus().bitLength();
            } else if (publicKey instanceof java.security.interfaces.ECPublicKey) {
                return ((java.security.interfaces.ECPublicKey) publicKey).getParams().getCurve().getField().getFieldSize();
            }
            return 0;
        } catch (Exception e) {
            log.warn("无法获取密钥长度", e);
            return 0;
        }
    }

    /**
     * 证书信息内部类
     */
    public static class CertificateInfo {
        private String serialNumber;
        private String subject;
        private String issuer;
        private Date validFrom;
        private Date validTo;
        private String algorithm;
        private int keySize;

        // Getters and Setters
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }

        public Date getValidFrom() { return validFrom; }
        public void setValidFrom(Date validFrom) { this.validFrom = validFrom; }

        public Date getValidTo() { return validTo; }
        public void setValidTo(Date validTo) { this.validTo = validTo; }

        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

        public int getKeySize() { return keySize; }
        public void setKeySize(int keySize) { this.keySize = keySize; }

        @Override
        public String toString() {
            return String.format(
                "CertificateInfo{serialNumber='%s', subject='%s', issuer='%s', algorithm='%s', keySize=%d}",
                serialNumber, subject, issuer, algorithm, keySize
            );
        }
    }
}
