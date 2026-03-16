# basebackend-ticket-api 模块代码审查报告

- 审查日期：2026-03-02
- 审查范围：`basebackend-ticket-api`（含关联导出与搜索公共能力）
- 审查方式：静态代码审查 + 本地构建/测试验证
- 审查结论：发现 **8** 项重点问题（高风险 4，中风险 3，低风险 1）

---

## 一、总体结论

`basebackend-ticket-api` 模块主流程完整、分层清晰，但在“权限边界、越权防护、跨租户隔离、缓存一致性、测试可用性”上存在明显缺口。尤其是导出任务与实时/审批查询接口存在“仅认证、无细粒度授权”的情况，叠加任务归属未绑定，可能导致跨用户数据访问。

---

## 二、重点问题清单

## [高] 1) 异步导出状态/下载接口缺少权限与归属校验（可越权访问）

**问题描述**
- `exportStatus` 与 `download` 未标注 `@RequiresPermission`。
- 导出任务仅用 `taskId` 作为全局键，未记录发起人/租户归属；查询与下载只凭 `taskId` 即可获取状态/结果。
- `exportStatus` 对不存在任务缺少空值处理，存在空指针风险。

**证据**
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/export/TicketExportController.java:83`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/export/TicketExportController.java:94`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/export/TicketExportController.java:90`
- `basebackend-common/basebackend-common-export/src/main/java/com/basebackend/common/export/AsyncExportService.java:53`
- `basebackend-common/basebackend-common-export/src/main/java/com/basebackend/common/export/AsyncExportService.java:81`
- `basebackend-common/basebackend-common-export/src/main/java/com/basebackend/common/export/AsyncExportService.java:88`

**影响**
- 认证用户可尝试越权查询/下载他人的导出结果（IDOR 风险）。
- 非法 `taskId` 请求可能触发 500。

**建议修复**
- 给 `/status/{taskId}`、`/download/{taskId}` 增加 `@RequiresPermission("ticket:export")`。
- 导出任务状态中增加 `ownerUserId`、`tenantId` 并在查询/下载时强制匹配。
- 对 `status == null` 返回 404/业务错误码，避免空指针。

---

## [高] 2) 审批查询与实时订阅接口缺少细粒度权限注解

**问题描述**
- 审批记录列表与活跃任务接口未做 `@RequiresPermission`。
- 实时订阅/取消订阅接口未做 `@RequiresPermission`。

**证据**
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/controller/TicketApprovalController.java:54`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/controller/TicketApprovalController.java:82`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/realtime/TicketRealtimeController.java:28`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/realtime/TicketRealtimeController.java:38`

**影响**
- 在“已登录即通过”的场景下，任意认证用户可能读取不该看的审批信息，或订阅不属于自己的工单实时事件。

**建议修复**
- 审批查询接口至少补 `ticket:approve` 或 `ticket:query` 权限。
- 实时接口补 `ticket:query`（或专门 `ticket:realtime`）权限。
- 额外加入资源级校验：当前用户是否对 `ticketId` 有访问权（数据权限/工单参与者校验）。

---

## [高] 3) 评论/附件删除未校验“子资源归属”，存在越权删除与计数污染

**问题描述**
- 删除评论、删除附件均直接按子资源 ID 删除，未校验其是否属于路径中的 `ticketId`。
- 随后仍按路径中的 `ticketId` 递减计数，可能污染错误工单计数。

**证据**
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketCommentServiceImpl.java:73`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketCommentServiceImpl.java:75`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketAttachmentServiceImpl.java:78`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketAttachmentServiceImpl.java:80`

**影响**
- 可构造“跨工单子资源删除”请求（IDOR）。
- 评论/附件计数出现负偏差，影响统计与展示。

**建议修复**
- 先按 `id + ticketId` 查询子资源，不匹配则拒绝。
- 删除前后以数据库聚合重新计算计数，避免并发下累加/递减漂移。

---

## [高] 4) 创建工单允许客户端传入 `reporterId/reporterName/deptId`，可伪造身份

**问题描述**
- DTO 对外暴露了 `reporterId/reporterName/deptId`。
- 服务层仅在字段为 `null` 时才从上下文回填，意味着调用方可主动指定并覆盖。

**证据**
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/dto/TicketCreateDTO.java:30`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/dto/TicketCreateDTO.java:40`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketServiceImpl.java:84`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketServiceImpl.java:88`

**影响**
- 可能伪造提交人、归属部门，破坏审计可信度与数据权限边界。

**建议修复**
- 从请求 DTO 中移除这些字段，统一以 `UserContextHolder` 覆盖写入。
- 如确需代提交，改为单独受控接口并加入更高权限校验与审计。

---

## [中] 5) 搜索过滤字段映射错误：`assigneeId` 被过滤到 `assigneeName`

**问题描述**
- 搜索条件中 `assigneeId` 被错误地用于 `assigneeName` 字段 term 过滤。

**证据**
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/search/TicketSearchServiceImpl.java:111`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/search/TicketSearchServiceImpl.java:112`

**影响**
- 指定处理人 ID 的筛选结果失真或为空，影响业务查询准确性。

**建议修复**
- 在索引文档增加 `assigneeId` 字段，并按该字段过滤。

---

## [中] 6) 搜索索引缺少租户字段与租户过滤，`SHARED_DB` 下存在跨租户泄露风险

**问题描述**
- 当前多租户模式为 `SHARED_DB`。
- 搜索文档未包含 `tenantId`，查询侧也未强制 tenant filter。

**证据**
- `basebackend-ticket-api/src/main/resources/application.yml:83`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/search/TicketSearchDocument.java:23`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/search/TicketSearchServiceImpl.java:68`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/search/TicketSearchServiceImpl.java:103`

