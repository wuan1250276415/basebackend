# Phase 12.3: 云原生存储实施指南

## 📋 概述

本指南介绍如何实施云原生存储解决方案，包括对象存储、分布式文件系统和数据库云化，构建高性能、高可用的存储架构。

---

## 🏗️ 云原生存储架构

### 存储层次架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                      云原生存储架构                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   对象存储    │  │  分布式文件系统 │  │   云数据库    │           │
│  │              │  │              │  │              │           │
│  │ • MinIO      │  │ • CephFS     │  │ • RDS MySQL  │           │
│  │ • S3 兼容    │  │ • 块存储      │  │ • PostgreSQL │           │
│  │ • CDN 加速   │  │ • POSIX      │  │ • MongoDB    │           │
│  │ • 版本管理    │  │ • 快照        │  │ • Redis Cloud│           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   备份归档    │  │   灾难恢复   │  │   监控运维   │           │
│  │              │  │              │  │              │           │
│  │ • 定期备份   │  │ • 主从复制   │  │ • 性能监控   │           │
│  │ • 生命周期管理│  │ • 跨区域同步 │  │ • 容量规划   │           │
│  │ • 冷存储     │  │ • 自动切换   │  │ • 告警通知   │           │
│  │ • 数据加密   │  │ • 备份恢复   │  │ • 健康检查   │           │
│  └──────────────┘  └─────────────┘  └─────────────┘           │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    存储服务层                                 │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • CSI 驱动 (Container Storage Interface)                   │ │
│  │ • StorageClass (存储类)                                     │ │
│  │ • PersistentVolume (持久化卷)                               │ │
│  │ • PersistentVolumeClaim (持久化卷声明)                       │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 存储类型对比

| 存储类型 | 特点 | 适用场景 | 性能 | 成本 |
|----------|------|----------|------|------|
| **对象存储** | 海量存储、S3协议 | 文件存储、备份、静态资源 | 中等 | 低 |
| **分布式文件系统** | POSIX兼容、共享访问 | 数据分析、容器编排 | 高 | 中 |
| **块存储** | 低延迟、IOPS高 | 数据库存储、虚拟化 | 最高 | 高 |
| **云数据库** | 托管服务、高可用 | 业务数据库、缓存 | 高 | 中 |

---

## 📦 对象存储集成 (MinIO)

### 1. MinIO 部署配置

```yaml
# minio-deployment.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: minio-storage

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: minio
  namespace: minio-storage
spec:
  replicas: 2
  selector:
    matchLabels:
      app: minio
  template:
    metadata:
      labels:
        app: minio
    spec:
      containers:
      - name: minio
        image: minio/minio:latest
        args:
          - server
          - /data
          - --console-address
          - :9001
        ports:
        - containerPort: 9000
          name: s3
        - containerPort: 9001
          name: console
        env:
        - name: MINIO_ROOT_USER
          value: "admin"
        - name: MINIO_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: minio-secret
              key: password
        volumeMounts:
        - name: data
          mountPath: /data
        resources:
          limits:
            cpu: 1000m
            memory: 2Gi
          requests:
            cpu: 500m
            memory: 1Gi
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: minio-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: minio
  namespace: minio-storage
spec:
  type: LoadBalancer
  ports:
  - port: 9000
    targetPort: 9000
    name: s3
  - port: 9001
    targetPort: 9001
    name: console
  selector:
    app: minio

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: minio-pvc
  namespace: minio-storage
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
  storageClassName: fast-ssd

---
apiVersion: v1
kind: Secret
metadata:
  name: minio-secret
  namespace: minio-storage
type: Opaque
data:
  password: cGFzc3dvcmQxMjM=  # base64编码的密码
```

### 2. MinIO Client 配置

