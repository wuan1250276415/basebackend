# Phase 1 实际待删除文件清单（2026-03-21）

> 本清单只列出建议在 Phase 1 中从 Git 取消跟踪或移除的污染项，不包含业务源码修改。

---

## 1. 目录级删除项

### 1.1 basebackend-chat-ui/node_modules

- 跟踪文件数：20163
- 建议动作：整体取消跟踪该目录
- 说明：依赖目录，不应入库
- 示例文件（前 40 条）：
  - basebackend-chat-ui/node_modules/.bin/baseline-browser-mapping
  - basebackend-chat-ui/node_modules/.bin/browserslist
  - basebackend-chat-ui/node_modules/.bin/esbuild
  - basebackend-chat-ui/node_modules/.bin/jsesc
  - basebackend-chat-ui/node_modules/.bin/json5
  - basebackend-chat-ui/node_modules/.bin/loose-envify
  - basebackend-chat-ui/node_modules/.bin/nanoid
  - basebackend-chat-ui/node_modules/.bin/parser
  - basebackend-chat-ui/node_modules/.bin/rollup
  - basebackend-chat-ui/node_modules/.bin/semver
  - basebackend-chat-ui/node_modules/.bin/tsc
  - basebackend-chat-ui/node_modules/.bin/tsserver
  - basebackend-chat-ui/node_modules/.bin/update-browserslist-db
  - basebackend-chat-ui/node_modules/.bin/vite
  - basebackend-chat-ui/node_modules/.package-lock.json
  - basebackend-chat-ui/node_modules/.vite/deps/@ant-design_icons.js
  - basebackend-chat-ui/node_modules/.vite/deps/@ant-design_icons.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/_metadata.json
  - basebackend-chat-ui/node_modules/.vite/deps/antd.js
  - basebackend-chat-ui/node_modules/.vite/deps/antd.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/antd_locale_zh_CN.js
  - basebackend-chat-ui/node_modules/.vite/deps/antd_locale_zh_CN.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/axios.js
  - basebackend-chat-ui/node_modules/.vite/deps/axios.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-6PXSGDAH.js
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-6PXSGDAH.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-DRWLMN53.js
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-DRWLMN53.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-FTDBO4JW.js
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-FTDBO4JW.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-G3PMV62Z.js
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-G3PMV62Z.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-NXESFFTV.js
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-NXESFFTV.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-ZDBJOGHJ.js
  - basebackend-chat-ui/node_modules/.vite/deps/chunk-ZDBJOGHJ.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/dayjs.js
  - basebackend-chat-ui/node_modules/.vite/deps/dayjs.js.map
  - basebackend-chat-ui/node_modules/.vite/deps/package.json
  - basebackend-chat-ui/node_modules/.vite/deps/react-dom.js

### 1.2 basebackend-chat-ui/dist

- 跟踪文件数：3
- 建议动作：整体取消跟踪该目录
- 示例文件：
  - basebackend-chat-ui/dist/assets/index-C7WMbKak.js
  - basebackend-chat-ui/dist/assets/index-CqmnqF7F.css
  - basebackend-chat-ui/dist/index.html

---

## 2. 文件类型删除项

### 2.1 所有被跟踪的 .class 文件

