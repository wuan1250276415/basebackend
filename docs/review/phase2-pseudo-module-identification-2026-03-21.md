# Phase 2 伪模块识别报告（2026-03-21）

> 目标：识别顶层目录中“名字像正式模块，但当前不具备有效模块结构”的历史残骸，给出保留 / 归档 / 删除建议。

---

## 1. 审查对象

本次识别的目标目录：

- `basebackend-feature-toggle`
- `basebackend-feign-api`
- `basebackend-security-starter`
- `basebackend-transaction`
- `basebackend-web`

---

## 2. 审查方法

对每个目录检查以下内容：

1. 目录是否存在
2. 是否存在 `pom.xml`
3. 是否存在 `src/main/java` / `src/test/java` / `src/main/resources`
4. 是否出现在根 `pom.xml` 的 reactor 模块列表中
5. 当前目录中是否仅剩 `target/` 或产物目录
6. Git 是否仍在跟踪该目录下文件

---

## 3. 总体结论

这 5 个目录当前都 **不具备“正式模块”应有的最小结构**。

共同特征如下：

- 目录存在
- **没有 `pom.xml`**
- **没有 `src/` 主源码结构**
- **没有被 Git 跟踪的文件**
- 目录内几乎只剩 `target/` 构建输出
- **没有出现在根 `pom.xml` 的 reactor 模块列表中**

因此，这 5 个目录应统一判定为：

> **历史构建残骸 / 非有效模块目录**

它们当前不应继续被视为项目正式结构的一部分。

---

## 4. 分目录判定

## 4.1 `basebackend-feature-toggle`

### 现状

该目录存在，但仅发现：

- `target/basebackend-feature-toggle-1.0.0-SNAPSHOT.jar`
- `target/classes`
- `target/generated-sources`
- `target/generated-test-sources`
- `target/maven-archiver`
- `target/maven-status`
- `target/test-classes`

未发现：

- `pom.xml`
- `src/main/java`
- `src/test/java`
- `src/main/resources`

Git 跟踪文件数：**0**

### 判定

这是一个**仅剩构建产物目录的空壳残留**，不构成可维护模块。

### 建议

- **建议动作：删除目录**
- **理由：**没有源码、没有构建定义、没有被 Git 跟踪，也不在 reactor 中
- **保留必要性：**低

---

## 4.2 `basebackend-feign-api`

### 现状

该目录存在，但仅发现：

- `target/basebackend-feign-api-1.0.0-SNAPSHOT.jar`
- `target/classes`
- `target/generated-sources`
- `target/maven-archiver`
- `target/maven-status`

未发现：

- `pom.xml`
- `src/` 主源码结构

Git 跟踪文件数：**0**

### 判定

这是一个**仅保留 target 产物的历史目录**，不是有效模块。

### 建议

- **建议动作：删除目录**
- **理由：**当前没有任何可维护资产
- **补充说明：**名字很像正式公共模块，反而更具误导性，不适合继续留在根目录

---

## 4.3 `basebackend-security-starter`

### 现状

该目录存在，但仅发现：

- `target/basebackend-security-starter-1.0.0-SNAPSHOT.jar`
- `target/classes`
- `target/generated-sources`
- `target/generated-test-sources`
- `target/maven-archiver`
- `target/maven-status`
- `target/test-classes`

未发现：

- `pom.xml`
- `src/` 主源码结构

Git 跟踪文件数：**0**

### 判定

这是一个**构建后遗留目录**，并不具备 starter 模块的任何有效定义。

### 建议

- **建议动作：删除目录**
- **理由：**只剩 target，且未纳入版本控制，不属于主工程结构
- **注意：**如果团队历史上确实规划过该模块，可在文档中保留规划记录，而不是保留空壳目录

---

## 4.4 `basebackend-transaction`

### 现状

该目录存在，但仅发现：

- `target/basebackend-transaction-1.0.0-SNAPSHOT.jar`
- `target/classes`
- `target/generated-sources`
- `target/maven-archiver`
- `target/maven-status`

未发现：

- `pom.xml`
- `src/` 主源码结构

Git 跟踪文件数：**0**

### 判定

该目录当前仅代表一次历史构建结果，不构成实际可维护模块。

### 建议

- **建议动作：删除目录**
- **理由：**没有保留在主仓库根目录的必要性

---

## 4.5 `basebackend-web`

### 现状

该目录存在，但仅发现：

- `target/basebackend-web-1.0.0-SNAPSHOT.jar`
- `target/classes`
- `target/generated-sources`
- `target/maven-archiver`
- `target/maven-status`

未发现：

- `pom.xml`
- `src/` 主源码结构

Git 跟踪文件数：**0**

### 判定

该目录是**高度误导性的空壳残留目录**：

- 名称像基础 Web 模块
- 实际只剩 target 产物
- 不在 reactor
- 不在版本控制中

### 建议

- **建议动作：删除目录**
- **理由：**继续保留会误导后续模块边界认知

---

## 5. 与根 `pom.xml` 的关系

本次检查中，根 `pom.xml` 未发现以下模块引用：

- `basebackend-feature-toggle`
- `basebackend-feign-api`
- `basebackend-security-starter`
- `basebackend-transaction`
- `basebackend-web`

当前唯一匹配到的是：

- `basebackend-websocket`

这进一步说明：

> 上述 5 个目录当前并不是主工程实际参与构建的模块。

---

## 6. 风险评估

### 删除风险

总体风险：**低**

原因：

- 无 Git 跟踪文件
- 无源码
- 无 `pom.xml`
- 无 reactor 引用
- 仅为 target 构建残留

### 唯一需要注意的点

如果某些外部脚本、IDE 工作区或人工说明文档硬编码引用了这些路径，删除前应做一次全仓库文本搜索确认。

建议搜索关键字：

- `basebackend-feature-toggle`
- `basebackend-feign-api`
- `basebackend-security-starter`
- `basebackend-transaction`
- `basebackend-web`

如果搜索结果仅为历史文档描述，可同步更新文档。

---

## 7. 结论与建议动作

### 结论

这 5 个目录应统一归类为：

- **历史构建残骸**
- **非有效模块目录**
- **不应继续保留在根目录的误导性路径**

### 建议动作

#### 推荐方案（优先）

直接删除以下目录：

- `basebackend-feature-toggle/`
- `basebackend-feign-api/`
- `basebackend-security-starter/`
- `basebackend-transaction/`
- `basebackend-web/`

#### 执行前补充检查

1. 全仓库搜索这些目录名是否被引用
2. 若只在文档中出现，则更新文档口径
3. 删除后再次确认根目录结构与 `git status`

---

## 8. 建议的下一步

最适合作为 Phase 2 后续动作的是：

### Phase 2.1：生成删除前引用检查清单

先确认这些目录名是否被任何脚本、文档或配置引用。

### Phase 2.2：执行低风险清理

若确认仅为 target 残骸，则直接删除这些目录。

---

## 9. 最终判断

与 Phase 1 相比，Phase 2 的这批目录甚至更像“地上散落的空盒子”——它们不是功能代码，不是正式模块，也不是受版本控制的资产。

继续保留它们只会制造误解，不会带来任何真实价值。