```bash
#!/bin/bash
# minio-setup.sh

MINIO_ENDPOINT="http://minio.minio-storage:9000"
MINIO_ACCESS_KEY="admin"
MINIO_SECRET_KEY="password123"

# 安装 mc 客户端
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc
mv mc /usr/local/bin/

# 配置 mc
mc alias set basebackend $MINIO_ENDPOINT $MINIO_ACCESS_KEY $MINIO_SECRET_KEY

# 创建 bucket
mc mb basebackend/files
mc mb basebackend/images
mc mb basebackend/backups
mc mb basebackend/logs

# 设置存储桶策略
mc policy set public basebackend/files
mc policy set public basebackend/images

# 配置生命周期管理（30天后转冷存储）
mc ilm add basebackend/backups --days 30 --storage-class GLACIER

# 开启版本管理
mc version enable basebackend/files

# 开启加密
mc encrypt set sse-s3 basebackend/files

echo "MinIO 配置完成!"
```

### 3. Java SDK 集成

```java
/**
 * MinIO 对象存储服务
 */
@Service
public class MinioStorageService {

    @Autowired
    private MinioClient minioClient;

    private final String bucketName = "basebackend-files";

    /**
     * 上传文件
     */
    public void uploadFile(String objectName, InputStream inputStream,
                          String contentType, long size) {
        try {
            // 检查 bucket 是否存在
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );

            if (!exists) {
                minioClient.createBucket(
                    CreateBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
            }

            // 上传文件
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            );

            log.info("文件上传成功: {}", objectName);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new StorageException("文件上传失败", e);
        }
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String objectName) {
        try {
            GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            return response;
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new StorageException("文件下载失败", e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            log.info("文件删除成功: {}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new StorageException("文件删除失败", e);
        }
    }

    /**
     * 获取文件访问URL
     */
    public String getFileUrl(String objectName, int expirySeconds) {
        try {
            return minioClient.presignedGetObject(
                bucketName, objectName, expirySeconds
            );
        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            throw new StorageException("获取文件URL失败", e);
        }
    }

    /**
     * 复制文件
     */
    public void copyFile(String sourceObject, String targetObject) {
        try {
            CopyObjectResponse response = minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(targetObject)
                    .source(
                        CopySource.builder()
                            .bucket(bucketName)
                            .object(sourceObject)
                            .build()
                    )
                    .build()
            );
            log.info("文件复制成功: {} -> {}", sourceObject, targetObject);
        } catch (Exception e) {
            log.error("文件复制失败", e);
            throw new StorageException("文件复制失败", e);
        }
    }

    /**
     * 列出文件
     */
    public List<Item> listFiles(String prefix) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .recursive(true)
                    .build()
            );

            List<Item> items = new ArrayList<>();
            for (Result<Item> result : results) {
                items.add(result.get());
            }
            return items;
        } catch (Exception e) {
            log.error("列出文件失败", e);
            throw new StorageException("列出文件失败", e);
        }
    }
}
```

### 4. Storage 配置

```java
/**
 * 存储配置
 */
@Configuration
public class StorageConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }
}
```

### 5. Spring Boot 集成

```yaml
# application-storage.yml
minio:
  endpoint: http://minio.minio-storage:9000
  access-key: admin
  secret-key: password123
  bucket: basebackend-files

spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

```java
/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/storage")
@Api(tags = "文件存储")
public class FileStorageController {

    @Autowired
    private MinioStorageService storageService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String objectName = generateObjectName(file.getOriginalFilename());
            storageService.uploadFile(
                objectName,
                file.getInputStream(),
                file.getContentType(),
                file.getSize()
            );

            // 返回访问URL
            String fileUrl = storageService.getFileUrl(objectName, 3600);
            return Result.success(fileUrl);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败");
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{objectName}")
    @ApiOperation("下载文件")
    public ResponseEntity<Resource> downloadFile(@PathVariable String objectName) {
        try {
            InputStream is = storageService.downloadFile(objectName);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + objectName);

            return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(is));
        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{objectName}")
    @ApiOperation("删除文件")
    public Result<Void> deleteFile(@PathVariable String objectName) {
        try {
            storageService.deleteFile(objectName);
            return Result.success();
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return Result.error("文件删除失败");
        }
    }

    private String generateObjectName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return String.format("%s/%s%s", timestamp, uuid, extension);
    }
}
```

---

## 🗄️ 分布式文件系统 (CephFS)

### 1. Ceph 集群部署

```yaml
# ceph-cluster.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ceph-storage

