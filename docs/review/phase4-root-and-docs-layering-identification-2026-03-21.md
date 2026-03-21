# Phase 4 根目录与文档分层识别报告（2026-03-21）

> 目标：识别 `basebackend` 根目录与文档区当前的结构问题，明确哪些内容应保留在根目录，哪些应迁入 `docs/`，哪些应归档，哪些属于本地工具状态或协作辅助目录。

---

## 1. 总体结论

经过 Phase 1~3 的污染清理后，仓库已经从“构建垃圾堆”回到了“像个工程仓库”的状态，但根目录仍然承担了过多职责。

当前根目录同时混放了：

1. **正式工程入口**（模块、pom、config、docker）
2. **长期文档**（`docs/`、`wiki/`）
3. **阶段性计划与问题跟踪**（`plan/`、`issues/`）
4. **知识库类资料**（`TEST_FIXING_KNOWLEDGE_BASE/`）
5. **多套 agent / workflow 工具状态目录**（`.claude`、`.kiro`、`.omc`、`.serena`、`.spec-workflow`、`.golutra`、`.ace-tool`）
6. **运行时目录**（`logs/`、`storage/`）

这意味着：

> 根目录虽然已经不脏了，但仍然“职责过载”。

Phase 4 的目标，不是再去删代码，而是：

> **把“工程入口”和“文档/资料/工具状态”拆开。**

---

## 2. 根目录分类判断

## 2.1 应继续保留在根目录的内容

这些是主工程入口，不建议移动：

### 构建与工程基础
- `pom.xml`
- `README.md`
- `checkstyle.xml`
- `spotbugs.xml`
- `sonar-project.properties`
- `.gitignore`
- `.dockerignore`
- `.env.example`
- `.env.production`（若确实是模板性质；若含真实环境信息则应进一步审查）
- `.trivyignore`
- `.trivy.yaml`

### 代码与模块目录
- 所有正式 `basebackend-*` 模块目录
- `config/`
- `docker/`
- `monitoring/`
- `sentinel-rules/`

### 协作入口文件
- `AGENTS.md`
- `CLAUDE.md`

> 备注：这类文件即使带有 agent 语义，只要承担仓库级协作说明，仍可留在根目录。

---

## 2.2 不适合继续平铺在根目录的内容

### A. 计划与 issue 导出
- `plan/`
- `issues/`

当前内容：
- `plan/2026-01-18_14-12-40-project-review-plan.md`
- `issues/2026-01-18_14-16-51-project-review-plan.csv`

### 判断
这类内容不是工程入口，而是一次性或阶段性项目管理材料。

### 建议归位
- `docs/plans/`
- `docs/issues/`

或若你更偏向归档：
- `docs/archive/plans/`
- `docs/archive/issues/`

---

### B. Wiki 镜像/知识型文档
- `wiki/`

当前内容是成体系的中文知识文档，例如：
- `项目概览.md`
- `项目结构说明.md`
- `快速启动指南.md`
- `基础设施模块说明.md`
- `API网关.md`
- `JWT认证体系.md`
- `Docker部署.md`

### 判断
`wiki/` 本身是长期文档，不是垃圾，但它与 `docs/` 形成了双文档入口。

### 风险
- README 已经把完整文档指向 GitHub Wiki
- 仓库里又同时保留 `wiki/` 目录和 `docs/` 目录
- 会形成“到底看哪边”的认知分叉

### 建议
二选一：

#### 方案 A（推荐）
保留 `wiki/` 作为 **Wiki 镜像区**，但在 README 与 docs 中明确它的角色。

建议定位：
- `wiki/` = 面向使用者的长期说明 / GitHub Wiki 镜像
- `docs/` = 审查、报告、专项设计、运行手册、项目内资料

#### 方案 B
将 `wiki/` 整体迁入 `docs/wiki/`

但这样会影响与 GitHub Wiki 的概念对应关系，不如方案 A 清晰。

### 结论
**建议保留 `wiki/`，但要在 README 中明确它和 `docs/` 的职责分工。**

---

### C. 测试修复知识库
- `TEST_FIXING_KNOWLEDGE_BASE/`

结构完整，包含：
- 架构修复策略
- 测试修复模式
- 最佳实践
- 案例研究
- 快速参考

### 判断
这不是临时垃圾，而是一个专题知识库。

