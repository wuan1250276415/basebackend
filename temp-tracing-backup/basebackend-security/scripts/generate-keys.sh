#!/bin/bash
# =====================================================================
# BaseBackend 安全密钥和证书生成脚本
# 创建时间: 2025-11-15
# 描述: 生成AES密钥、RSA密钥对和SSL证书
# =====================================================================

set -e

echo "======================================="
echo "BaseBackend 安全密钥和证书生成"
echo "======================================="

# 配置变量
KEY_DIR="/opt/basebackend/security/keys"
SSL_DIR="/opt/basebackend/security/ssl"
ENCRYPTION_DIR="/opt/basebackend/security/encryption"

# 创建目录
mkdir -p $KEY_DIR
mkdir -p $SSL_DIR
mkdir -p $ENCRYPTION_DIR

# 生成AES-256密钥
echo "生成AES-256密钥..."
AES_KEY=$(openssl rand -base64 32)
echo $AES_KEY > $ENCRYPTION_DIR/aes-key.txt
echo "AES密钥已保存到: $ENCRYPTION_DIR/aes-key.txt"

# 生成RSA密钥对
echo "生成RSA-2048密钥对..."
openssl genpkey -algorithm RSA -out $KEY_DIR/rsa-private-key.pem -pkcs8 -aes256 -pass pass:basebackend-pass
openssl rsa -pubout -in $KEY_DIR/rsa-private-key.pem -passin pass:basebackend-pass -out $KEY_DIR/rsa-public-key.pem

# 转换为Base64格式
openssl base64 -in $KEY_DIR/rsa-private-key.pem -out $KEY_DIR/rsa-private-key-base64.txt
openssl base64 -in $KEY_DIR/rsa-public-key.pem -out $KEY_DIR/rsa-public-key-base64.txt

echo "RSA密钥对已保存到:"
echo "  私钥: $KEY_DIR/rsa-private-key-base64.txt"
echo "  公钥: $KEY_DIR/rsa-public-key-base64.txt"

# 生成自签名SSL证书 (开发环境)
echo ""
echo "生成自签名SSL证书..."
openssl req -x509 -newkey rsa:4096 -nodes \
    -keyout $SSL_DIR/basebackend-key.pem \
    -out $SSL_DIR/basebackend-cert.pem \
    -days 365 \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=BaseBackend/OU=BaseBackend/CN=localhost"

# 创建PKCS12证书格式
openssl pkcs12 -export -in $SSL_DIR/basebackend-cert.pem \
    -inkey $SSL_DIR/basebackend-key.pem \
    -out $SSL_DIR/basebackend-keystore.p12 \
    -name basebackend \
    -passout pass:basebackend-pass

# 生成证书签名请求(CSR)
openssl req -new -key $SSL_DIR/basebackend-key.pem \
    -out $SSL_DIR/basebackend.csr \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=BaseBackend/OU=BaseBackend/CN=localhost"

echo "SSL证书已生成:"
echo "  证书文件: $SSL_DIR/basebackend-cert.pem"
echo "  私钥文件: $SSL_DIR/basebackend-key.pem"
echo "  证书库: $SSL_DIR/basebackend-keystore.p12"

# 生成TrustStore (用于客户端信任服务器证书)
keytool -importcert -alias basebackend \
    -file $SSL_DIR/basebackend-cert.pem \
    -keystore $SSL_DIR/truststore.jks \
    -storepass truststore-pass \
    -noprompt

echo "TrustStore已生成: $SSL_DIR/truststore.jks"

# 生成配置模板
cat > $ENCRYPTION_DIR/security-config-template.properties << 'EOF'
# =====================================================================
# BaseBackend 安全配置模板
# 创建时间: 2025-11-15
# =====================================================================

# AES加密配置
security.encryption.aes.key=BASE64_ENCODED_AES_KEY

# RSA加密配置
security.encryption.rsa.privateKey=BASE64_ENCODED_RSA_PRIVATE_KEY
security.encryption.rsa.publicKey=BASE64_ENCODED_RSA_PUBLIC_KEY