---
# Ceph Monitors
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ceph-mon
  namespace: ceph-storage
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ceph-mon
  template:
    metadata:
      labels:
        app: ceph-mon
    spec:
      containers:
      - name: ceph-mon
        image: ceph/ceph:v17.2.5
        command: ["/bin/bash"]
        args:
          - -c
          - |
            ceph-mon --fsid=$FSID \
                     --mon.cluster=$CLUSTER_NAME \
                     --mon.interface=eth0 \
                     --mon.hostname=$(hostname) \
                     --public-addr=$PUBLIC_IP \
                     --setuser=ceph \
                     --setgroup=ceph \
                     --log-to-stderr=true \
                     --err-to-stderr=false \
                     --log-level=info \
                     --mon-data=$MON_DATA_DIR \
                     --mon.initial-members=$MON_INITIAL_MEMBERS
        env:
        - name: FSID
          value: "12345678-1234-1234-1234-123456789012"
        - name: CLUSTER_NAME
          value: "ceph-cluster"
        - name: PUBLIC_IP
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
        - name: MON_DATA_DIR
          value: "/var/lib/ceph/mon/ceph-$(hostname)"
        - name: MON_INITIAL_MEMBERS
          value: "ceph-mon-0,ceph-mon-1,ceph-mon-2"
        volumeMounts:
        - name: mon-data
          mountPath: /var/lib/ceph/mon
        ports:
        - containerPort: 6789
          name: mon
      volumes:
      - name: mon-data
        persistentVolumeClaim:
          claimName: ceph-mon-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: ceph-mon-service
  namespace: ceph-storage
spec:
  clusterIP: None
  selector:
    app: ceph-mon
  ports:
  - port: 6789
    name: mon

---
# Ceph OSDs
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: ceph-osd
  namespace: ceph-storage
spec:
  selector:
    matchLabels:
      app: ceph-osd
  template:
    metadata:
      labels:
        app: ceph-osd
    spec:
      containers:
      - name: ceph-osd
        image: ceph/ceph:v17.2.5
        command: ["/bin/bash"]
        args:
          - -c
          - |
            ceph-osd --fsid=$FSID \
                     --setuser=ceph \
                     --setgroup=ceph \
                     --log-to-stderr=true \
                     --err-to-stderr=false \
                     --log-level=info \
                     --cluster=$CLUSTER_NAME \
                     --osd-data=$OSD_DATA_DIR \
                     --osd-journal=$OSD_JOURNAL_DIR \
                     --public-addr=$PUBLIC_IP
        env:
        - name: FSID
          value: "12345678-1234-1234-1234-123456789012"
        - name: CLUSTER_NAME
          value: "ceph-cluster"
        - name: PUBLIC_IP
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
        - name: OSD_DATA_DIR
          value: "/var/lib/ceph/osd/ceph-$(hostname)"
        - name: OSD_JOURNAL_DIR
          value: "/var/lib/ceph/osd/journal"
        volumeMounts:
        - name: osd-data
          mountPath: /var/lib/ceph/osd
        - name: osd-journal
          mountPath: /var/lib/ceph/osd/journal
      volumes:
      - name: osd-data
        hostPath:
          path: /var/lib/ceph/osd
      - name: osd-journal
        hostPath:
          path: /var/lib/ceph/osd/journal
```

### 2. CephFS 客户端配置

```yaml
# cephfs-provisioner.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: cephfs-provisioner
  namespace: ceph-storage

---
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: cephfs-provisioner
rules:
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["create", "get", "delete"]
- apiGroups: [""]
  resources: ["endpoints"]
  verbs: ["get"]
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["list", "get"]
- apiGroups: [""]
  resources: ["namespaces"]
  verbs: ["list", "get"]
- apiGroups: [""]
  resources: ["persistentvolumes"]
  verbs: ["list", "get", "create", "delete"]
- apiGroups: [""]
  resources: ["persistentvolumeclaims"]
  verbs: ["list", "get", "update", "create"]

---
kind: ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: cephfs-provisioner
subjects:
- kind: ServiceAccount
  name: cephfs-provisioner
  namespace: ceph-storage
roleRef:
  kind: ClusterRole
  name: cephfs-provisioner
  apiGroup: rbac.authorization.k8s.io/v1

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cephfs-provisioner
  namespace: ceph-storage