- 跟踪文件数：597
- 建议动作：全部取消跟踪
- 示例文件（前 120 条）：
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/annotation/DataScope.class
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/aspect/DataScopeAspect.class
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/config/DataScopeAutoConfiguration.class
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/config/DataScopeProperties.class
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/context/DataScopeContext.class
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/enums/DataScopeType.class
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/handler/DataScopeHelper.class
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/handler/DataScopeSqlBuilder.class
  - basebackend-common/basebackend-common-datascope/bin/src/main/java/com/basebackend/common/datascope/interceptor/DataScopeInterceptor.class
  - basebackend-common/basebackend-common-datascope/bin/src/test/java/com/basebackend/common/datascope/DataScopeTest.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/config/StorageAutoConfiguration.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/config/StorageProperties$ChecksumConfig.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/config/StorageProperties$Local.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/config/StorageProperties$Minio.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/config/StorageProperties$Oss.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/config/StorageProperties$S3.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/config/StorageProperties.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/exception/StorageException.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/model/Checksum$ChecksumBuilder.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/model/Checksum.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/model/StorageResult$StorageResultBuilder.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/model/StorageResult.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/model/StorageUsage$StorageUsageBuilder.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/model/StorageUsage.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/model/UploadRequest$UploadRequestBuilder.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/model/UploadRequest.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/provider/LocalStorageProvider.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/provider/MinioStorageProvider.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/provider/OssStorageProvider.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/provider/S3StorageProvider.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/reliability/ChecksumProvider.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/reliability/impl/ChecksumServiceImpl.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/spi/StorageProvider.class
  - basebackend-common/basebackend-common-storage/bin/src/main/java/com/basebackend/storage/spi/StorageType.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/AssertUtils.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/DateUtils.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/IdGenerator$SnowflakeIdWorker.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/IdGenerator.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/IpUtil.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/NacosUtils.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/SnowflakeIdGenerator.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/StringUtils.class
  - basebackend-common/basebackend-common-util/bin/src/main/java/com/basebackend/common/util/UserAgentUtil.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/IdGeneratorTest$RandomStringTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/IdGeneratorTest$SnowflakeTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/IdGeneratorTest$TimestampIdTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/IdGeneratorTest$UuidTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/IdGeneratorTest.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/SnowflakeIdGeneratorTest$ConcurrencyTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/SnowflakeIdGeneratorTest$ConstructorTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/SnowflakeIdGeneratorTest$IdGenerationTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/SnowflakeIdGeneratorTest$IdParsingTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/SnowflakeIdGeneratorTest$StaticMethodTests.class
  - basebackend-common/basebackend-common-util/bin/src/test/java/com/basebackend/common/util/SnowflakeIdGeneratorTest.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/antivirus/AntivirusService.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/antivirus/ClamAVAntivirusService.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/antivirus/MockAntivirusService.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/antivirus/ScanResult.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/audit/AuditAction.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/audit/AuditOutcome.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/audit/AuditService$DetailsBuilder.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/audit/AuditService.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/audit/FileShareAuditLog.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/chunk/ChunkUploadInfo$UploadStatus.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/chunk/ChunkUploadInfo.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/chunk/ChunkUploadService.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/AsyncConfiguration.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/FileProperties.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/FileSecurityProperties.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/FileStorageProperties$Local.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/FileStorageProperties$MinioConfig.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/FileStorageProperties$OssConfig.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/FileStorageProperties$S3Config.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/FileStorageProperties.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/MinioConfig.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/MinioProperties.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/OssProperties.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/PasswordEncoderConfig.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/RateLimiterConfig.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/config/S3Properties.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/controller/ChunkUploadController.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/controller/FileController.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/dedup/DeduplicationInfo.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/dedup/FileDeduplicationService$FileUploader.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/dedup/FileDeduplicationService.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileMetadata.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileOperationLog.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FilePermission.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileRecycleBin.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileShare.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileTag.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileTagRelation.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileUploadResult$FileUploadResultBuilder.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileUploadResult.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/entity/FileVersion.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/RateLimitPolicy$LimitType.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/RateLimitPolicy.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/RateLimiter$FailureResult.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/RateLimiter$RateLimitExceededException.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/RateLimiter$RateLimitResult.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/RateLimiter.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/RedisRateLimiter.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/SimpleRateLimiter$FixedWindowState.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/SimpleRateLimiter$PasswordFailureState.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/SimpleRateLimiter$TokenBucketState.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/limit/SimpleRateLimiter.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FileMetadataMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FileOperationLogMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FilePermissionMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FileRecycleBinMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FileShareAuditLogMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FileShareMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FileTagMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FileTagRelationMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/mapper/FileVersionMapper.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/model/FileShareRequest.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/model/FileStatistics$FileTypeDistribution.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/model/FileStatistics$StorageUsage.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/model/FileStatistics.class
  - basebackend-file-service/bin/src/main/java/com/basebackend/file/model/StorageUsageSummary.class

### 2.2 所有被跟踪的 .jar 文件

- 跟踪文件数：1
- 建议动作：逐项确认后取消跟踪；当前审查建议移除全部已发现项
  - app-extracted.jar

### 2.3 所有被跟踪的 .tsbuildinfo 文件

- 跟踪文件数：4
- 建议动作：全部取消跟踪
  - basebackend-admin-web/tsconfig.node.tsbuildinfo
  - basebackend-admin-web/tsconfig.tsbuildinfo
  - basebackend-album-ui/tsconfig.tsbuildinfo
  - basebackend-chat-ui/tsconfig.tsbuildinfo

### 2.4 所有被跟踪的 .m2 文件

- 跟踪文件数：3
- 建议动作：全部取消跟踪
  - .m2/repository/com/alibaba/cloud/spring-cloud-alibaba-dependencies/2022.0.0.0/spring-cloud-alibaba-dependencies-2022.0.0.0.pom.lastUpdated
  - .m2/repository/org/springframework/boot/spring-boot-dependencies/3.1.5/spring-boot-dependencies-3.1.5.pom.lastUpdated
  - .m2/repository/org/springframework/cloud/spring-cloud-dependencies/2022.0.4/spring-cloud-dependencies-2022.0.4.pom.lastUpdated

---

## 3. 路径级删除项

### 3.1 根目录提取产物 / 残留类文件

- app-extracted.jar
- org/springframework/security/web/servlet/util/matcher/PathPatternRequestMatcher.class

---

## 4. 删除前人工确认项

- 确认 `app-extracted.jar` 没有被当前流程当作发布产物使用。
- 确认 `basebackend-chat-ui/dist` 不是依赖提交产物部署。
- 确认根目录 `org/` 下不存在除 `.class` 外的人工维护文件。
- 本阶段不处理 `bin/` 目录，即使其中包含 `.class`，也仅从“文件类型清单”记录，不在此阶段做结构处置。

---

## 5. 建议执行命令方向（仅供审核，不在本清单中执行）

1. 先更新 `.gitignore`
2. 再对目录级污染项执行 `git rm -r --cached ...`
3. 再对文件类型污染项执行批量取消跟踪
4. 最后检查 `git status` 与 `git ls-files`
