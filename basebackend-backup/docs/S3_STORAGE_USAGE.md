# S3云存储使用指南

## 概述

BaseBackend备份系统支持多种S3兼容的云存储服务，包括：
- AWS S3
- 阿里云OSS
- 腾讯云COS
- MinIO自建对象存储
- 其他S3协议兼容存储

## 功能特性

✅ **完整S3协议支持** - 标准S3 API兼容
✅ **多部分上传** - 支持大文件分块上传
✅ **预签名URL** - 支持临时访问链接生成
✅ **MD5验证** - 自动计算和验证ETag
✅ **错误重试** - 内置重试机制和异常处理
✅ **跨平台兼容** - 支持Windows/Linux/macOS

## 快速开始

### 1. 启用S3存储

在 `application-backup.yml` 中启用S3：

```yaml
backup:
  storage:
    s3:
      enabled: true  # 启用S3存储
```

### 2. 配置AWS S3

```yaml
backup:
  storage:
    s3:
      enabled: true
      bucket: your-backup-bucket
      access-key: your-access-key
      secret-key: your-secret-key
      region: us-east-1
      # 可选配置
      connection-timeout: 60s
      read-timeout: 300s
```

### 3. 配置阿里云OSS

```yaml
backup:
  storage:
    s3:
      enabled: true
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      bucket: your-oss-bucket
      access-key: your-access-key
      secret-key: your-secret-key
      region: oss-cn-hangzhou
```

### 4. 配置MinIO自建存储

```yaml
backup:
  storage:
    s3:
      enabled: true
      endpoint: http://minio.example.com:9000
      bucket: basebackend-backup
      access-key: minioadmin
      secret-key: minioadmin
      region: us-east-1
```

### 5. 环境变量配置

推荐使用环境变量避免硬编码密钥：

```bash
# AWS S3
export S3_ENABLED=true
export S3_BUCKET=my-backup-bucket
export S3_ACCESS_KEY=AKIAxxxxxxxxxxxxxxx
export S3_SECRET_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export S3_REGION=us-east-1

# 阿里云OSS
export S3_ENABLED=true
export S3_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
export S3_BUCKET=my-oss-bucket
export S3_ACCESS_KEY=LTAIxxxxxxxxxxxxxxxxxxxxxxxx
export S3_SECRET_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxx
export S3_REGION=oss-cn-hangzhou
```

## 高级配置

### 多部分上传

大文件（>16MB）会自动使用多部分上传：

```yaml
backup:
  storage:
    s3:
      # 分块大小（默认16MB）
      multipart-chunk-size: 16777216  # 字节
      # 连接超时
      connection-timeout: 60s
      # 读取超时
      read-timeout: 300s
```

### 预签名URL生成

系统支持生成预签名URL供临时访问：

```java
// 示例：生成1小时有效的预签名URL
String presignedUrl = storageProvider.getPresignedUrl(
    "backup-bucket",
    "backup/2024-01-01/mysql-full.sql",
    60  // 分钟
);
```

## 存储类型支持

### AWS S3

```yaml
backup:
  storage:
    s3:
      enabled: true
      bucket: my-backup-bucket
      region: ap-southeast-1
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
```

### 阿里云OSS

```yaml
backup:
  storage:
    s3:
      enabled: true
      endpoint: https://oss-{region}.aliyuncs.com
      bucket: my-backup-bucket
      region: oss-cn-hangzhou
      access-key: ${OSS_ACCESS_KEY}
      secret-key: ${OSS_SECRET_KEY}
```

支持的OSS地域：
- oss-cn-hangzhou (杭州)
- oss-cn-beijing (北京)
- oss-cn-shanghai (上海)
- oss-cn-shenzhen (深圳)
- oss-cn-hongkong (香港)
- oss-us-west-1 (硅谷)

### 腾讯云COS

```yaml
backup:
  storage:
    s3:
      enabled: true
      endpoint: https://cos.{region}.myqcloud.com
      bucket: my-backup-bucket
      region: ap-beijing
      access-key: ${COS_ACCESS_KEY}
      secret-key: ${COS_SECRET_KEY}
```

支持的COS地域：
- ap-beijing-1 (北京一区)
- ap-beijing (北京)
- ap-shanghai (上海)
- ap-guangzhou (广州)
- ap-chengdu (成都)
- ap-shenzhen-fsi (深圳金融)
- ap-shanghai-fsi (上海金融)

### MinIO自建

```yaml
backup:
  storage:
    s3:
      enabled: true
      endpoint: http://minio.example.com:9000
      bucket: basebackend-backup
      region: us-east-1
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
```

## 安全建议

### 1. IAM权限配置

为S3备份账户创建专用IAM策略：

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::your-backup-bucket",
                "arn:aws:s3:::your-backup-bucket/*"
            ]
        }
    ]
}
```

### 2. 存储桶策略

启用存储桶版本控制：

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "BackupVersioning",
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::ACCOUNT-ID:user/backup-service"
            },
            "Action": [
                "s3:PutObject",
                "s3:PutObjectVersion"
            ],
            "Resource": "arn:aws:s3:::your-backup-bucket/*"
        }
    ]
}
```