spec:
  selector:
    matchLabels:
      app: cephfs-provisioner
  replicas: 1
  template:
    metadata:
      labels:
        app: cephfs-provisioner
    spec:
      serviceAccountName: cephfs-provisioner
      containers:
      - name: provisioner
        image: ceph/cephfs-provisioner:latest
        env:
        - name: PROVISIONER_NAME
          value: "ceph.com/cephfs"
        - name: MONITOR_ENDPOINT
          value: "ceph-mon-service.ceph-storage:6789"
        - name: MONITOR_PATH
          value: "/ceph-mon-map"
        - name: MONITOR_USER
          value: "admin"
```

### 3. StorageClass 配置

```yaml
# cephfs-storageclass.yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: cephfs
  provisioner: ceph.com/cephfs
  parameters:
    monitors: "ceph-mon-service.ceph-storage:6789"
    adminId: admin
    adminSecretName: ceph-secret
    adminSecretNamespace: ceph-storage
    path: "/"
    fsType: ceph
    pool: cephfs_data
reclaimPolicy: Delete
allowVolumeExpansion: true
mountOptions:
  - debug
```

### 4. 客户端使用示例

```java
/**
 * CephFS 文件系统客户端
 */
@Component
public class CephFSClient {

    private Path cephFSPath;
    private FileSystem cephFS;

    @PostConstruct
    public void init() {
        try {
            // 连接到 CephFS 集群
            String monitorAddress = "ceph-mon-service.ceph-storage:6789";
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", "hdfs://" + monitorAddress);

            cephFS = FileSystem.get(conf);
            cephFSPath = new Path("/basebackend-data");

            // 确保目录存在
            if (!cephFS.exists(cephFSPath)) {
                cephFS.mkdirs(cephFSPath);
            }
        } catch (Exception e) {
            log.error("CephFS 初始化失败", e);
        }
    }

    /**
     * 上传文件到 CephFS
     */
    public void uploadToCephFS(String localFilePath, String remoteFilePath) {
        try {
            Path remotePath = new Path(cephFSPath, remoteFilePath);
            Path localPath = Paths.get(localFilePath);

            cephFS.copyFromLocalFile(localPath, remotePath);
            log.info("文件上传到 CephFS: {}", remoteFilePath);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new StorageException("CephFS 文件上传失败", e);
        }
    }

    /**
     * 从 CephFS 下载文件
     */
    public void downloadFromCephFS(String remoteFilePath, String localFilePath) {
        try {
            Path remotePath = new Path(cephFSPath, remoteFilePath);
            Path localPath = Paths.get(localFilePath);

            // 确保本地目录存在
            Files.createDirectories(localPath.getParent());

            cephFS.copyToLocalFile(remotePath, localPath);
            log.info("文件从 CephFS 下载: {}", remoteFilePath);
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new StorageException("CephFS 文件下载失败", e);
        }
    }

    /**
     * 删除 CephFS 上的文件
     */
    public void deleteFromCephFS(String remoteFilePath) {
        try {
            Path remotePath = new Path(cephFSPath, remoteFilePath);
            cephFS.delete(remotePath, true);
            log.info("文件从 CephFS 删除: {}", remoteFilePath);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new StorageException("CephFS 文件删除失败", e);
        }
    }

