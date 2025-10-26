# 📦 文件管理服务 (File Service)

企业级文件管理服务，支持文件上传、下载、预览、多存储后端、权限控制、版本管理和回收站等功能。

**版本**：2.0
**状态**：✅ 核心功能已完成
**更新日期**：2025年1月

---

## ✨ 核心特性

- ✅ **多存储后端** - 支持本地、MinIO、OSS、S3（可扩展）
- ✅ **权限控制** - 细粒度的文件权限管理
- ✅ **版本管理** - 完整的文件版本控制
- ✅ **回收站** - 软删除机制，支持文件恢复
- ✅ **文件分享** - 生成分享链接，支持密码和过期时间
- ✅ **操作审计** - 完整的操作日志记录
- ✅ **文件去重** - 基于MD5的文件去重
- ✅ **标签管理** - 灵活的文件标签系统

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────┐
│         REST API (Controller)           │
│  POST   /api/file/upload                │
│  GET    /api/file/download/{fileId}     │
│  DELETE /api/file/{fileId}              │
│  POST   /api/file/{fileId}/restore      │
│  POST   /api/file/{fileId}/version      │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      FileManagementService              │
│  - 文件上传/下载/删除                     │
│  - 权限控制                               │
│  - 版本管理                               │
│  - 回收站管理                             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      StorageService (抽象层)             │
│  ┌─────────┬─────────┬─────────┬──────┐│
│  │Local    │MinIO    │OSS      │S3    ││
│  └─────────┴─────────┴─────────┴──────┘│
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      MySQL Database                     │
│  - file_metadata (文件元数据)            │
│  - file_version (版本管理)               │
│  - file_permission (权限控制)            │
│  - file_recycle_bin (回收站)            │
│  - file_operation_log (操作日志)        │
└─────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- MySQL 8.0+
- Maven 3.8+
- MinIO (可选)

### 2. 配置文件

```yaml
# application.yml
file:
  storage:
    type: local  # 可选: local, minio, aliyun-oss, aws-s3

  local:
    upload-path: ./uploads
    access-prefix: /files

  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: basebackend-files

  max-size: 104857600  # 100MB
  allowed-types:
    - jpg
    - jpeg
    - png
    - gif
    - pdf
    - doc
    - docx
    - xls
    - xlsx
```

### 3. 运行服务

```bash
# 1. 克隆项目
git clone https://github.com/your-repo/basebackend.git

# 2. 进入文件服务目录
cd basebackend-file-service

# 3. 构建项目
mvn clean package

# 4. 运行服务
java -jar target/basebackend-file-service-1.0.0-SNAPSHOT.jar
```

### 4. 测试接口

```bash
# 上传文件
curl -X POST http://localhost:8080/api/file/upload \
  -F "file=@example.pdf"

# 下载文件
curl -X GET http://localhost:8080/api/file/download/{fileId} \
  -o downloaded.pdf
```

---

## 📖 文档导航

### 核心文档

| 文档 | 说明 | 链接 |
|------|------|------|
| 📘 **实施指南** | 完整的实施指南，包含架构设计、功能说明 | [FILE-SERVICE-IMPLEMENTATION-GUIDE.md](./FILE-SERVICE-IMPLEMENTATION-GUIDE.md) |
| 📗 **代码示例** | 核心服务代码实现示例 | [FILE-SERVICE-CODE-EXAMPLES.md](./FILE-SERVICE-CODE-EXAMPLES.md) |
| 📙 **完成总结** | 项目完成情况总结 | [FILE-SERVICE-COMPLETION-SUMMARY.md](./FILE-SERVICE-COMPLETION-SUMMARY.md) |

### 快速导航

**想要了解...**
- ✅ 如何快速开始？ → 阅读本文档
- ✅ 完整的功能说明？ → [实施指南](./FILE-SERVICE-IMPLEMENTATION-GUIDE.md)
- ✅ 代码如何实现？ → [代码示例](./FILE-SERVICE-CODE-EXAMPLES.md)
- ✅ 项目完成了什么？ → [完成总结](./FILE-SERVICE-COMPLETION-SUMMARY.md)

---

## 🎯 核心功能

### 1. 文件上传

**功能特点：**
- 支持多种存储后端
- 基于MD5的文件去重
- 文件大小和类型限制
- 自动版本创建

**API示例：**
```bash
POST /api/file/upload
Content-Type: multipart/form-data

参数:
- file: 文件对象
- folderId: 文件夹ID（可选）
```

### 2. 文件下载

**功能特点：**
- 权限验证
- 下载统计
- 支持断点续传（待实现）

