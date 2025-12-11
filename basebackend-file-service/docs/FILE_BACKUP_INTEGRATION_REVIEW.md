# basebackend-file-service 与 basebackend-backup 相似性审查报告

## 概览
- **file-service**：面向业务上传/下载/分享的文件中心，覆盖分片、去重、防病毒、预览、分享、审计与权限控制，存储层支持 Local/MinIO/OSS/S3。
- **backup**：面向数据备份的独立子系统，提供 MySQL 备份、定时任务、重试/分布式锁、校验与多存储后端（Local/S3），关注可恢复性与可靠性。

## 主要相似点
- **存储适配层**：两者都有抽象接口及 Local/S3 实现。`file-service` 的 `StorageService` 与 `backup` 的 `StorageProvider`/`UploadRequest`/`StorageResult` 语义高度重合（上传、下载、删除、存在性、预签名 URL、列表/usage）。
- **S3/本地实现模式**：均通过配置化方式创建客户端，处理桶存在性/预签名 URL。S3 适配器中都有元数据设置、异常包装与自定义域名/endpoint 支持。
- **校验/完整性关注**：`backup` 侧有 `ChecksumService`、重试与分布式锁；`file-service` 侧有去重、分片校验、病毒扫描与审计。可靠性相关能力可以互补。

## 差异与不可直接合并点
- **领域职责不同**：`file-service` 面向在线文件交付与分享（延迟敏感、权限复杂、支持预览），`backup` 面向离线/批量备份（吞吐优先、调度/重试、锁与校验）。直接合并为一个模块会违反单一职责并引入不必要依赖。
- **API 形态**：`file-service` 暴露业务 API/分享链接；`backup` 主要内部任务/调度，接口更偏运维。合并会混淆安全边界与发布节奏。
- **可靠性策略**：`backup` 强调重试、校验、锁；`file-service` 当前缺少这些跨实例可靠性构件，需按业务需求择优引入，而非全量搬运。

## 可复用/整合建议
1. **提炼统一存储 SPI（建议放在 `basebackend-common` 或独立 storage 模块）**  
   - 合并 `StorageService` 与 `StorageProvider` 接口/模型，保留通用能力：上传/下载/删除/拷贝/移动/exists/URL/预签名/usage。  
   - 对齐配置与 Bean 装配，复用同一套 Local/MinIO/OSS/S3 实现，减少重复维护。
2. **共享校验与重试组件**  
   - 将 `ChecksumService`、`RetryTemplate`、`LockManager`（Redisson）下沉为可选依赖，`file-service` 的分片合并/去重可选用校验以提升一致性。
3. **统一异常与日志规范**  
   - 将存储适配器中的异常包装、敏感信息脱敏、ETag/URL 日志格式对齐，减少差异化维护。
4. **按职责保持模块边界**  
   - 继续保持备份调度/历史/恢复逻辑在 `backup`，文件分享/预览/鉴权在 `file-service`；通过共享 SPI/组件实现代码复用，而非功能合并。

## 结论
两模块在存储适配层存在明显重复，可抽象为公共存储模块共享实现；可靠性构件可作为可选增强引入到 `file-service`。但业务职责差异大，不建议整体合并为单一服务，宜通过拆分公共库 + 保持独立业务层的方式实现 DRY 与可维护性。 
