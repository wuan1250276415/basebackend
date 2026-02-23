[根目录](../../CLAUDE.md) > **basebackend-file-service**

# basebackend-file-service

## 模块职责

文件存储微服务。基于MinIO对象存储，提供文件上传/下载/预览/版本管理/分享/回收站等功能。

## 入口与启动

- 独立微服务，含Dockerfile
- MinIO配置: `application-minio.yml`

## 对外接口

- `FileService`: 文件CRUD服务
- `DelegatingStorageService`: 存储代理(本地/MinIO可切换)
- `LocalStorageService`: 本地存储实现

## 数据模型

迁移: `V1.0__file_service_init.sql`, `V3.0__create_file_share_table.sql`, `V4.0__create_file_share_audit_log.sql`

## 测试与质量

4个测试: DelegatingStorageServiceTest, FilePropertiesTest, FileServiceTest, LocalStorageServiceTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