### 问题
它现在的目录名非常“临时项目味”，而且平铺在根目录会显得突兀。

### 建议
迁入文档区，并重命名为更长期化的名字，例如：
- `docs/knowledge-base/test-fixing/`
- 或 `docs/testing/knowledge-base/`

### 结论
**建议迁入 `docs/knowledge-base/test-fixing/`。**

---

### D. 本地/多代理工具目录
- `.claude`
- `.kiro`
- `.omc`
- `.serena`
- `.spec-workflow`
- `.golutra`
- `.ace-tool`

### 判断
这批目录不是源码，也不是业务文档，而是：
- agent 协作配置
- 本地工作流模板
- AI 规划/执行辅助状态

### 问题
它们是否应该存在，不取决于“目录好不好看”，而取决于：
- 这仓库是否主动采用这些工具协作
- 团队是否接受这些目录作为仓库协作元数据

### 进一步判断
从内容看，它们不是纯缓存垃圾：
- `.claude` 有 commands / skills / team-plan
- `.kiro` 有 specs / steering
- `.omc` 有 plans
- `.spec-workflow` 有模板
- `.golutra` 有 agents / skills

也就是说，这批目录至少部分承担“协作流程资产”的角色，而不是简单临时目录。

### 建议
#### 短期建议
**先保留，不在 Phase 4 直接动。**

#### 中期建议
后续可考虑在根目录下建立统一入口说明，比如：
- `docs/reference/agent-tooling.md`

解释这些目录分别用于什么。

### 结论
**Phase 4 暂不迁移这批工具目录，只做文档解释与边界声明。**

---

### E. 运行时目录
- `logs/`
- `storage/`

### 判断
这两个目录是否应入库，需要单独再看 `.gitignore` 和实际用途。

一般来说：
- `logs/` 更像运行时输出，不适合长期入库
- `storage/` 可能是样例数据、挂载目录、也可能是运行时文件

### 结论
这两个目录值得单独审查，但不属于本轮 Phase 4 主线。

---

## 3. `docs/` 当前问题

当前 `docs/` 顶层同时混放了：

1. **系统规格说明**
   - `album-system-spec.md`
   - `chat-system-spec.md`
   - `ticket-system-spec.md`

2. **参考文档 / 运行手册**
   - `API_DOCUMENTATION.md`
   - `api-inventory.md`
   - `CODE_QUALITY_TOOLS.md`

3. **大量阶段性报告**
   - `*_REPORT.md`
   - `*_SUMMARY.md`
   - `*_STATUS.md`
   - `*_EXECUTION_REPORT.md`

4. **专题子目录**
   - `docs/mall/`
   - `docs/review/`

### 结论
`docs/` 当前最大的问题不是内容少，而是：

> **缺少层次。**

---

## 4. `docs/` 建议分层

建议逐步收敛为以下结构：

```text
docs/
  reference/        # 长期参考文档、接口说明、工具说明、运行手册
  specs/            # 功能规格、模块设计、系统方案
  review/           # 审查、盘点、治理报告
  reports/          # 阶段性执行报告、修复总结、状态汇报
  knowledge-base/   # 专题知识库
  mall/             # mall 专题文档（可保留）
  archive/          # 过时或阶段性完成后保留的旧材料
```

---

## 5. 推荐迁移映射

## 5.1 建议迁入 `docs/specs/`

- `docs/album-system-spec.md`
- `docs/chat-system-spec.md`
- `docs/ticket-system-spec.md`

---

## 5.2 建议迁入 `docs/reference/`

- `docs/API_DOCUMENTATION.md`
- `docs/api-inventory.md`
- `docs/CODE_QUALITY_TOOLS.md`

可选（视内容而定）：
- `walkthrough.md` → `docs/reference/walkthrough.md`

---

## 5.3 建议继续保留在 `docs/review/`

当前已经较成体系：
- 模块审查
- 结构治理
- 本轮清理报告

这部分可以继续保留为“治理审查专区”。

---

## 5.4 建议迁入 `docs/reports/`

