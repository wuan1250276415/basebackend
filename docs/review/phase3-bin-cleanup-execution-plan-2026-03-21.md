# Phase 3 `bin/` 清理执行计划（逐目录版，2026-03-21）

> 目标：将模块内部语义错误的 `bin/` 目录拆解为“可直接删除 / 需迁移 / 需归档”三类，确保后续清理可执行、可回滚、可审计。

---

## 1. 执行原则

1. **主模块优先**：若 `bin/` 中资源已在主模块正式位置存在，则优先保留主模块版本，删除 `bin/` 副本。
2. **迁移先于删除**：仅当 `bin/` 中内容在主模块无对应版本时，才考虑迁移。
3. **文档分流**：
   - 长期文档 → 模块根 `docs/` 或统一 `docs/reference/`
   - 阶段报告 → `docs/reports/` 或 `docs/archive/`
4. **最终语义修正**：清理完成后，`bin/` 若存在，只能用于脚本，不可再承载 `pom.xml` / `src/` / `docs/`。

---

## 2. 当前判断总览

本轮比对后，这些 `bin/` 可以分成两类：

### 2.1 可直接删除型（内容已在主模块有对应版本）

- `basebackend-jwt/bin`
- `basebackend-common/basebackend-common-datascope/bin`
- `basebackend-common/basebackend-common-storage/bin`
- `basebackend-common/basebackend-common-util/bin`
- `basebackend-security/bin`
- `basebackend-messaging/bin`
- `basebackend-nacos/bin`
- `basebackend-file-service/bin`
- `basebackend-gateway/bin`

### 2.2 需先迁移/核对再删除型

- `basebackend-observability/bin`

原因：当前主模块资源扫描结果显示，`basebackend-observability` 主模块下未直接扫到对应 `src/main/resources` 内容，而 `bin/` 中却存在配置、mapper、测试资源与文档，因此不能先斩后奏。

---

## 3. 逐目录执行建议

## 3.1 `basebackend-jwt/bin`

