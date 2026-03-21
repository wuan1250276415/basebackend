# BaseBackend 可执行清理计划（2026-03-21）

> 目标：基于《项目整理审查报告（2026-03-21）》将本仓库从“源码、产物、历史残骸、工具目录混杂”的状态，整理为边界清晰、可维护、可持续演进的工程仓库。

---

## 1. 执行原则

1. **止血优先**：先处理产物污染、依赖入库、缓存文件。
2. **最小破坏**：对历史目录和 `bin/` 副本先核查来源，再删除或归档。
3. **分阶段提交**：每个阶段独立提交，避免一次性大爆炸。
4. **先结构后命名**：先把无效内容拿走，再讨论命名统一。
5. **先对齐事实再重写文档**：README 和 docs 的修订必须基于整理后的真实结构。
6. **每个阶段都应可回滚**。

---

## 2. 目标产出

本轮清理完成后，应至少达到以下状态：

- Git 中不再跟踪 `node_modules`、`dist`、`.class`、`.jar`、`.m2` 缓存、`*.tsbuildinfo`
- 根目录只保留主工程必要入口
- 明确哪些目录是正式模块，哪些是前端应用，哪些是归档资料
- `bin/` 历史副本得到清理或隔离
- README 中的模块结构与仓库事实一致
- `basebackend-chat-api` 和 `basebackend-album-api` 拥有基本测试布局
- 三个前端目录遵循统一的工程规则

---

## 3. 分阶段清理计划

## Phase 1：仓库止血（必须先做）

### 3.1 目标

移除最明显的仓库污染项，恢复 Git 仓库的基本整洁度。

### 3.2 处理项

#### A. 清理前端依赖与构建产物

- `basebackend-chat-ui/node_modules/**`
- `basebackend-chat-ui/dist/**`

#### B. 清理编译产物与缓存

- 全仓库 `*.class`
- 根目录 `app-extracted.jar`
- 根目录 `org/...class`
- `.m2/repository/**/*.lastUpdated`
- `*.tsbuildinfo`

#### C. 补齐忽略规则

建议更新 `.gitignore`，至少覆盖：

```gitignore
node_modules/
dist/
*.class
*.jar
*.tsbuildinfo
.m2/
```

> 注意：如果某些 `jar` 是确有意图提交的发行物，必须先明确例外规则；当前审查未发现这种必要性。

### 3.3 验收标准

- `git ls-files` 不再包含：
  - `node_modules`
  - `dist`
  - `*.class`
  - `app-extracted.jar`
  - `.m2/**`
  - `*.tsbuildinfo`
- 仓库 diff 体积明显下降
- 不影响主模块源码目录

### 3.4 风险提示

- 不要在本阶段顺手做结构重命名
- 不要把 `bin/` 目录直接全删，这部分单独处理

---

## Phase 2：伪模块与历史残骸识别

### 4.1 目标

识别哪些顶层目录是“真实模块”，哪些只是历史遗留、拆分失败、构建残留或空壳。

### 4.2 重点核查对象

- `basebackend-feature-toggle`
- `basebackend-feign-api`
- `basebackend-security-starter`
- `basebackend-transaction`
- `basebackend-web`

### 4.3 对每个目录做三选一判断

#### 选项 A：恢复为正式模块
条件：
- 有明确业务价值
- 需要重新纳入 reactor 或作为独立模块维护

#### 选项 B：归档
条件：
- 有参考价值
- 暂时不参与主工程构建

建议归档到类似目录：

- `archive/modules/`
- `archive/experiments/`

#### 选项 C：删除
条件：
- 仅剩 `target/`
- 无源码、无文档价值、无依赖引用

### 4.4 验收标准

- 顶层目录都能回答“它是什么、是否生效、为什么在这里”
- 不再存在“名字像正式模块，但实际无效”的目录

---

## Phase 3：专项清理 `bin/` 重复树

### 5.1 目标

将模块内部的 `bin/` 重复树从主源码树中清理出去。

### 5.2 重点路径

- `basebackend-file-service/bin`
- `basebackend-gateway/bin`
- `basebackend-jwt/bin`
- `basebackend-messaging/bin`
- `basebackend-nacos/bin`
- `basebackend-observability/bin`
- `basebackend-security/bin`
- `basebackend-common/basebackend-common-datascope/bin`
- `basebackend-common/basebackend-common-storage/bin`
- `basebackend-common/basebackend-common-util/bin`

### 5.3 执行动作

#### 第一步：分类
对每个 `bin/` 目录标记类型：

- **脚本目录**：真实可执行脚本为主
- **重复源码副本**：含 `pom.xml` + `src/`
- **编译产物目录**：以 `.class` / 输出文件为主
- **调试快照 / 提取残留**

#### 第二步：迁移或删除

- 纯产物或重复副本：删除
- 有审计参考价值但非主工程内容：迁移至 `archive/bin-snapshots/`
- 仅保留真正需要的启动脚本，并迁移到统一根 `bin/` 或模块 `scripts/`

### 5.4 验收标准

- 模块内部不再出现伪源码 `bin/` 副本
- `bin/` 的语义统一：脚本就是脚本，不再伪装成源码树

### 5.5 风险提示