建议纳入所有阶段性执行成果，例如：
- `CACHE_MODULE_TEST_FIX_REPORT.md`
- `DATABASE_P1_FIX_SUMMARY.md`
- `FILE_SHARE_SECURITY_OPTIMIZATION_REPORT.md`
- `FULL_PROJECT_TEST_STATUS_SUMMARY.md`
- `IMMEDIATE_EXECUTABLE_TASKS_EXECUTION_REPORT.md`
- `MEDIUM_PRIORITY_FIX_REPORT.md`
- `P1_CODE_QUALITY_IMPROVEMENT_REPORT.md`
- `P2_CODE_QUALITY_IMPROVEMENT_REPORT.md`
- `P2_COMPREHENSIVE_TEST_REPORT.md`
- `PHASE3_SECURITY_ENHANCEMENT_SUMMARY.md`
- `SCHEDULER_MODULE_FINAL_FIX_REPORT.md`
- `SCHEDULER_MODULE_QUICK_FIX_EXECUTION_REPORT.md`
- `SCHEDULER_MODULE_TEST_FIX_PROGRESS_REPORT.md`
- `SECURITY_HIGH_PRIORITY_FIXES_COMPLETION_REPORT.md`
- `SECURITY_MODULE_P2_OPTIMIZATION_SUMMARY.md`
- `SERVICE_LAYER_PRIORITY_STRATEGY_SUCCESS_REPORT.md`
- `TEST_FIXING_FINAL_SUCCESS_REPORT.md`
- `TEST_IMPLEMENTATION_SUMMARY.md`
- `TESTING_SUMMARY.md`

---

## 5.5 建议迁入 `docs/knowledge-base/`

- `TEST_FIXING_KNOWLEDGE_BASE/` → `docs/knowledge-base/test-fixing/`

---

## 5.6 建议迁入 `docs/plans/` / `docs/issues/`

- `plan/2026-01-18_14-12-40-project-review-plan.md` → `docs/plans/`
- `issues/2026-01-18_14-16-51-project-review-plan.csv` → `docs/issues/`

---

## 6. README 当前问题

本次扫描确认 README 至少存在这几个明显问题：

### 6.1 模块统计过期
README 仍写：
- `Modules-24`
- `24 个顶层模块 · 68 个子模块`

但当前仓库结构已不是这个数字。

### 6.2 文档入口没有分工说明
README 只强调 GitHub Wiki，但仓库内同时还有：
- `docs/`
- `wiki/`
- `docs/review/`
- `TEST_FIXING_KNOWLEDGE_BASE/`

缺少“看哪里找什么”的说明。

### 6.3 项目结构图未同步清理成果
README 的结构图没有反映：
- 伪模块已经清理
- `bin/` 副本树已清理
- 文档区应分层

---

## 7. Phase 4 的推荐动作

## 7.1 本轮建议先做的低风险动作

1. 新建文档分层目录：
   - `docs/specs/`
   - `docs/reference/`
   - `docs/reports/`
   - `docs/knowledge-base/`
   - `docs/plans/`
   - `docs/issues/`

2. 迁移最明确的文档：
   - 3 个 `*-system-spec.md`
   - `API_DOCUMENTATION.md`
   - `api-inventory.md`
   - `CODE_QUALITY_TOOLS.md`
   - `plan/`、`issues/`
   - `TEST_FIXING_KNOWLEDGE_BASE/`

3. 更新 README 的文档导航区

---

## 7.2 本轮建议暂缓的动作

1. 不直接处理 `.claude` / `.kiro` / `.omc` / `.serena` / `.spec-workflow` / `.golutra` / `.ace-tool`
2. 不直接判断 `logs/` 与 `storage/` 的去留
3. 不立即改动所有报告类文件名

原因：这些动作更容易牵涉协作习惯与运行流程，适合单独审查。

---

## 8. 最终判断

Phase 4 的核心，不是“继续删”，而是：

> **把仓库从“整理过的工程”进一步推进到“结构可读的工程”。**

当前最值得做的，是先建立一套稳定的文档分层，再把最明确的计划/问题/知识库/规格说明归位。这样做收益高、风险低，而且能立刻改善 README 与 docs 的可读性。

---

## 9. 建议的下一步

最自然的下一步是：

### 生成并执行 Phase 4 第一批低风险迁移清单

也就是只处理：
- `plan/`
- `issues/`
- `TEST_FIXING_KNOWLEDGE_BASE/`
- `docs/` 中最明确的 `specs/reference/reports` 归位

然后再单独修 README 文档导航。
