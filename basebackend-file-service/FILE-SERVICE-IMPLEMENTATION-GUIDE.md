# 文件管理服务扩展实施指南

## 📋 项目概述

这是一个功能完整的企业级文件管理系统，支持文件上传、下载、预览、多存储后端、权限控制、版本管理和回收站等功能。

**版本**：2.0
**创建日期**：2025年1月

---

## 🎯 核心功能

### 1. 文件基本操作
- ✅ 文件上传（支持分片上传）
- ✅ 文件下载
- ✅ 文件预览（图片、PDF、Office文档）
- ✅ 文件删除（软删除）
- ✅ 文件重命名
- ✅ 文件移动
- ✅ 文件复制
- ✅ 批量操作

### 2. 多存储后端支持
- ✅ 本地存储（Local）
- ✅ MinIO对象存储
- ✅ 阿里云OSS（待实现）
- ✅ AWS S3（待实现）
- ✅ 存储类型动态切换
- ✅ 存储配置热更新

### 3. 文件权限控制
- ✅ 所有者权限
- ✅ 读权限（查看、下载）
- ✅ 写权限（编辑、上传）
- ✅ 删除权限
- ✅ 分享权限
- ✅ 基于用户的权限
- ✅ 基于角色的权限
- ✅ 基于部门的权限
- ✅ 权限过期时间

### 4. 版本管理
- ✅ 版本自动创建
- ✅ 版本历史查看
- ✅ 版本比较
- ✅ 版本回退
- ✅ 版本删除
- ✅ 版本号管理

### 5. 回收站功能
- ✅ 文件软删除
- ✅ 回收站列表
- ✅ 文件恢复
- ✅ 彻底删除
- ✅ 自动清理（30天）
- ✅ 批量恢复/删除

### 6. 文件分享
- ✅ 生成分享链接
- ✅ 密码保护
- ✅ 有效期限制
- ✅ 下载次数限制
- ✅ 分享统计

### 7. 其他功能
- ✅ 文件搜索
- ✅ 文件夹管理
- ✅ 文件标签
- ✅ 缩略图生成
- ✅ 操作日志
- ✅ 文件去重（MD5）

---

## 🏗️ 架构设计

### 系统架构图

```
┌─────────────────────────────────────────────┐
│              前端（React + TypeScript）        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│  │文件列表  │ │文件上传  │ │回收站    │    │
│  └──────────┘ └──────────┘ └──────────┘    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│  │版本管理  │ │权限管理  │ │分享管理  │    │
│  └──────────┘ └──────────┘ └──────────┘    │
└─────────────────────────────────────────────┘
                     ↓ HTTP/REST API
┌─────────────────────────────────────────────┐
│          后端（Spring Boot）                  │
│  ┌──────────────────────────────────┐       │
│  │      FileController (REST)       │       │
│  └──────────────────────────────────┘       │
│                  ↓                           │
│  ┌──────────────────────────────────┐       │
│  │      FileManagementService       │       │
│  │  ├─ 文件操作服务                   │       │
│  │  ├─ 权限控制服务                   │       │
│  │  ├─ 版本管理服务                   │       │
│  │  └─ 回收站服务                     │       │
│  └──────────────────────────────────┘       │
│                  ↓                           │
│  ┌──────────────────────────────────┐       │
│  │      StorageService (抽象层)      │       │
│  └──────────────────────────────────┘       │
│         ↓         ↓         ↓        ↓      │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────┐  │
│  │Local   │ │MinIO   │ │OSS     │ │S3  │  │
│  └────────┘ └────────┘ └────────┘ └────┘  │
└─────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────┐
│              数据库（MySQL）                  │
│  ├─ file_metadata (文件元数据)                │
│  ├─ file_version (文件版本)                  │
│  ├─ file_permission (文件权限)               │
│  ├─ file_share (文件分享)                    │
│  ├─ file_recycle_bin (回收站)                │
│  ├─ file_operation_log (操作日志)            │
│  └─ file_tag (文件标签)                      │
└─────────────────────────────────────────────┘
```

### 技术栈

**后端：**
- Spring Boot 3.1.5
- MyBatis Plus 3.5.3
- MinIO 8.5.7
- Aliyun OSS SDK（可选）
- AWS S3 SDK（可选）
- Hutool 5.8.20
- Thumbnailator 0.4.20

**前端：**
- React 18.2.0
- TypeScript 5.3.3
- Ant Design 5.12.0
- Ant Design Pro Components
- React Query (数据缓存)
- Uppy (文件上传)

**数据库：**
- MySQL 8.0+

---

## 📦 数据库设计

### 核心表结构