- 这一阶段必须做目录来源确认
- 不建议盲删；建议按模块逐步执行并提交

---

## Phase 4：根目录分层与文档归位

### 6.1 目标

让根目录只承担“工程入口”的角色，不再继续平铺杂项资料。

### 6.2 建议的根目录分层

#### 保留在根目录的内容

- `pom.xml`
- 主模块目录
- 前端应用目录
- `docs/`
- `config/`
- `docker/`
- `bin/`
- `.github/`
- 核心说明文件（README、checkstyle、spotbugs 等）

#### 迁移/归档的内容

- `plan/` → `docs/plans/` 或 `archive/plans/`
- `issues/` → `docs/issues/` 或 `archive/issues/`
- `wiki/` → `docs/wiki-mirror/` 或 `archive/wiki/`
- `TEST_FIXING_KNOWLEDGE_BASE/` → `docs/knowledge-base/`
- 工具目录根据团队约定处理：
  - `.claude`
  - `.kiro`
  - `.omc`
  - `.serena`
  - `.spec-workflow`
  - `.golutra`
  - `.ace-tool`

### 6.3 文档分层建议

将 `docs/` 拆为：

- `docs/reference/`：长期参考文档
- `docs/specs/`：需求/设计说明
- `docs/review/`：审查报告
- `docs/reports/`：阶段性执行报告
- `docs/archive/`：历史过期材料

### 6.4 验收标准

- 根目录项数显著下降
- 文档结构变得可预测
- 新人可以快速区分源码、文档、归档、工具目录

---

## Phase 5：README 与结构口径对齐

### 7.1 目标

让 README 真实反映当前仓库结构。

### 7.2 重点修订内容

- 顶层模块数量
- Maven reactor 实际模块
- 前端项目位置与角色
- 聚合模块与子模块层次
- 哪些目录不参与 Maven 构建
- 根目录中保留的工具/文档分区说明

### 7.3 验收标准

- README 中的结构描述与实际目录一致
- 不再出现“24 个顶层模块”这类过期统计
- 新人仅看 README 能建立正确地图

---

## Phase 6：测试与前端工程规范统一

### 8.1 目标

收尾阶段补齐工程规范，让仓库整理不是一次性“扫地”，而是形成可持续边界。

### 8.2 后端测试优先项

优先补齐以下模块：

- `basebackend-chat-api`
- `basebackend-album-api`

至少补齐：

- `src/test/java`
- 基础 smoke test / context load test
- 核心业务或控制器的最小回归测试

### 8.3 前端工程统一项

统一约定以下内容：

- 是否统一使用 Vitest
- 是否保留 `package-lock.json`
- `dist/` 是否永不入库（建议是）
- `node_modules/` 是否永不入库（必须是）
- `.env.*` 的提交规则
- `*.tsbuildinfo` 的忽略规则
- 是否提供统一的 `npm run test` / `npm run build`

### 8.4 验收标准

- 三个前端项目具备一致的工程边界
- 至少关键新业务模块有最小测试布局
- 后续整理不会再被构建缓存反向污染

---

## 4. 推荐执行顺序（实际落地版）

建议按下面顺序推进，每一步独立提交：

1. **提交 1：仓库止血**
   - 删 `node_modules` / `dist` / `.class` / `.jar` / `.m2` / `*.tsbuildinfo`
   - 改 `.gitignore`

2. **提交 2：伪模块识别与处理**
   - 删除空壳目录或迁移到 `archive/`

3. **提交 3：清理 `bin/` 副本树**
   - 按模块逐步处理，避免误删

4. **提交 4：文档与根目录归位**
   - 调整 `docs/`、`wiki/`、`plan/`、`issues/` 等

5. **提交 5：README 结构修订**
   - 对齐真实工程地图

6. **提交 6：测试与前端规范统一**
   - 先从 chat-api 和 album-api 开始

---

## 5. 建议的任务拆分

### 任务包 A：止血清理
- 更新 `.gitignore`
- 清理被跟踪产物
- 验证 `git ls-files` 输出

### 任务包 B：伪模块处置
- 为每个可疑目录写一句结论
- 决定保留 / 归档 / 删除

### 任务包 C：`bin/` 专项核查
- 逐目录列出来源和内容类型
- 确认后删除或迁移

### 任务包 D：文档重构
- 调整 `docs/` 层级
- README 重写结构章节

### 任务包 E：规范补齐
- 前端统一规范
- 后端测试补齐

---

## 6. 完成判定

满足以下条件时，可认为本轮“项目整理”完成：

- 仓库不再跟踪构建产物和依赖目录
- 模块边界清晰，没有明显伪模块
- `bin/` 副本树已清理或归档
- 根目录职责明确
- README 与 docs 口径统一
- 关键业务模块具备最小测试布局
- 前端项目遵循统一工程规则

---

## 7. 建议的下一步

最合理的下一步不是继续讨论，而是直接开始执行 **Phase 1：仓库止血**。

如果采用 agent 辅助整理，建议严格限制第一轮只做：

- 忽略规则补齐
- 产物与缓存移出 Git
- 不碰业务逻辑
- 不做模块重命名

这样风险最低，收益最大，也最适合作为整理工程的第一刀。