**影响**
- ES 侧检索可能绕开数据库租户拦截，导致跨租户结果可见。

**建议修复**
- 索引与查询统一携带 `tenantId`，并在 `search()` 中强制过滤当前租户。
- 重建索引时同步写入租户字段。

---

## [中] 7) `ticketNo` 维度缓存未失效，更新后可能读到旧数据

**问题描述**
- 读取工单号走缓存键 `no:<ticketNo>`。
- 更新/状态变更/分配/删除仅驱逐 `id` 维度缓存，未驱逐 `no:*` 维度。

**证据**
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketServiceImpl.java:129`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketServiceImpl.java:209`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketServiceImpl.java:241`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketServiceImpl.java:295`
- `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketServiceImpl.java:340`

**影响**
- `GET /api/ticket/tickets/no/{ticketNo}` 可能返回旧状态或旧字段。

**建议修复**
- 对写操作改用 `@Caching` 同时驱逐 `id` 与 `no:<ticketNo>`。
- 或统一封装缓存键策略，避免多键失效遗漏。

---

## [低] 8) 模块测试当前不可编译（导出测试用例与模型构造方式不匹配）

**问题描述**
- 测试代码使用 `new ExportResult()`，但 `ExportResult` 由 `@Builder` 生成构造器后不再支持无参构造。

**证据**
- `basebackend-ticket-api/src/test/java/com/basebackend/ticket/export/TicketExportControllerTest.java:68`
- `basebackend-ticket-api/src/test/java/com/basebackend/ticket/export/TicketExportControllerTest.java:88`
- `basebackend-common/basebackend-common-export/src/main/java/com/basebackend/common/export/ExportResult.java:7`

**影响**
- `basebackend-ticket-api` 的测试编译失败，影响 CI 可用性与回归保障。

**建议修复**
- 测试改为 `ExportResult.builder()` 构造，或在模型中显式补 `@NoArgsConstructor`（二选一，推荐前者）。

---

## 三、验证记录

执行命令与结果：

1. `mvn -pl basebackend-ticket-api test`
   - 结果：失败（依赖未安装：`basebackend-search`、`basebackend-ai`、`basebackend-websocket`）。

2. `mvn -pl basebackend-ticket-api -am test`
   - 结果：失败（上游 `basebackend-security` 测试失败，阻断到达 ticket-api）。

3. `mvn -pl basebackend-ticket-api -am -DskipTests install`
   - 结果：失败（`basebackend-ticket-api` 在 testCompile 阶段失败，见“问题 8”）。

4. `mvn -pl basebackend-ticket-api -DskipTests compile`
   - 结果：成功（主代码可编译）。

---

## 四、建议整改优先级

- **第一优先级（立即）**：问题 1、2、3、4（权限与越权风险）。
- **第二优先级（本迭代）**：问题 5、6、7（查询准确性、租户隔离、缓存一致性）。
- **第三优先级（尽快）**：问题 8（恢复测试可用性，保障后续变更质量）。

---

## 五、附注

本报告仅覆盖静态审查与本地构建验证；未进行联调压测、真实租户隔离回归、渗透测试。

---

## 六、修复进展（2026-03-02）

以下问题已完成修复并通过回归测试：

- 已修复：问题 1（导出状态/下载接口权限与任务归属校验、空值处理）
- 已修复：问题 2（审批查询、实时订阅接口补权限注解）
- 已修复：问题 3（评论/附件删除新增“子资源归属校验”，并改为按数据库实时计数回写）
- 已修复：问题 4（创建工单时 reporter/dept 由当前用户上下文覆盖，避免客户端伪造）
- 已修复：问题 5（搜索过滤字段从 `assigneeName` 修正为 `assigneeId`）
- 已修复：问题 6（搜索索引补充 `tenantId` 字段，查询强制追加租户过滤）
- 已修复：问题 7（更新/状态变更/分配/删除后补充 `ticketNo` 维度缓存失效）

本次修复相关验证命令：

- `mvn -pl basebackend-ticket-api -Dtest=TicketExportControllerTest,TicketPermissionAnnotationsTest,TicketSearchServiceImplTest,TicketCommentServiceImplTest,TicketAttachmentServiceImplTest,TicketServiceImplTest test`
  - 结果：通过（41 tests, 0 failures）