### 当前内容
- `pom.xml`
- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### 比对结果
主模块已存在：
- `basebackend-jwt/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### 结论
**可直接删除**。

### 动作建议
- 删除 `basebackend-jwt/bin/`

---

## 3.2 `basebackend-common/basebackend-common-datascope/bin`

### 当前内容
- `pom.xml`
- `src/main/resources/META-INF/spring/...imports`

### 比对结果
主模块已存在对应资源。

### 结论
**可直接删除**。

### 动作建议
- 删除 `basebackend-common/basebackend-common-datascope/bin/`

---

## 3.3 `basebackend-common/basebackend-common-storage/bin`

### 当前内容
- `pom.xml`
- `src/main/resources/META-INF/spring/...imports`

### 比对结果
主模块已存在对应资源。

### 结论
**可直接删除**。

### 动作建议
- 删除 `basebackend-common/basebackend-common-storage/bin/`

---

## 3.4 `basebackend-common/basebackend-common-util/bin`

### 当前内容
- `pom.xml`

### 比对结果
该目录接近空壳，无需迁移。

### 结论
**可直接删除**。

### 动作建议
- 删除 `basebackend-common/basebackend-common-util/bin/`

---

## 3.5 `basebackend-security/bin`

### 当前内容
- `pom.xml`
- `docs/`
- `src/main/resources/application-security.yml`
- `src/main/resources/META-INF/spring/...imports`

### 比对结果
主模块已存在：
- `basebackend-security/src/main/resources/application-security.yml`
- `basebackend-security/src/main/resources/META-INF/spring/...imports`

### 结论
资源层面**可直接删除**；文档属于副本型审查材料，建议归档后删除 `bin/`。

### 动作建议
- 若文档有保留价值：迁移 `docs/*.md` 到模块根 `docs/archive/`
- 否则直接删除整个 `basebackend-security/bin/`

### 推荐做法
当前更建议：**直接删除整个 `bin/`**，后续再统一处理报告类文档归档问题。

---

## 3.6 `basebackend-messaging/bin`

### 当前内容
- `CLAUDE.md`
- `docs/`
- `pom.xml`
- `src/main/resources/META-INF/spring/...imports`
- `src/main/resources/mapper/*.xml`

### 比对结果
主模块已存在：
- `src/main/resources/META-INF/spring/...imports`
- `src/main/resources/mapper/DeadLetterMapper.xml`
- `src/main/resources/mapper/MessageLogMapper.xml`
- `src/main/resources/mapper/WebhookEndpointMapper.xml`

### 结论
主资源已经重复，**可直接删除**。

### 动作建议
- 删除 `basebackend-messaging/bin/`
- 如需保留文档，后续统一从 Git 历史中恢复或集中归档

---

## 3.7 `basebackend-nacos/bin`

### 当前内容
- `CLAUDE.md`
- `README.md`
- `docs/`
- `example-config.yml`
- `pom.xml`
- `src/main/resources/application-nacos.yml`
- `src/main/resources/nacos-logback.xml`
- `src/main/resources/META-INF/spring/...imports`

### 比对结果
主模块已存在：
- `src/main/resources/application-nacos.yml`
- `src/main/resources/nacos-logback.xml`
- `src/main/resources/META-INF/spring/...imports`
- 甚至还存在 `spring.factories`

### 结论
资源层面已重复，**原则上可直接删除**。

### 唯一注意项
- `example-config.yml`
- `README.md`

这两个如果认为有保留价值，应迁出 `bin/`。

### 推荐做法
- 若你追求快速清理：直接删除整个 `bin/`
- 若你想保留说明资料：先迁移 `README.md` / `example-config.yml`，再删除

---

## 3.8 `basebackend-file-service/bin`

### 当前内容
- `CLAUDE.md`
- `Dockerfile`
- `README.md`
- 大量 `docs/`
- `pom.xml`
- 配置文件
- Flyway migration
- Mapper XML
- 静态示例页面

### 比对结果
主模块已存在对应资源：
- `application-file-storage-example.yml`
- `application-file.yml`
- `application-minio.yml`
- `db/migration/*.sql`
- `mapper/FileShareAuditLogMapper.xml`
- `static/examples/pdf-viewer.html`
- `static/examples/PdfViewer.tsx`
- 以及 `META-INF/spring/...imports`

### 结论
资源层面为**高度重复副本**，可以删除。

### 唯一注意项
- `Dockerfile`
- `README.md`
- `docs/*.md`

这些属于文档/部署说明，而非资源重复。是否保留取决于你是否认为这些资料还值得存在。

### 推荐做法
当前推荐：
- 若以“仓库结构回正”为优先，**直接删除整个 `bin/`**
- 后续若确需某份文档，可再按主题重新放回主模块 `docs/`

---

## 3.9 `basebackend-gateway/bin`

### 当前内容
- `CLAUDE.md`
- `Dockerfile`
- `Dockerfile.standalone`
- `GATEWAY_ROUTES.md`
- `docs/`
- `pom.xml`
- `src/main/resources/*.yml`
- `logback-structured.xml`
- `nacos-logback.xml`

### 比对结果
主模块已存在：
- `application-gateway.yml`
- `application-routes.yml`
- `application.yml`
- `logback-structured.xml`
- `nacos-logback.xml`

此外主模块还存在：
- `META-INF/native-image/.../native-image.properties`

### 结论
资源层面重复度高，**可删除**。

### 唯一注意项
- `application-dev.yml`
- `Dockerfile*`
- `GATEWAY_ROUTES.md`
- `docs/*.md`

这类内容若主模块无对应版本，需要决定是否保留。

### 推荐做法
- 若追求快速整理：直接删除 `bin/`
- 若要更细致：先迁移 `application-dev.yml` 和必要文档，再删除

---

## 3.10 `basebackend-observability/bin`

### 当前内容
- `CLAUDE.md`
- 大量 `docs/`
- `pom.xml`
- `src/main/resources/application-observability*.yml`
- `src/main/resources/logback-spring.xml.example`
- `src/main/resources/mapper/*.xml`
- `src/test/java/*.disabled`
- `src/test/resources/application-test.yml`

### 比对结果
当前扫描中，主模块下**未直接看到对应资源清单**。

这不意味着主模块一定没有，而是说明：
- 目前证据不足以支持“可直接删整个 `bin/`”
- 至少需要先比对主模块源码结构是否已经吸收这些资源

### 结论
**需先迁移/核对，再删除。**

### 动作建议
先做专项核查：
1. 比对主模块是否已有 `application-observability*.yml`
2. 比对 `mapper/AlertEventMapper.xml`、`AlertRuleMapper.xml` 是否已有正式版本
3. 判断 `.disabled` 测试文件是否应迁入主模块测试区或直接废弃
4. 文档按长期说明 / 阶段报告分类

### 当前不建议
- 不建议在未核实前直接删整个 `basebackend-observability/bin/`

---

## 4. 推荐执行顺序

### 第一步：先删低风险纯重复项
- `basebackend-jwt/bin`
- `basebackend-common/basebackend-common-datascope/bin`
- `basebackend-common/basebackend-common-storage/bin`
- `basebackend-common/basebackend-common-util/bin`

### 第二步：删中风险但资源已明确重复的目录
- `basebackend-security/bin`
- `basebackend-messaging/bin`
- `basebackend-nacos/bin`
- `basebackend-file-service/bin`
- `basebackend-gateway/bin`

### 第三步：最后单独处理 observability
- `basebackend-observability/bin`

---

## 5. 推荐策略（务实版）

如果你的目标是尽快让仓库结构回正，我建议：

### 方案 A：快速清理
- 先直接删除前 9 个 `bin/`
- 单独保留 `basebackend-observability/bin`
- 然后再做 observability 专项收口

### 方案 B：保守清理
- 先把 `README.md` / `Dockerfile` / `example-config.yml` / `application-dev.yml` 这类少数非重复文件迁出
- 再删除相应 `bin/`

### 我的建议
基于你当前这轮整理的目标，我更推荐：

> **先走方案 A**

理由很简单：
- 这次任务核心是把错位结构先收回来
- 大多数 `bin/` 已经被证实是重复副本
- 留着只会继续误导
- 少量需要的说明性文件，真要保留也可以后面按主题重新建立，不值得为保全旧布局继续拖结构治理后腿

---

## 6. 结论

目前可以非常明确地说：

- **前 9 个 `bin/` 目录已具备删除条件**
- **`basebackend-observability/bin` 需要专项核查后再动**

这已经足够支持下一步实际执行。