    /**
     * 列出 CephFS 上的文件
     */
    public FileStatus[] listFiles(String remoteDir) {
        try {
            Path remotePath = new Path(cephFSPath, remoteDir);
            return cephFS.listStatus(remotePath);
        } catch (Exception e) {
            log.error("列出文件失败", e);
            throw new StorageException("CephFS 列出文件失败", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (cephFS != null) {
            try {
                cephFS.close();
            } catch (Exception e) {
                log.error("关闭 CephFS 连接失败", e);
            }
        }
    }
}
```

---

## 💾 数据库云化

### 1. RDS MySQL 主从配置

```yaml
# mysql-rds.yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysql-secret
type: Opaque
data:
  password: cGFzc3dvcmQxMjM=  # base64编码的密码

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: mysql-config
data:
  master.cnf: |
    [mysqld]
    server-id = 1
    log-bin = mysql-bin
    binlog-format = ROW
    sync_binlog = 1
    innodb_flush_log_at_trx_commit = 1
    max_connections = 2000

  slave.cnf: |
    [mysqld]
    server-id = 2
    read_only = 1
    relay_log = mysql-relay-log
    log-slave-updates = 1
    max_connections = 2000

---
# MySQL 主库
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql-master
  labels:
    app: mysql
    role: master
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
      role: master
  template:
    metadata:
      labels:
        app: mysql
        role: master
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        ports:
        - containerPort: 3306
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: password
        volumeMounts:
        - name: mysql-config
          mountPath: /etc/mysql/conf.d/
        - name: mysql-data
          mountPath: /var/lib/mysql
        resources:
          limits:
            memory: 4Gi
            cpu: 2000m
          requests:
            memory: 2Gi
            cpu: 1000m
      volumes:
      - name: mysql-config
        configMap:
          name: mysql-config
      - name: mysql-data
        persistentVolumeClaim:
          claimName: mysql-master-pvc

---
# MySQL 从库
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql-slave
  labels:
    app: mysql
    role: slave
spec:
  replicas: 2
  selector:
    matchLabels:
      app: mysql
      role: slave
  template:
    metadata:
      labels:
        app: mysql
        role: slave
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        ports:
        - containerPort: 3306
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: password
        volumeMounts:
        - name: mysql-config
          mountPath: /etc/mysql/conf.d/
        - name: mysql-data
          mountPath: /var/lib/mysql
        resources:
          limits:
            memory: 4Gi
            cpu: 2000m
          requests:
            memory: 2Gi
            cpu: 1000m
      volumes:
      - name: mysql-config
        configMap:
          name: mysql-config
      - name: mysql-data
        persistentVolumeClaim:
          claimName: mysql-slave-pvc

---
# MySQL 服务
apiVersion: v1
kind: Service
metadata:
  name: mysql-master
  labels:
    app: mysql
    role: master
spec:
  type: ClusterIP
  ports:
  - port: 3306
    targetPort: 3306
  selector:
    app: mysql
    role: master

---
apiVersion: v1
kind: Service
metadata:
  name: mysql-slave
  labels:
    app: mysql
    role: slave
spec:
  type: LoadBalancer
  ports:
  - port: 3306
    targetPort: 3306
  selector:
    app: mysql
    role: slave

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-master-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
  storageClassName: fast-ssd

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-slave-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
  storageClassName: fast-ssd
```

### 2. 数据库主从复制脚本

```bash
#!/bin/bash
# setup-mysql-replication.sh

# 在主库上创建复制用户
mysql -u root -p'password' << EOF
CREATE USER 'replicator'@'%' IDENTIFIED BY 'replicator_password';
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
FLUSH PRIVILEGES;
SHOW MASTER STATUS;
EOF

# 在从库上配置复制
mysql -u root -p'password' << EOF
STOP SLAVE;
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_USER='replicator',
    MASTER_PASSWORD='replicator_password',
    MASTER_LOG_FILE='mysql-bin.000001',
    MASTER_LOG_POS=154;
START SLAVE;
SHOW SLAVE STATUS\G;
EOF

echo "MySQL 主从复制配置完成!"
```

### 3. PostgreSQL 高可用配置

```yaml
# postgresql-ha.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgresql-config
data:
  postgresql.conf: |
    listen_addresses = '*'
    port = 5432
    max_connections = 200
    shared_buffers = 256MB
    effective_cache_size = 1GB
    work_mem = 4MB
    maintenance_work_mem = 64MB
    wal_level = replica
    max_wal_senders = 3
    wal_keep_size = 16
    hot_standby = on
    hot_standby_feedback = on

  pg_hba.conf: |
    host all all 0.0.0.0/0 md5
    host replication all 0.0.0.0/0 md5

---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql
spec:
  serviceName: postgresql
  replicas: 3
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
    spec:
      containers:
      - name: postgresql
        image: postgres:15
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: password
        - name: POSTGRES_DB
          value: "basebackend"
        - name: POSTGRES_USER
          value: "postgres"
        volumeMounts:
        - name: postgresql-data
          mountPath: /var/lib/postgresql/data
        - name: postgresql-config
          mountPath: /etc/postgresql
        resources:
          limits:
            memory: 2Gi
            cpu: 1000m
          requests:
            memory: 1Gi
            cpu: 500m
      volumes:
      - name: postgresql-config
        configMap:
          name: postgresql-config
  volumeClaimTemplates:
  - metadata:
      name: postgresql-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 50Gi
      storageClassName: fast-ssd

---
apiVersion: v1
kind: Service
metadata:
  name: postgresql
spec:
  clusterIP: None
  selector:
    app: postgresql
  ports:
  - port: 5432
    targetPort: 5432

---
apiVersion: v1
kind: Service
metadata:
  name: postgresql-read
spec:
  selector:
    app: postgresql
  ports:
  - port: 5432
    targetPort: 5432
```

### 4. Redis 集群配置

```yaml
# redis-cluster.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
data:
  redis.conf: |
    bind 0.0.0.0
    port 6379
    protected-mode no
    cluster-enabled yes
    cluster-config-file nodes.conf
    cluster-node-timeout 5000
    cluster-announce-ip redis-cluster
    cluster-announce-port 6379
    cluster-announce-bus-port 16379
    appendonly yes
    maxmemory 1gb
    maxmemory-policy allkeys-lru

---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis-cluster
spec:
  serviceName: redis-cluster
  replicas: 6
  selector:
    matchLabels:
      app: redis-cluster
  template:
    metadata:
      labels:
        app: redis-cluster
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        - containerPort: 16379
        command:
        - redis-server
        - /etc/redis/redis.conf
        volumeMounts:
        - name: redis-config
          mountPath: /etc/redis
        - name: redis-data
          mountPath: /data
        resources:
          limits:
            memory: 2Gi
            cpu: 1000m
          requests:
            memory: 1Gi
            cpu: 500m
      volumes:
      - name: redis-config
        configMap:
          name: redis-config
  volumeClaimTemplates:
  - metadata:
      name: redis-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 10Gi
      storageClassName: fast-ssd

---
apiVersion: v1
kind: Service
metadata:
  name: redis-cluster
spec:
  clusterIP: None
  selector:
    app: redis-cluster
  ports:
  - port: 6379
    targetPort: 6379
  - port: 16379
    targetPort: 16379
```

### 5. 数据源配置

```java
/**
 * 云数据库数据源配置
 */
@Configuration
public class CloudDatabaseConfig {

    @Value("${cloud.database.type}")
    private String databaseType;

    @Value("${cloud.database.primary.url}")
    private String primaryUrl;

    @Value("${cloud.database.primary.username}")
    private String primaryUsername;

    @Value("${cloud.database.primary.password}")
    private String primaryPassword;

    @Value("${cloud.database.secondary.url}")
    private String secondaryUrl;

    @Value("${cloud.database.secondary.username}")
    private String secondaryUsername;

    @Value("${cloud.database.secondary.password}")
    private String secondaryPassword;

    /**
     * 主数据源
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
            .url(primaryUrl)
            .username(primaryUsername)
            .password(primaryPassword)
            .build();
    }

    /**
     * 从数据源
     */
    @Bean
    @ConfigurationProperties("spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create()
            .url(secondaryUrl)
            .username(secondaryUsername)
            .password(secondaryPassword)
            .build();
    }

    /**
     * 动态数据源
     */
    @Bean
    public DataSource dynamicDataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("primary", primaryDataSource());
        dataSourceMap.put("secondary", secondaryDataSource());
        dynamicDataSource.setTargetDataSources(dataSourceMap);
        dynamicDataSource.setDefaultTargetDataSource(primaryDataSource());
        return dynamicDataSource;
    }
}
```

---

## 📊 备份与恢复

### 1. 备份策略脚本

```bash
#!/bin/bash
# backup-strategy.sh

set -e

BACKUP_DIR="/backup"
S3_BUCKET="basebackend-backups"
DATE=$(date +%Y%m%d_%H%M%S)

# MySQL 备份
backup_mysql() {
    echo "开始备份 MySQL 数据库..."

    # 全量备份
    mysqldump --single-transaction --routines --triggers \
        --all-databases > $BACKUP_DIR/mysql_full_$DATE.sql

    # 上传到 S3
    aws s3 cp $BACKUP_DIR/mysql_full_$DATE.sql \
        s3://$S3_BUCKET/mysql/

    # 清理本地文件
    rm -f $BACKUP_DIR/mysql_full_*.sql

    echo "MySQL 备份完成"
}

# PostgreSQL 备份
backup_postgresql() {
    echo "开始备份 PostgreSQL 数据库..."

    pg_basebackup -D $BACKUP_DIR/pg_backup_$DATE -Ft -z -P

    # 上传到 S3
    aws s3 cp $BACKUP_DIR/pg_backup_$DATE.tar.gz \
        s3://$S3_BUCKET/postgresql/

    # 清理本地文件
    rm -rf $BACKUP_DIR/pg_backup_*

    echo "PostgreSQL 备份完成"
}

# MinIO 备份
backup_minio() {
    echo "开始备份 MinIO 数据..."

    # 列出所有 bucket
    buckets=$(mc ls basebackend)

    for bucket in $buckets; do
        echo "备份 bucket: $bucket"
        mc mirror basebackend/$bucket \
            s3/$S3_BUCKET/minio/$bucket/
    done

    echo "MinIO 备份完成"
}

# CephFS 备份
backup_cephfs() {
    echo "开始备份 CephFS 数据..."

    # 使用 rsync 备份数据
    rsync -avz --progress \
        cephfs-mount/ \
        $BACKUP_DIR/cephfs_$DATE/

    # 上传到 S3
    tar -czf $BACKUP_DIR/cephfs_$DATE.tar.gz \
        $BACKUP_DIR/cephfs_$DATE/
    aws s3 cp $BACKUP_DIR/cephfs_$DATE.tar.gz \
        s3://$S3_BUCKET/cephfs/

    # 清理本地文件
    rm -rf $BACKUP_DIR/cephfs_*

    echo "CephFS 备份完成"
}

# 执行所有备份
backup_mysql
backup_postgresql
backup_minio
backup_cephfs

# 设置生命周期策略（7天后转冷存储，30天后删除）
aws s3api put-bucket-lifecycle-configuration \
    --bucket $S3_BUCKET \
    --lifecycle-configuration file://lifecycle-policy.json

echo "所有备份任务完成!"
```

### 2. 恢复脚本

```bash
#!/bin/bash
# restore.sh

set -e

S3_BUCKET="basebackend-backups"
BACKUP_FILE="$1"

if [ -z "$BACKUP_FILE" ]; then
    echo "用法: $0 <backup_file>"
    echo "示例: $0 mysql_full_20241201_120000.sql"
    exit 1
fi

# 从 S3 下载备份文件
echo "下载备份文件: $BACKUP_FILE"
aws s3 cp s3://$S3_BUCKET/mysql/$BACKUP_FILE /tmp/

# 恢复 MySQL 数据库
restore_mysql() {
    echo "恢复 MySQL 数据库..."
    mysql < /tmp/$BACKUP_FILE
    echo "MySQL 数据库恢复完成"
}

# 选择恢复选项
echo "选择恢复选项:"
echo "1) MySQL"
echo "2) PostgreSQL"
read -p "请选择 (1-2): " choice

case $choice in
    1)
        restore_mysql
        ;;
    2)
        echo "PostgreSQL 恢复功能待实现"
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac

