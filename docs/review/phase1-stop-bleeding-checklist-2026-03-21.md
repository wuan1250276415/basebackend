# Phase 1 止血清理清单（2026-03-21）

> 目标：仅处理仓库污染项，不碰业务逻辑、不做模块重命名、不改架构边界。

---

## 1. Phase 1 的范围

本阶段只做四类事情：

1. 停止继续跟踪明显不该入库的产物与缓存
2. 把已经进入 Git 的污染项移出版本控制
3. 补齐 `.gitignore` 基础规则
4. 验证仓库已经恢复到“源码仓库”的最低整洁标准

**本阶段明确不做：**

- 不改 Java 业务代码
- 不改 API 行为
- 不做模块重命名
- 不整理 `bin/` 重复树（另开阶段）
- 不删除历史模块（另开阶段）
- 不重写 README（另开阶段）

---

## 2. 建议变更项总览

## A. 必删：前端依赖与构建产物

### A1. `basebackend-chat-ui/node_modules/**`

**建议动作：**
- 从 Git 中移除整个 `node_modules/` 目录
- 保留本地依赖安装能力，不保留仓库跟踪

**原因：**
- 属于依赖目录，不应提交到源码仓库
- 当前约 20163 个被跟踪文件，污染最重

**风险：**
- 低

**前提检查：**
- `basebackend-chat-ui/package.json` 存在
- `basebackend-chat-ui/package-lock.json` 存在

**预期结果：**
- 后续通过 `npm install` / `npm ci` 恢复依赖
- Git 中不再出现 `node_modules/**`

---

### A2. `basebackend-chat-ui/dist/**`

**建议动作：**
- 从 Git 中移除整个 `dist/` 目录

**原因：**
- 属于构建产物，不应纳入源码仓库

**风险：**
- 低

**前提检查：**
- 构建入口仍由 `package.json` 驱动
- 项目不是靠提交 `dist` 产物发布（当前审查看不出有这个必要）

---

## B. 必删：编译产物与提取产物

### B1. 全仓库 `*.class`

**建议动作：**
- 将所有被 Git 跟踪的 `.class` 文件移出版本控制

**原因：**
- 编译产物，不应入库
- 当前约 597 个，且分散在多处

**风险：**
- 中低

**注意：**
- 删除前建议输出清单留档
- 尤其注意这些文件主要集中在：
  - 各类 `bin/` 目录
  - 根目录 `org/...class`

---

### B2. `app-extracted.jar`

**建议动作：**
- 从 Git 中移除 `app-extracted.jar`

**原因：**
- 明显属于提取/调试/分析产物
- 不属于源码仓库常规资产

**风险：**
- 低

**注意：**
- 如果你确认它有审计参考价值，建议迁移到归档目录，而不是继续平铺在根目录

---

### B3. `org/springframework/security/web/servlet/util/matcher/PathPatternRequestMatcher.class`

**建议动作：**
- 从 Git 中移除整个根目录 `org/` 下的编译类残留

**原因：**
- 明显不应作为源码仓库顶层主内容
- 会误导人以为这里存在一套可维护源码树

**风险：**
- 低

---

## C. 必删：本地缓存与工具生成文件

### C1. `.m2/repository/**/*.lastUpdated`

**建议动作：**
- 从 Git 中移除 `.m2/` 下已被跟踪的缓存文件

**当前已发现示例：**
- `.m2/repository/com/alibaba/cloud/...pom.lastUpdated`
- `.m2/repository/org/springframework/boot/...pom.lastUpdated`
- `.m2/repository/org/springframework/cloud/...pom.lastUpdated`

**原因：**
- 这是 Maven 本地缓存状态，不应提交

**风险：**
- 极低

---

### C2. `*.tsbuildinfo`

**建议动作：**
- 从 Git 中移除所有被跟踪的 `*.tsbuildinfo`

**当前已发现：**
- `basebackend-admin-web/tsconfig.tsbuildinfo`
- `basebackend-admin-web/tsconfig.node.tsbuildinfo`
- `basebackend-album-ui/tsconfig.tsbuildinfo`

**原因：**
- TypeScript 构建缓存，不应纳入版本控制

**风险：**
- 极低

---

## D. 必加：`.gitignore` 基础规则

建议补齐或确认以下规则存在：

```gitignore
# Frontend dependencies and builds
node_modules/
dist/

# Java / extracted build outputs
*.class
*.jar

# TypeScript incremental cache
*.tsbuildinfo

# Maven local cache
.m2/

# Optional: local tool state
*.log
```

### 注意

如果仓库中确实有极少数必须提交的 `jar`，应使用“先忽略，再白名单例外”的方式处理，而不是放任全部 `*.jar` 可提交。

---

## 3. 建议先导出一份删除前清单

在真正执行前，建议先生成并确认以下列表：

### 3.1 将被取消跟踪的目录/文件类型

- `basebackend-chat-ui/node_modules/**`
- `basebackend-chat-ui/dist/**`
- `**/*.class`
- `app-extracted.jar`
- `org/**`
- `.m2/**`
- `**/*.tsbuildinfo`

### 3.2 需要人工二次确认的项

#### 人工确认项 1：是否存在必须保留的 `jar`
当前已发现：
- `app-extracted.jar`

初步判断应删除，但建议执行前人工确认一次。

#### 人工确认项 2：根目录 `org/` 是否有任何非产物内容
当前审查只发现 `.class`，初步判断可整体移除。

#### 人工确认项 3：`basebackend-chat-ui/dist` 是否被任何发布流程依赖
若当前发布依赖构建而非提交产物，则可直接移出 Git。

---

## 4. 建议执行顺序

### 步骤 1：更新 `.gitignore`
先把规则补齐，避免你刚删完又被重新加回去。

### 步骤 2：移除前端依赖和构建产物
优先处理：
- `basebackend-chat-ui/node_modules`
- `basebackend-chat-ui/dist`

这是最重的污染源。

### 步骤 3：移除 `.class` / `.jar` / 根目录 `org`
把明显不属于源码树的产物移出去。

### 步骤 4：移除 `.m2` 与 `*.tsbuildinfo`
扫尾，把缓存类污染拿干净。

### 步骤 5：做验证
验证项包括：
- `git ls-files` 不再列出上述文件
- `package.json` / `package-lock.json` 仍在
- 主源码目录未受影响

---

## 5. Phase 1 执行后的预期状态

执行完后，仓库应该达到以下状态：

### 结构层面
- 根目录不再出现明显构建产物
- chat-ui 恢复为正常前端项目，而不是“源码 + node_modules 仓库镜像”

### Git 层面
- 不再跟踪 `node_modules`、`dist`、`.class`、`.jar`、`.m2`、`*.tsbuildinfo`

### 风险层面
- 不改变业务行为
- 不改变构建边界
- 为后续 Phase 2/3 的模块与目录清理创造干净基线

---

## 6. 建议的实际提交拆分

如果执行，建议拆成两个 commit：

### Commit 1：更新忽略规则
- 修改 `.gitignore`

### Commit 2：仓库止血清理
- 取消跟踪污染项
- 不包含业务代码修改

这样以后回看历史也清楚：
不是改业务，是在给仓库止血。

---

## 7. 建议的下一步

在执行 Phase 1 之前，建议再补一件小事：

> 先输出“准备删除的精确文件清单”，尤其是：

- `.class` 文件列表
- `.jar` 文件列表
- `.m2` 文件列表
- `*.tsbuildinfo` 文件列表

这样你就能在真正删除前最后过一眼，避免误伤。

如果继续下一步，最合理的是：

**生成一份《Phase 1 实际待删除文件清单》**，然后再执行止血清理。