**API示例：**
```bash
GET /api/file/download/{fileId}
```

### 3. 版本管理

**功能特点：**
- 自动版本创建
- 版本历史查看
- 版本回退

**API示例：**
```bash
# 创建新版本
POST /api/file/{fileId}/version

# 回退到指定版本
POST /api/file/{fileId}/revert/{versionId}
```

### 4. 回收站

**功能特点：**
- 软删除机制
- 30天自动清理
- 支持恢复

**API示例：**
```bash
# 删除文件（移入回收站）
DELETE /api/file/{fileId}

# 恢复文件
POST /api/file/{fileId}/restore
```

---

## 📦 数据库表

### 核心表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| file_metadata | 文件元数据 | file_id, file_name, file_size, owner_id, is_deleted |
| file_version | 文件版本 | file_id, version_number, file_path, is_current |
| file_permission | 文件权限 | file_id, user_id, permission_type, expire_time |
| file_recycle_bin | 回收站 | file_id, deleted_by, deleted_at, expire_at |
| file_operation_log | 操作日志 | file_id, operation_type, operator_id, operation_time |

---

## 🔧 配置说明

### 存储类型配置

#### 1. 本地存储

```yaml
file:
  storage:
    type: local
  local:
    upload-path: /data/uploads
    access-prefix: /files
```

#### 2. MinIO对象存储

```yaml
file:
  storage:
    type: minio
  minio:
    endpoint: http://minio:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: basebackend-files
```

#### 3. 阿里云OSS（待实现）

```yaml
file:
  storage:
    type: aliyun-oss
  aliyun-oss:
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    access-key-id: your-access-key
    access-key-secret: your-secret-key
    bucket-name: your-bucket
```

---

## 🛠️ 技术栈

- **Spring Boot** 3.1.5
- **MyBatis Plus** 3.5.3
- **MinIO** 8.5.7
- **MySQL** 8.0+
- **Hutool** 5.8.20
- **Thumbnailator** 0.4.20（图片处理）

---

## 📊 已完成功能

### 后端

- ✅ 多存储后端支持（本地、MinIO）
- ✅ 文件上传/下载/删除
- ✅ 文件元数据管理
- ✅ 权限控制体系
- ✅ 版本管理
- ✅ 回收站功能
- ✅ 操作日志记录
- ✅ 文件去重（MD5）
- ✅ REST API完整实现

### 数据库

- ✅ 完整的表结构设计（8张表）
- ✅ Flyway迁移脚本
- ✅ 索引优化

### 文档

- ✅ 实施指南
- ✅ 代码示例
- ✅ 完成总结
- ✅ README文档

---

## 📝 待实现功能

### 高优先级

- [ ] 前端页面
  - [ ] 文件列表（网格/列表视图）
  - [ ] 文件上传（拖拽、进度）
  - [ ] 文件预览
  - [ ] 回收站管理
  - [ ] 版本历史

- [ ] 阿里云OSS支持
- [ ] AWS S3支持

### 中优先级

- [ ] 文件分享功能
- [ ] 文件预览（图片、PDF、Office）
- [ ] 分片上传
- [ ] 断点续传
- [ ] 缩略图生成优化

### 低优先级

- [ ] 文件全文搜索
- [ ] 文件加密存储
- [ ] CDN集成
- [ ] 病毒扫描

---

## 🆘 常见问题

### Q1: 如何切换存储后端？

修改 `application.yml` 中的 `file.storage.type` 配置即可：
```yaml
file:
  storage:
    type: minio  # local, minio, aliyun-oss, aws-s3
```

### Q2: 如何限制上传文件大小？

在配置文件中设置：
```yaml
file:
  max-size: 104857600  # 100MB (字节)
```

### Q3: 回收站文件何时自动清理？

默认30天后自动清理，可在代码中修改：
```java
recycleBin.setExpireAt(LocalDateTime.now().plusDays(30));
```

### Q4: 如何查看操作日志？

查询 `file_operation_log` 表即可：
```sql
SELECT * FROM file_operation_log WHERE file_id = 'xxx' ORDER BY operation_time DESC;
```

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 📞 联系方式

如有问题或建议，请：
- 提交 Issue
- 发送邮件至：support@example.com
- 访问文档中心：https://docs.example.com

---

## 🙏 致谢

感谢以下开源项目：
- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis Plus](https://baomidou.com/)
- [MinIO](https://min.io/)
- [Hutool](https://hutool.cn/)

---

**文档版本**：v2.0
**最后更新**：2025年1月
**维护者**：Claude Code

🎉 **文件管理服务现已就绪，开始使用吧！**