### 3. 数据加密

S3默认支持服务端加密，无需额外配置：

```yaml
backup:
  storage:
    s3:
      enabled: true
      bucket: your-backup-bucket
      # 系统会自动使用SSE-S3加密
```

## 监控和告警

### CloudWatch指标

监控以下指标：
- `NumberOfObjects` - 对象数量
- `BucketSizeBytes` - 存储空间使用量
- `AllRequests` - 请求次数
- `4xxErrors` - 4xx错误数
- `5xxErrors` - 5xx错误数

### 告警配置

```yaml
# CloudWatch告警示例
alarms:
  - name: "S3HighErrorRate"
    metric: "5xxErrors"
    threshold: 100
    period: 300
    evaluation: 2

  - name: "S3BucketSizeWarning"
    metric: "BucketSizeBytes"
    threshold: 1000000000000  # 1TB
    period: 3600
```

## 故障排查

### 常见问题

1. **认证失败**
   ```
   错误：The AWS Access Key Id you provided does not exist
   解决：检查access-key和secret-key是否正确
   ```

2. **权限不足**
   ```
   错误：Access Denied
   解决：检查IAM策略是否包含所需权限
   ```

3. **网络超时**
   ```
   错误：Read timed out
   解决：增加connection-timeout和read-timeout配置
   ```

4. **分块上传失败**
   ```
   错误：Your proposed upload exceeds the maximum allowed object size
   解决：减小multipart-chunk-size值
   ```

### 调试模式

启用详细日志：

```yaml
logging:
  level:
    com.basebackend.backup.infrastructure.storage.impl.S3StorageProvider: DEBUG
```

## 最佳实践

### 1. 分层存储

```yaml
# 热数据 - 标准存储
backup:
  storage:
    s3:
      bucket: hot-backup

# 冷数据 - 低频访问存储
backup:
  storage:
    # 通过生命周期策略自动转换
    lifecycle:
      transitions:
        - days: 30
          storage-class: STANDARD_IA
```

### 2. 生命周期管理

配置生命周期策略自动清理旧备份：

```xml
<LifecycleConfiguration>
  <Rule>
    <ID>DeleteOldBackups</ID>
    <Status>Enabled</Status>
    <Expiration>
      <Days>90</Days>
    </Expiration>
  </Rule>
</LifecycleConfiguration>
```

### 3. 跨区域复制

配置CRR（跨区域复制）提高可用性：

```json
{
    "Role": "arn:aws:iam::ACCOUNT-ID:role/CrossRegionReplicationRole",
    "Rules": [{
        "Status": "Enabled",
        "Prefix": "",
        "Destination": {
            "Bucket": "arn:aws:s3:::backup-replica-bucket"
        }
    }]
}
```

## 性能优化

### 1. 多线程上传

系统自动使用多线程上传多部分文件，提高吞吐量。

### 2. 就近访问

选择距离业务系统最近的S3地域，减少延迟：

```yaml
backup:
  storage:
    s3:
      region: ap-southeast-1  # 新加坡（就近）
```

### 3. CDN加速

对于频繁访问的备份文件，可配置CloudFront CDN：

```yaml
backup:
  storage:
    cdn:
      enabled: true
      domain: dxxxxx.cloudfront.net
```

## 示例代码

### 编程方式配置

```java
@Configuration
public class S3Config {

    @Bean
    @ConditionalOnProperty(name = "backup.storage.s3.enabled", havingValue = "true")
    public StorageProvider s3StorageProvider(BackupProperties backupProperties) {
        return new S3StorageProvider();
    }
}
```

### 使用示例

```java
@Autowired
private StorageProvider storageProvider;

// 上传文件
UploadRequest uploadRequest = new UploadRequest();
uploadRequest.setBucket("backup-bucket");
uploadRequest.setKey("mysql/backup-2024-01-01.sql");
uploadRequest.setInputStream(new FileInputStream(file));
uploadRequest.setSize(file.length());

StorageResult result = storageProvider.upload(uploadRequest);
log.info("上传成功: {}", result.getLocation());

// 生成预签名URL
String url = storageProvider.getPresignedUrl(
    "backup-bucket",
    "mysql/backup-2024-01-01.sql",
    60  // 1小时
);
log.info("预签名URL: {}", url);
```

## 参考资料

- [AWS S3官方文档](https://docs.aws.amazon.com/s3/)
- [阿里云OSS官方文档](https://help.aliyun.com/product/29415.html)
- [腾讯云COS官方文档](https://cloud.tencent.com/document/product/436)
- [MinIO官方文档](https://docs.min.io/)
- [S3协议兼容性说明](https://docs.aws.amazon.com/zh_cn/general/latest/gr/aws_response_signing.html)

---

**注意**：
- 确保S3存储桶的CORS配置允许应用访问
- 定期备份IAM凭证，避免凭证泄露
- 监控S3使用量，避免意外产生高额费用
