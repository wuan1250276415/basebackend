# Phase 5 测试基线识别报告（2026-03-21）

> 目标：识别 `basebackend-chat-api` 与 `basebackend-album-api` 当前测试布局缺口，并给出最小可执行的测试补齐方案。

---

## 1. 审查对象

- `basebackend-chat-api`
- `basebackend-album-api`

---

## 2. 总体结论

这两个模块当前有一个很典型的问题：

> **POM 里已经声明了 `spring-boot-starter-test`，但模块内完全没有 `src/test` 测试布局。**

也就是说，它们并不是“没有测试能力”，而是：

- 测试依赖已经接好了
- 主模块代码量已经不小
- 但测试目录、最小 smoke test、最基本的 context load test 都还没建立

这类状态的风险在于：

- 模块看起来像完整服务，实际上缺乏最小回归保护
- 后续重构、升级依赖、改配置时，容易无感破坏启动能力
- 结构治理已经推进到后半段，如果不补测试基线，后续改动没有护栏

---

## 3. 模块现状

## 3.1 `basebackend-chat-api`

### 当前情况

- 有 `spring-boot-starter-test`
- 有 Spring Boot 启动类：
  - `com.basebackend.chat.ChatApiApplication`
- 有完整控制器、服务、mapper、配置、WebSocket、搜索、Redis 等集成点
- 有 `src/main/resources/application.yml`
- 有 Flyway migration：
  - `db/migration/V1__chat_init.sql`
- **没有 `src/test/java`**
- **没有 `src/test/resources`**

### 风险判断

该模块比 album 更复杂，集成点更多：
- Web
- Redis
- DB/Flyway
- WebSocket
- Search
- Nacos
- JWT

如果直接上完整 `@SpringBootTest`，很可能一上来就被外部依赖绊住。

### 建议策略

对 chat-api，第一轮不追求“大而全”，而是先建立：

1. 测试目录结构
2. **最小化启动类存在性测试**
3. **控制器/DTO/配置类的轻量 smoke test**

更保守的做法是先不上真正全量上下文加载，而是：
- 用类存在性与注解约束测试先建立测试基线
- 再逐步补 WebMvc slice / service 单测 / integration test

---

## 3.2 `basebackend-album-api`

### 当前情况

- 有 `spring-boot-starter-test`
- 有 Spring Boot 启动类：
  - `com.basebackend.album.AlbumApiApplication`
- 有控制器、服务、mapper、DTO、实体、VO
- 有 `src/main/resources/application.yml`
- 有 Flyway migration：
  - `db/migration/V1__init_album_tables.sql`
- **没有 `src/test/java`**
- **没有 `src/test/resources`**

### 风险判断

album-api 也缺测试，但比 chat-api 的系统耦合略少一些，比较适合作为第一批“补最小测试基线”的示范模块。

### 建议策略

album-api 可以优先补：

1. `Application` 启动类 smoke test
2. 控制器存在性测试
3. service bean / 类结构测试
4. 后续再演进到 WebMvc 或 service 层测试

---

## 4. 推荐的最小测试基线

## 4.1 第一层：目录基线（必须有）

对两个模块都补齐：

```text
src/test/java/
src/test/resources/
```

即使第一版测试文件不多，也必须先把结构立起来。

---

## 4.2 第二层：最小 smoke test（推荐首批落地）

### chat-api
建议先补：

- `ChatApiApplicationSmokeTest`
- `ChatApiStructureSmokeTest`

可测试内容：
- 启动类存在
- 关键控制器类可加载
- 关键 service 接口 / 实现类存在
- 核心 DTO / VO / entity 包可访问

### album-api
建议先补：

- `AlbumApiApplicationSmokeTest`
- `AlbumApiStructureSmokeTest`

可测试内容：
- 启动类存在
- 控制器类存在
- 核心 service / mapper / entity 类存在

---

## 4.3 第三层：后续可演进测试（本轮可暂缓）

- `@WebMvcTest` 控制器切片测试
- service 单测
- mapper + DB integration test
- Flyway migration 验证
- 鉴权 / WebSocket / Redis 相关集成测试

这些适合作为后续阶段，而不是当前这轮结构治理的第一刀。

---

## 5. 不建议当前直接做的事情

### 不建议直接上全量 `@SpringBootTest`

原因：
- chat-api 依赖多
- album-api 也牵涉数据库/缓存/安全
- 如果 test profile 不完善，容易一补测试就先陷进环境问题泥坑

### 不建议一口气补业务深测

当前阶段目标是“建立测试基线”，不是“瞬间把覆盖率拉满”。

先把：
- 目录
- smoke test
- 最小结构校验

补起来，收益最大，风险最低。

---

## 6. 推荐执行顺序

### 步骤 1
先为两个模块创建：
- `src/test/java`
- `src/test/resources`

### 步骤 2
先补 album-api 最小 smoke test

理由：
- 相对更适合作为示范
- 依赖复杂度略低

### 步骤 3
再补 chat-api 最小 smoke test

### 步骤 4
确认 `mvn -pl <module> test` 至少能跑通这些基础测试

---

## 7. 最终判断

这两个模块目前都属于：

> **测试依赖已就位，但测试基线缺失。**

这类问题最适合用“小而稳”的方式补：

- 先立结构
- 先加 smoke test
- 先让模块开始有最小回归能力
- 再考虑更深层的集成与业务测试

---

## 8. 建议的下一步

最自然的下一步是：

### 执行 Phase 5 第一批补齐

为 `basebackend-chat-api` 和 `basebackend-album-api`：

1. 创建测试目录
2. 各补 2 个最小 smoke test
3. 先不碰复杂集成依赖
4. 验证模块级测试可执行性