# 清理临时文件
rm -f /tmp/$BACKUP_FILE

echo "数据恢复完成!"
```

---

## 🔍 监控与告警

### 1. 存储监控配置

```yaml
# storage-monitoring.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: storage-prometheus-rules
data:
  rules.yml: |
    groups:
    - name: storage.rules
      rules:
      # 磁盘使用率告警
      - alert: DiskUsageHigh
        expr: (node_filesystem_size_bytes - node_filesystem_avail_bytes) / node_filesystem_size_bytes * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "磁盘使用率过高"
          description: "{{ $labels.instance }} 磁盘使用率为 {{ $value }}%"

      # MySQL 连接数告警
      - alert: MySQLConnectionsHigh
        expr: mysql_global_status_threads_connected / mysql_global_variables_max_connections * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MySQL 连接数过高"
          description: "MySQL 当前连接数: {{ $value }}%"

      # Redis 内存使用率告警
      - alert: RedisMemoryHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis 内存使用率过高"
          description: "Redis 内存使用率: {{ $value }}%"

      # PostgreSQL 慢查询告警
      - alert: PostgreSQLSlowQueries
        expr: pg_stat_database_blks_hit / (pg_stat_database_blks_hit + pg_stat_database_blks_read) * 100 < 99
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 缓存命中率低"
          description: "PostgreSQL 缓存命中率: {{ $value }}%"

      # Ceph 集群健康状态告警
      - alert: CephHealthError
        expr: ceph_health_status == 2
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "Ceph 集群健康状态异常"
          description: "Ceph 集群当前处于错误状态"