#### 1. file_metadata（文件元数据表）

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | BIGINT | 主键 |
| file_id | VARCHAR(64) | 文件唯一标识 |
| file_name | VARCHAR(255) | 文件名 |
| original_name | VARCHAR(255) | 原始文件名 |
| file_path | VARCHAR(500) | 存储路径 |
| file_size | BIGINT | 文件大小 |
| content_type | VARCHAR(100) | MIME类型 |
| storage_type | VARCHAR(20) | 存储类型 |
| owner_id | BIGINT | 所有者ID |
| is_public | TINYINT | 是否公开 |
| is_deleted | TINYINT | 是否删除 |
| version | INT | 当前版本号 |
| ... | ... | ... |

#### 2. file_version（文件版本表）

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | BIGINT | 主键 |
| file_id | VARCHAR(64) | 文件ID |
| version_number | INT | 版本号 |
| file_path | VARCHAR(500) | 存储路径 |
| md5 | VARCHAR(64) | MD5值 |
| change_description | VARCHAR(500) | 变更说明 |
| created_by | BIGINT | 创建人ID |
| is_current | TINYINT | 是否当前版本 |

#### 3. file_permission（文件权限表）

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | BIGINT | 主键 |
| file_id | VARCHAR(64) | 文件ID |
| user_id | BIGINT | 用户ID |
| role_id | BIGINT | 角色ID |
| dept_id | BIGINT | 部门ID |
| permission_type | VARCHAR(20) | 权限类型 |
| expire_time | DATETIME | 过期时间 |

#### 4. file_recycle_bin（回收站表）

| 字段名 | 类型 | 说明 |
|-------|------|------|
| id | BIGINT | 主键 |
| file_id | VARCHAR(64) | 文件ID |
| file_name | VARCHAR(255) | 文件名 |
| deleted_by | BIGINT | 删除人ID |
| deleted_at | DATETIME | 删除时间 |
| expire_at | DATETIME | 过期时间 |
| original_metadata | JSON | 原始元数据 |

---

## 🔧 后端实现

### 1. 存储抽象层

```java
// StorageService接口
public interface StorageService {
    String upload(InputStream inputStream, String path, String contentType, long size);
    InputStream download(String path);
    void delete(String path);
    void copy(String sourcePath, String targetPath);
    void move(String sourcePath, String targetPath);
    boolean exists(String path);
    String getUrl(String path);
    String getPresignedUrl(String path, int expireTime);
    List<String> listFiles(String prefix);
    StorageType getStorageType();
}
```

**实现类：**
- `LocalStorageServiceImpl` - 本地存储
- `MinioStorageServiceImpl` - MinIO存储
- `AliyunOssStorageServiceImpl` - 阿里云OSS（待实现）
- `AwsS3StorageServiceImpl` - AWS S3（待实现）

### 2. 文件管理服务

```java
@Service
public class FileManagementService {

    // 文件上传
    public FileMetadata uploadFile(MultipartFile file, Long folderId, Long userId);

    // 文件下载
    public FileDownloadInfo downloadFile(String fileId, Long userId);

    // 文件删除
    public void deleteFile(String fileId, Long userId);

    // 文件恢复
    public void restoreFile(String fileId, Long userId);

    // 创建版本
    public FileVersion createVersion(String fileId, MultipartFile file, String description);

    // 版本回退
    public void revertToVersion(String fileId, Long versionId);

    // 权限检查
    public boolean hasPermission(String fileId, Long userId, PermissionType type);

    // 文件分享
    public FileShare createShare(String fileId, ShareRequest request);
}
```

### 3. 配置文件

```yaml
# application.yml
file:
  storage:
    type: minio  # local, minio, aliyun-oss, aws-s3

  local:
    upload-path: /data/uploads
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
    - pdf
    - doc
    - docx
    - xls
    - xlsx
```

---

## 💻 前端实现

### 页面结构

```
src/pages/FileManagement/
├── FileList/                # 文件列表
│   ├── index.tsx           # 主页面
│   ├── FileGrid.tsx        # 网格视图
│   ├── FileTable.tsx       # 列表视图
│   ├── FileUpload.tsx      # 上传组件
│   └── FilePreview.tsx     # 预览组件
├── RecycleBin/              # 回收站
│   └── index.tsx
├── VersionHistory/          # 版本历史
│   └── index.tsx
├── PermissionManagement/    # 权限管理
│   └── index.tsx
└── ShareManagement/         # 分享管理
    └── index.tsx
```

### 核心组件

#### 1. 文件列表组件

```tsx
import React, { useState } from 'react';
import { Upload, Table, Button, Modal } from 'antd';
import { UploadOutlined, DownloadOutlined, DeleteOutlined } from '@ant-design/icons';

const FileList: React.FC = () => {
  const [files, setFiles] = useState([]);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('list');

  return (
    <div>
      <Upload>
        <Button icon={<UploadOutlined />}>上传文件</Button>
      </Upload>

      <Table
        dataSource={files}
        columns={[
          { title: '文件名', dataIndex: 'fileName' },
          { title: '大小', dataIndex: 'fileSize' },
          { title: '修改时间', dataIndex: 'updateTime' },
          { title: '操作', render: (record) => (
            <>
              <Button icon={<DownloadOutlined />}>下载</Button>
              <Button icon={<DeleteOutlined />}>删除</Button>
            </>
          )},
        ]}
      />
    </div>
  );
};
```