# SSL配置
server.ssl.enabled=true
server.ssl.key-store=classpath:ssl/basebackend-keystore.p12
server.ssl.key-store-password=basebackend-pass
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=basebackend
server.ssl.key-password=basebackend-pass

# 客户端SSL配置
spring.ssl.bundle.jks.client.trust-store=classpath:ssl/truststore.jks
spring.ssl.bundle.jks.client.trust-store-password=truststore-pass

# Jasypt加密配置
jasypt.encryptor.password=basebackend-encrypt-password
EOF

# 创建加密配置示例
cat > $ENCRYPTION_DIR/application-security.yml << 'EOF'
# =====================================================================
# BaseBackend 应用安全配置示例
# 创建时间: 2025-11-15
# =====================================================================

# 数据加密配置
security:
  encryption:
    # AES加密
    aes:
      enabled: true
      key-length: 256
      iv-length: 12
    # RSA加密
    rsa:
      enabled: true
      key-size: 2048
      algorithm: RSA/ECB/PKCS1Padding

  # 传输加密
  transport:
    # HTTPS配置
    https:
      enabled: true
      port: 8443
      redirect-http: true
    # 双向SSL
    mutual-tls:
      enabled: false

  # 安全审计
  audit:
    enabled: true
    log-level: INFO

# 数据库连接加密
spring:
  datasource:
    # 使用Jasypt加密数据库密码
    password: ENC(加密后的数据库密码)
    druid:
      # 连接池配置
      connection-init-sqls:
        - "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci"
        - "SET sql_mode='STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO'"

# Redis加密配置
spring:
  redis:
    password: ENC(加密后的Redis密码)
    ssl:
      enabled: true

# Jasypt配置
jasypt:
  encryptor:
    algorithm: PBEWITHHMACSHA512ANDAES_256
    password: basebackend-encrypt-password

# Actuator安全配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
  security:
    enabled: true

# 日志配置
logging:
  level:
    com.basebackend.security: DEBUG
    org.springframework.security: DEBUG
EOF

# 生成密钥摘要文件
cat > $ENCRYPTION_DIR/keys-summary.txt << EOF
=======================================
BaseBackend 安全密钥和证书摘要
=======================================
生成时间: $(date)

目录结构:
  密钥目录: $KEY_DIR
  SSL目录: $SSL_DIR
  加密目录: $ENCRYPTION_DIR

生成的文件:
  AES密钥: $ENCRYPTION_DIR/aes-key.txt
  RSA私钥: $KEY_DIR/rsa-private-key-base64.txt
  RSA公钥: $KEY_DIR/rsa-public-key-base64.txt
  SSL证书: $SSL_DIR/basebackend-cert.pem
  SSL私钥: $SSL_DIR/basebackend-key.pem
  KeyStore: $SSL_DIR/basebackend-keystore.p12
  TrustStore: $SSL_DIR/truststore.jks

配置文件:
  配置模板: $ENCRYPTION_DIR/security-config-template.properties
  应用配置: $ENCRYPTION_DIR/application-security.yml

密钥信息:
  AES密钥长度: 256位
  RSA密钥长度: 2048位
  SSL证书有效期: 365天
  Jasypt算法: PBEWITHHMACSHA512ANDAES_256

注意事项:
1. 请妥善保管密钥文件，不要提交到代码仓库
2. 生产环境请使用正式的SSL证书
3. 定期轮换密钥和证书
4. 使用环境变量或安全配置中心存储密钥
=======================================
EOF

echo ""
echo "======================================="
echo "✅ 所有密钥和证书生成完成!"
echo "======================================="
echo "📁 文件位置:"
echo "  密钥目录: $KEY_DIR"
echo "  SSL目录: $SSL_DIR"
echo "  加密目录: $ENCRYPTION_DIR"
echo ""
echo "📋 生成摘要:"
cat $ENCRYPTION_DIR/keys-summary.txt
echo ""
echo "⚠️  重要提醒:"
echo "  1. 请妥善保管密钥文件，不要提交到代码仓库"
echo "  2. 生产环境请使用正式的SSL证书"
echo "  3. 定期轮换密钥和证书"
echo "======================================="