```

### 2. Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "存储监控仪表盘",
    "panels": [
      {
        "title": "磁盘使用情况",
        "type": "graph",
        "targets": [
          {
            "expr": "node_filesystem_size_bytes - node_filesystem_avail_bytes",
            "legendFormat": "{{ instance }} - {{ mountpoint }}"
          }
        ]
      },
      {
        "title": "MySQL 连接数",
        "type": "singlestat",
        "targets": [
          {
            "expr": "mysql_global_status_threads_connected",
            "legendFormat": "当前连接数"
          }
        ]
      },
      {
        "title": "Redis 内存使用",
        "type": "graph",
        "targets": [
          {
            "expr": "redis_memory_used_bytes",
            "legendFormat": "{{ instance }}"
          }
        ]
      },
      {
        "title": "PostgreSQL 查询性能",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(pg_stat_database_tup_fetched[5m])",
            "legendFormat": "{{ instance }} - {{ datname }}"
          }
        ]
      }
    ]
  }
}
```

---

## 🧪 测试与验证

### 1. 存储性能测试脚本

```bash
#!/bin/bash
# storage-performance-test.sh

set -e

# 测试 MinIO 性能
test_minio_performance() {
    echo "测试 MinIO 性能..."

    # 测试写入性能
    dd if=/dev/zero of=/tmp/test_file bs=1M count=100
    time mc cp /tmp/test_file basebackend/test/performance.txt

    # 测试读取性能
    time mc cp basebackend/test/performance.txt /tmp/download_test_file

    # 清理测试文件
    mc rm basebackend/test/performance.txt
    rm -f /tmp/test_file /tmp/download_test_file

    echo "MinIO 性能测试完成"
}

# 测试 MySQL 性能
test_mysql_performance() {
    echo "测试 MySQL 性能..."

    # 创建测试表
    mysql -u root -p'password' << EOF
    CREATE DATABASE IF NOT EXISTS test_db;
    USE test_db;
    CREATE TABLE IF NOT EXISTS performance_test (
        id INT AUTO_INCREMENT PRIMARY KEY,
        data VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
EOF

    # 批量插入测试
    start_time=$(date +%s)
    for i in {1..1000}; do
        mysql -u root -p'password' test_db \
            "INSERT INTO performance_test (data) VALUES ('test data $i');"
    done
    end_time=$(date +%s)
    insert_time=$((end_time - start_time))

    # 查询性能测试
    start_time=$(date +%s)
    mysql -u root -p'password' test_db \
        "SELECT COUNT(*) FROM performance_test;" > /dev/null
    end_time=$(date +%s)
    query_time=$((end_time - start_time))

    echo "MySQL 批量插入时间: ${insert_time}s"
    echo "MySQL 查询时间: ${query_time}s"

    # 清理测试数据
    mysql -u root -p'password' test_db "DROP TABLE IF EXISTS performance_test;"

    echo "MySQL 性能测试完成"
}

# 执行所有测试
test_minio_performance
test_mysql_performance

echo "存储性能测试全部完成!"
```

---

## 📚 参考资料

1. [MinIO 官方文档](https://min.io/docs/)
2. [Ceph 官方文档](https://docs.ceph.com/)
3. [Kubernetes 存储文档](https://kubernetes.io/docs/concepts/storage/)
4. [AWS RDS 最佳实践](https://aws.amazon.com/rds/)

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ 云原生存储即将完成！** ฅ'ω'ฅ