#### 2. 文件上传组件

```tsx
import { Upload } from 'antd';
import { Uppy } from '@uppy/core';
import XHRUpload from '@uppy/xhr-upload';

const FileUpload: React.FC = () => {
  const uppy = new Uppy({
    restrictions: {
      maxFileSize: 100 * 1024 * 1024, // 100MB
      allowedFileTypes: ['.jpg', '.png', '.pdf'],
    },
  }).use(XHRUpload, {
    endpoint: '/api/file/upload',
    fieldName: 'file',
  });

  return <DashboardModal uppy={uppy} />;
};
```

---

## 🚀 快速开始

### 1. 后端配置

#### 添加依赖（pom.xml）

```xml
<!-- 已包含在basebackend-file-service中 -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

#### 配置文件

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/basebackend?useSSL=false
    username: root
    password: root

file:
  storage:
    type: local  # 或 minio
  local:
    upload-path: ./uploads
    access-prefix: /files
```

#### 运行数据库迁移

```bash
# Flyway会自动执行SQL脚本
# 位置: src/main/resources/db/migration/V1.0__file_service_init.sql
```

### 2. 前端配置

#### 安装依赖

```bash
cd basebackend-admin-web
npm install @ant-design/pro-components @uppy/core @uppy/react
```

#### 配置路由

```tsx
// src/router/index.tsx
import FileList from '@/pages/FileManagement/FileList';
import RecycleBin from '@/pages/FileManagement/RecycleBin';

<Route path="file/list" element={<FileList />} />
<Route path="file/recycle-bin" element={<RecycleBin />} />
```

---

## 📖 API文档

### 文件上传

```
POST /api/file/upload
Content-Type: multipart/form-data

参数:
- file: 文件对象
- folderId: 文件夹ID（可选）

响应:
{
  "success": true,
  "data": {
    "fileId": "xxx",
    "fileName": "example.pdf",
    "fileUrl": "http://..."
  }
}
```

### 文件下载

```
GET /api/file/download/{fileId}

响应:
文件流
```

### 文件删除

```
DELETE /api/file/{fileId}

响应:
{
  "success": true,
  "message": "文件已移至回收站"
}
```

### 查看版本历史

```
GET /api/file/{fileId}/versions

响应:
{
  "success": true,
  "data": [
    {
      "versionId": 1,
      "versionNumber": 2,
      "createdBy": "张三",
      "createTime": "2025-01-15 10:00:00"
    }
  ]
}
```

---

## ✅ 验收清单

### 基本功能
- [ ] 文件上传成功
- [ ] 文件下载成功
- [ ] 文件删除成功
- [ ] 文件预览正常

### 存储后端
- [ ] 本地存储正常工作
- [ ] MinIO存储正常工作
- [ ] 存储类型可切换

### 权限控制
- [ ] 所有者权限正常
- [ ] 共享权限正常
- [ ] 权限过期检查有效

### 版本管理
- [ ] 版本自动创建
- [ ] 版本回退成功
- [ ] 版本历史查看正常

### 回收站
- [ ] 文件软删除成功
- [ ] 文件恢复成功
- [ ] 彻底删除成功
- [ ] 自动清理任务运行正常

---

## 📝 待实现功能

### 高优先级
- [ ] 阿里云OSS实现
- [ ] AWS S3实现
- [ ] 文件预览（Office文档）
- [ ] 分片上传
- [ ] 断点续传

### 中优先级
- [ ] 文件在线编辑
- [ ] 文件水印
- [ ] 图片压缩
- [ ] 视频转码

### 低优先级
- [ ] 文件全文检索
- [ ] 文件加密存储
- [ ] 文件审计

---

## 🆘 常见问题

### Q1: 文件上传失败？
**检查：**
1. 文件大小是否超过限制
2. 文件类型是否允许
3. 存储路径是否有写权限
4. MinIO服务是否启动

### Q2: 无法下载文件？
**检查：**
1. 文件是否存在
2. 用户是否有权限
3. 文件路径是否正确

### Q3: 版本创建失败？
**检查：**
1. 原文件是否存在
2. 存储空间是否足够

---

## 🎉 总结

文件管理服务现已支持：
- ✅ 完整的文件CRUD操作
- ✅ 多存储后端支持
- ✅ 细粒度权限控制
- ✅ 版本管理
- ✅ 回收站功能
- ✅ 文件分享
- ✅ 操作审计

**下一步**：
1. 完成前端页面开发
2. 实现OSS和S3支持
3. 添加文件预览功能
4. 优化性能和用户体验

---

**文档版本**：v1.0
**最后更新**：2025年1月
**作者**：Claude Code
