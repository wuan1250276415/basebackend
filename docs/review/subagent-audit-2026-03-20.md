# 子代理并行审查报告（2026-03-20）

## 范围

本次审查覆盖以下模块与切面：

- 用户、系统、通知、AI、WebSocket、Observability Service
- Gateway、Nacos、File Service、Backup、Service Client
- Chat、Ticket、Mall Product、Mall Trade、Mall Pay、Code Generator、Scheduler
- 主线程补充抽样校验当前仓库级安全、权限、并发与配置风险

说明：

- 本次使用了 4 个并行 subagents，其中 3 个完成并返回结论，1 个在基础模块切面超时关闭。
- 因此，`basebackend-common / jwt / database / cache / logging / security / observability / messaging` 这组基础模块没有形成完整的 subagent 审查结论；相关残余风险见文末说明。

## 高置信度 Findings

### Critical

1. 下单链路完全信任客户端提交的 `userId` 和 `payAmount`，允许代下单与金额篡改。
   - 文件：
     - `basebackend-mall-trade-api/src/main/java/com/basebackend/mall/trade/controller/TradeController.java:56`
     - `basebackend-mall-trade-api/src/main/java/com/basebackend/mall/trade/dto/OrderSubmitRequest.java:15`
     - `basebackend-mall-trade-api/src/main/java/com/basebackend/mall/trade/service/impl/TradeServiceImpl.java:78`
     - `basebackend-mall-trade-api/src/main/java/com/basebackend/mall/trade/service/impl/TradeServiceImpl.java:85`
     - `basebackend-mall-trade-api/src/main/java/com/basebackend/mall/trade/service/impl/TradeServiceImpl.java:95`
   - 影响：任何可达调用方都可以伪造他人下单，并把高价 SKU 以任意低价下单；后续支付、库存、订单事件都会传播这个伪造金额。
   - 建议：下单接口只接受 SKU/数量，`userId` 从认证上下文获取，金额由服务端核价并落快照。

2. “微信单点登录”实际是“手机号即登录”，且首次登录自动创建固定弱口令账号。
   - 文件：
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/AuthController.java:147`
     - `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java:212`
     - `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java:230`
     - `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java:236`
   - 影响：知道手机号即可冒充目标用户登录；未注册手机号还会被自动创建为默认密码 `123456` 的账号。
   - 建议：移除手机号直登；改为校验微信授权码/openId/unionId 或可信上游断言；自动补建账号时不得设置可用默认密码。

3. 文件服务当前并未真正验证 JWT，也允许伪造 `X-User-ID`。
   - 文件：
     - `basebackend-file-service/src/main/java/com/basebackend/file/security/AuthenticationFilter.java:122`
     - `basebackend-file-service/src/main/java/com/basebackend/file/security/AuthenticationFilter.java:169`
     - `basebackend-file-service/src/main/java/com/basebackend/file/security/AuthenticationFilter.java:226`
   - 影响：任何能访问文件服务的人都可以伪造任意身份；当前所谓 JWT 只是明文分隔字符串，不做签名/过期校验。
   - 建议：统一接入真实 JWT 验证；`X-User-ID` 只能在 mTLS 或受信签名网关之后接受，不能靠私网 IP 粗略放行。

4. WebSocket 握手完全信任客户端自报 `userId`，token 参数根本不校验。
   - 文件：
     - `basebackend-websocket/src/main/java/com/basebackend/websocket/interceptor/AuthHandshakeInterceptor.java:37`
     - `basebackend-websocket/src/main/java/com/basebackend/websocket/interceptor/AuthHandshakeInterceptor.java:46`
     - `basebackend-websocket/src/main/java/com/basebackend/websocket/interceptor/AuthHandshakeInterceptor.java:49`
     - `basebackend-websocket/src/main/java/com/basebackend/websocket/interceptor/AuthHandshakeInterceptor.java:56`
     - `basebackend-websocket/src/main/java/com/basebackend/websocket/interceptor/AuthHandshakeInterceptor.java:62`
   - 影响：客户端可以伪造任意 `userId` 建立连接，私聊、频道、在线态都会被冒用。
   - 建议：握手阶段只接受已验证 JWT/Session，由服务端解析 userId/roles；去掉 `userId` 直传能力。

### High

5. 文件列表与文件详情接口没有任何按用户/权限过滤，普通用户可枚举全部文件元数据。
   - 文件：
     - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:109`
     - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:132`
     - `basebackend-file-service/src/main/java/com/basebackend/file/service/FileManagementService.java:264`
     - `basebackend-file-service/src/main/java/com/basebackend/file/service/FileManagementService.java:294`
   - 影响：可批量读取其他用户文件名、路径、哈希、标签、目录结构等敏感元数据。
   - 建议：列表、详情、统计接口统一引入当前用户权限过滤，默认仅返回本人可见文件。

6. 旧版下载/删除接口绕过增强版权限模型，只靠路径即可访问或删除文件。
   - 文件：
     - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:141`
     - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:187`
     - `basebackend-file-service/src/main/java/com/basebackend/file/service/FileService.java:120`
     - `basebackend-file-service/src/main/java/com/basebackend/file/service/FileService.java:142`
   - 影响：知道或猜到路径即可绕过 `download-v2` / `delete-v2` 的权限体系。
   - 建议：下线旧接口，或者改成仅内部可用并复用统一的元数据和权限校验链。

7. 分块上传允许客户端控制最终 `targetPath`，本地存储实现未做路径归一化校验，存在路径穿越写文件风险。
   - 文件：
     - `basebackend-file-service/src/main/java/com/basebackend/file/controller/ChunkUploadController.java:29`
     - `basebackend-file-service/src/main/java/com/basebackend/file/chunk/ChunkUploadService.java:63`
     - `basebackend-file-service/src/main/java/com/basebackend/file/chunk/ChunkUploadService.java:156`
     - `basebackend-file-service/src/main/java/com/basebackend/file/storage/impl/LocalStorageServiceImpl.java:44`
   - 影响：本地存储模式下可以通过 `../../` 等路径把合并后的文件写到上传目录之外。
   - 建议：最终对象键必须由服务端生成；所有本地文件操作统一做 `normalize + startsWith(base)` 校验。

8. 文件服务默认限流器与控制器策略不匹配，上传/下载接口在默认配置下会直接走到未实现分支。
   - 文件：
     - `basebackend-file-service/src/main/java/com/basebackend/file/config/RateLimiterConfig.java:25`
     - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:58`
     - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:61`
     - `basebackend-file-service/src/main/java/com/basebackend/file/limit/SimpleRateLimiter.java:45`
     - `basebackend-file-service/src/main/java/com/basebackend/file/limit/SimpleRateLimiter.java:48`
   - 影响：默认注入的是 `SimpleRateLimiter`，但 `FileController` 使用的是 `slidingWindowLimit(...)`；一旦访问上传/下载限流路径，就会抛 `UnsupportedOperationException`，表现为接口直接 500。
   - 建议：要么实现 `SimpleRateLimiter` 的滑动窗口分支，要么默认切换到支持该策略的 `RedisRateLimiter`，并补控制器级回归测试。

9. 用户与角色管理的关键变更接口没有显式权限校验。
   - 文件：
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/UserController.java:76`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/UserController.java:93`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/UserController.java:119`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/UserController.java:136`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/UserController.java:174`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/UserController.java:193`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/RoleController.java:78`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/RoleController.java:95`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/RoleController.java:119`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/RoleController.java:136`
     - `basebackend-user-api/src/main/java/com/basebackend/user/controller/RoleController.java:155`
   - 影响：一旦请求能进入服务，普通用户可能直接调用创建/更新/删除/分配接口，破坏 RBAC。
   - 建议：为所有管理型读写接口补齐 `@RequiresPermission` 或等价授权规则，并加服务层兜底校验。

10. 系统监控接口把权限注解整段注释掉了，在线用户、强制下线、缓存操作、服务器信息都可能被未授权访问。
   - 文件：
     - `basebackend-system-api/src/main/java/com/basebackend/system/controller/MonitorController.java:35`
     - `basebackend-system-api/src/main/java/com/basebackend/system/controller/MonitorController.java:52`
     - `basebackend-system-api/src/main/java/com/basebackend/system/controller/MonitorController.java:69`
     - `basebackend-system-api/src/main/java/com/basebackend/system/controller/MonitorController.java:86`
     - `basebackend-system-api/src/main/java/com/basebackend/system/controller/MonitorController.java:103`
     - `basebackend-system-api/src/main/java/com/basebackend/system/controller/MonitorController.java:120`
     - `basebackend-system-api/src/main/java/com/basebackend/system/controller/MonitorController.java:137`
   - 影响：低权限用户可枚举在线会话、读取服务器信息、强制下线他人并干扰缓存。
   - 建议：恢复权限注解，并为高危监控操作补审计与二次保护。

11. 内部操作日志 API 缺少服务间鉴权或签名校验，任意可达调用方都能读取和伪造审计日志。
    - 文件：
      - `basebackend-system-api/src/main/java/com/basebackend/system/controller/internal/OperationLogInternalController.java:21`
      - `basebackend-system-api/src/main/java/com/basebackend/system/controller/internal/OperationLogInternalController.java:35`
      - `basebackend-system-api/src/main/java/com/basebackend/system/controller/internal/OperationLogInternalController.java:50`
    - 影响：审计可信度可被破坏，且可直接读取任意用户操作日志。
    - 建议：内部接口挂到独立安全链，至少校验服务间 token / mTLS / 签名来源。

12. 2FA 启用/禁用接口根本没有校验 `verifyCode`。
    - 文件：
      - `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/SecurityServiceImpl.java:175`
      - `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/SecurityServiceImpl.java:181`
      - `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/SecurityServiceImpl.java:210`
      - `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/SecurityServiceImpl.java:216`
    - 影响：已登录用户无需任何二次验证即可启用或关闭 2FA，安全配置形同虚设。
    - 建议：接入真实短信/邮箱/TOTP 验证，并为启停 2FA 补失败与审计测试。

13. 库存预占/扣减使用“先查再改”的非原子更新，在并发下会超卖。
    - 文件：
      - `basebackend-mall-product-api/src/main/java/com/basebackend/mall/product/service/impl/ProductServiceImpl.java:125`
      - `basebackend-mall-product-api/src/main/java/com/basebackend/mall/product/service/impl/ProductServiceImpl.java:135`
      - `basebackend-mall-product-api/src/main/java/com/basebackend/mall/product/service/impl/ProductServiceImpl.java:205`
      - `basebackend-mall-product-api/src/main/java/com/basebackend/mall/product/service/impl/ProductServiceImpl.java:217`
    - 影响：并发下会重复锁库存、扣成负数或超过可售库存，导致超卖。
    - 建议：改为带条件的原子 SQL 更新，或引入乐观锁版本字段，并补并发测试。

14. Chat WebSocket 会话只按 `userId` 路由，跨租户同 `userId` 会串消息。
    - 文件：
      - `basebackend-chat-api/src/main/java/com/basebackend/chat/websocket/ChatWebSocketHandler.java:66`
      - `basebackend-chat-api/src/main/java/com/basebackend/chat/service/impl/ChatMessageServiceImpl.java:419`
      - `basebackend-chat-api/src/main/java/com/basebackend/chat/service/impl/ChatMessageServiceImpl.java:438`
      - `basebackend-websocket/src/main/java/com/basebackend/websocket/session/SessionManager.java:36`
      - `basebackend-websocket/src/main/java/com/basebackend/websocket/session/SessionManager.java:65`
      - `basebackend-websocket/src/main/java/com/basebackend/websocket/session/SessionManager.java:153`
    - 影响：两个租户只要用户 ID 相同，就可能互相收到私聊/在线消息，属于租户隔离破坏。
    - 建议：会话键升级为 `tenantId:userId` 复合维度，所有路由和在线态查询都带租户。

15. 代码生成器的数据源接口直接返回已解密数据库密码，且控制层缺少权限保护。
    - 文件：
      - `basebackend-code-generator/src/main/java/com/basebackend/generator/controller/DataSourceController.java:31`
      - `basebackend-code-generator/src/main/java/com/basebackend/generator/controller/DataSourceController.java:40`
      - `basebackend-code-generator/src/main/java/com/basebackend/generator/controller/DataSourceController.java:68`
      - `basebackend-code-generator/src/main/java/com/basebackend/generator/entity/GenDataSource.java:55`
      - `basebackend-code-generator/src/main/java/com/basebackend/generator/handler/PasswordEncryptTypeHandler.java:20`
      - `basebackend-code-generator/src/main/java/com/basebackend/generator/handler/PasswordEncryptTypeHandler.java:52`
    - 影响：可直接读取明文数据库密码，还能借连接测试/表结构枚举探测内网数据库。
    - 建议：对外 DTO 去掉密码字段；控制器加管理员级鉴权；连接测试和表枚举补审计与限流。

16. 工单审批启动不是幂等操作，且远程启动流程与本地状态更新不具备事务一致性。
    - 文件：
      - `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketWorkflowServiceImpl.java:43`
      - `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketWorkflowServiceImpl.java:72`
      - `basebackend-ticket-api/src/main/java/com/basebackend/ticket/service/impl/TicketWorkflowServiceImpl.java:81`
    - 影响：重复点击会产生多个审批流程实例；远程成功、本地失败时会形成悬挂流程。
    - 建议：增加审批启动幂等键和状态条件更新；用带业务键的 Saga/Outbox 做跨服务一致性。

### Medium

17. 监控模块“清缓存”与“清所有缓存”是空实现，但接口返回成功。
    - 文件：
      - `basebackend-system-api/src/main/java/com/basebackend/system/service/impl/MonitorServiceImpl.java:243`
      - `basebackend-system-api/src/main/java/com/basebackend/system/service/impl/MonitorServiceImpl.java:253`
    - 影响：运维会被误导，以为缓存已清空，实际脏数据还在。
    - 建议：要么实现真实删除逻辑，要么在未实现前显式返回不支持。

18. 刷新令牌、新密码、强制下线 token 等敏感值通过 URL 传输，容易泄漏到日志、代理和浏览器历史。
    - 文件：
      - `basebackend-user-api/src/main/java/com/basebackend/user/controller/AuthController.java:81`
      - `basebackend-user-api/src/main/java/com/basebackend/user/controller/UserController.java:154`
      - `basebackend-system-api/src/main/java/com/basebackend/system/controller/MonitorController.java:52`
    - 影响：敏感值更容易被网关、APM、访问日志和浏览器历史采集。
    - 建议：改成请求体或安全请求头传递，并在日志中完全脱敏。

19. 网关签名校验没有覆盖 query/body，无法保护写请求内容完整性。
    - 文件：
      - `basebackend-gateway/src/main/java/com/basebackend/gateway/filter/SignatureVerifyFilter.java:39`
      - `basebackend-gateway/src/main/java/com/basebackend/gateway/filter/SignatureVerifyFilter.java:165`
      - `basebackend-gateway/src/main/java/com/basebackend/gateway/filter/SignatureVerifyFilter.java:209`
    - 影响：即使签名通过，攻击者仍可篡改请求体中的金额、商品、地址等字段。
    - 建议：把 canonical query string 和 body digest 纳入签名串。

20. `FileServiceClient` 被错误注册到了 `basebackend-system-api`，实际调用会打到错误服务。
    - 文件：
      - `basebackend-service-client/src/main/java/com/basebackend/service/client/ServiceClientAutoConfiguration.java:116`
      - `basebackend-service-client/src/main/java/com/basebackend/service/client/FileServiceClient.java:21`
    - 影响：所有文件服务客户端调用都可能 404 或打错下游。
    - 建议：把服务名改成真实的 `basebackend-file-service`，并补装配测试。

21. 订单号/支付单号按秒拼接业务主键，正常并发下就可能撞唯一索引。
    - 文件：
      - `basebackend-mall-trade-api/src/main/java/com/basebackend/mall/trade/service/impl/TradeServiceImpl.java:78`
      - `basebackend-mall-trade-api/src/main/resources/db/migration/V1.0__mall_trade_init.sql:42`
      - `basebackend-mall-pay-api/src/main/java/com/basebackend/mall/pay/service/impl/PayServiceImpl.java:67`
      - `basebackend-mall-pay-api/src/main/java/com/basebackend/mall/pay/service/impl/PayServiceImpl.java:109`
    - 影响：压测、双击、重试风暴场景下会偶发唯一键冲突，下单或建支付失败。
    - 建议：改为雪花 ID、sequence 或号段器，至少追加高熵随机尾缀并支持插入失败重试。

## 整改进展（2026-03-20）

以下问题已完成代码整改并通过受影响模块回归：

- Finding 2：`wechat-login` 已下线，`AuthServiceImpl.wechatLogin(...)` 不再按手机号直接登录或自动创建弱口令账号，当前会直接拒绝该入口，等待接入可信第三方身份断言后再开放。
- Finding 1：商城下单接口已不再接收客户端 `userId` / `payAmount`；`TradeServiceImpl.submitOrder(...)` 现在只接受 SKU/数量，从认证上下文解析当前用户，并通过 `ProductServiceClient` 按商品服务返回的售价、上下架状态和库存进行服务端核价后再落单与发事件。
- Finding 3：文件服务认证已切换为 `JwtTokenService` 真正验签，不再把所谓 token 当作明文分隔字符串解析；`X-User-ID` 头现在只记录安全告警，不再作为身份来源。
- Finding 4：通用 WebSocket 握手已改为仅接受已认证 `Principal`，不再信任客户端直传 `userId` 或未校验的 `token` 参数。
- Finding 5：文件列表与文件详情已改为按“当前用户可见范围”过滤；普通用户现在只能看到自己拥有、公开、或被显式授权的文件，管理员保留全量视角。
- Finding 6：文件服务旧版按路径下载/删除接口已下线，避免继续绕过基于 `fileId` 的权限链。
- Finding 7：分块上传最终存储路径改为服务端生成；本地存储读写、移动、复制、删除统一增加 `normalize + startsWith(base)` 路径归一化校验，阻断路径穿越。
- Finding 8：`SimpleRateLimiter` 已补齐滑动窗口实现，默认配置下文件上传/下载限流不再因未实现分支直接返回 500。
- Finding 9：`UserController` / `RoleController` 关键管理接口已补齐显式权限注解；同时权限切面已补上通配符匹配，`system:role:*` 等既有管理员权限能够正确命中细粒度接口权限。
- Finding 10：`MonitorController` 权限注解已恢复，并同步把内置管理员默认权限补齐到 `system:monitor:*` / 监控只读权限集合。
- Finding 12：2FA 启停接口已不再接受“空校验”；当前仅允许已初始化 `TOTP` 密钥的用户通过真实验证码启停，`sms/email` 因未接入可信校验链已改为 fail-close 拒绝。
- Finding 13：商品服务库存预占、扣减与释放已从“先查再改”切换为带库存条件的原子 SQL 更新，避免并发下重复锁库存或扣减超卖。
- Finding 11：内部操作日志接口已补基于共享密钥的 HMAC 内部调用签名校验；外部请求无法再仅靠普通用户 JWT 直打 `/api/internal/operation-log/**`。同时读取接口已收紧为“当前用户只能读自己的日志，管理员可例外”，写入接口也不再允许普通用户代他人伪造审计记录。
- Finding 14：`SessionManager` 已补租户复合键路由能力，聊天链路的会话注册、在线判断、好友状态广播、消息推送、群事件与好友事件推送均已改为按 `tenantId:userId` 维度路由，跨租户同 `userId` 不再串消息。
- Finding 15：代码生成器数据源接口已改为返回脱敏展示模型，不再出站明文数据库密码；同时控制器已收紧为管理员角色可访问。
- Finding 16：工单审批启动已补幂等保护；若本地已绑定流程实例则直接复用，若远端已存在同业务键流程则优先回填本地状态，若并发竞争导致重复启动则会清理多余流程实例并返回已成功绑定的实例。
- Finding 17：监控缓存清理接口已落真实 Redis 删除逻辑；`MonitorServiceImpl.clearCache(...)` / `clearAllCache()` 现在按受控缓存前缀调用 `redisService.deleteByPattern(...)`，不再是空操作。
- Finding 18：刷新令牌、重置密码、强制下线等敏感值已从 URL/query 中移出，统一改为 `@RequestBody` DTO（`RefreshTokenRequest`、`ResetPasswordRequest`、`ForceLogoutRequest`）传输；服务客户端与管理端前端请求契约也已同步到 body 方案。
- Finding 19：网关请求签名已覆盖 canonical query string 与 body 摘要；`SignatureVerifyFilter` 现按 `appId|timestamp|nonce|method|path|canonicalQuery|SHA-256(body)` 构造签名串，避免签名通过后仍可篡改 query/body。
- Finding 20：`FileServiceClient` 已改为绑定真实服务名 `basebackend-file-service`。
- Finding 21：订单号与支付单号已切换到高熵生成策略；`TradeServiceImpl.generateOrderNo(...)` 现在在时间戳和用户维度之外追加随机后缀，`PayServiceImpl.generatePayNo()` 改为基于雪花 ID 生成，避免秒级并发或重复重试时撞唯一索引。
- 构建链路补充处理：本轮额外修复 `basebackend-database/database-failover/target/generated-test-sources/test-annotations`、`basebackend-chat-api/target/generated-test-sources/test-annotations` 与 `basebackend-album-api/target/generated-test-sources/test-annotations` 等历史 `root` 属主构建产物，恢复顶层 `mvn clean install` 的 `clean` 阶段可重复执行性。

本轮尚未处理的高优先级问题：

- 无。报告中列出的高优先级 findings 已全部完成整改；当前剩余事项主要是“测试缺口”和“残余风险与假设”中列出的持续验证与后续治理工作。

本轮验证记录：

- `mvn -pl basebackend-file-service test`
- `mvn -pl basebackend-security test`
- `mvn -pl basebackend-user-api test`
- `mvn -pl basebackend-websocket test`
- `mvn -pl basebackend-system-api test`
- `mvn -pl basebackend-code-generator test`
- `mvn -pl basebackend-api-model,basebackend-service-client -am install -DskipTests`
- `mvn -pl basebackend-service-client -DskipTests compile`
- `mvn -pl basebackend-service-client -Dtest=AuthServiceClientContractTest test`
- `mvn -pl basebackend-gateway -Dtest=SignatureVerifyFilterTest test`
- `mvn -pl basebackend-user-api -Dtest=ControllerPermissionAnnotationTest test`
- `mvn -pl basebackend-system-api -Dtest=MonitorServiceTest,MonitorControllerPermissionTest test`
- `mvn -pl basebackend-service-client,basebackend-mall-trade-api -am test`
- `mvn -pl basebackend-file-service,basebackend-mall-product-api test`
- `mvn -pl basebackend-mall-pay-api -Dtest=PayServiceImplTest test`
- `mvn -pl basebackend-service-client,basebackend-system-api,basebackend-ticket-api,basebackend-websocket,basebackend-chat-api -am test`
- `mvn -pl basebackend-file-service -Dtest=FileManagementServiceAccessTest,FilePermissionServiceTest test`
- `mvn -pl basebackend-file-service -Dtest=FileControllerContextTest test`
- `mvn -pl basebackend-file-service -Dtest=FileControllerDeprecatedEndpointsTest test`
- `mvn -pl basebackend-file-service -Dtest=FileControllerWebMvcTest,FileControllerContextTest test`
- `mvn -pl basebackend-mall-product-api -Dtest=ProductServiceImplTest test`
- `mvn -pl basebackend-mall-product-api -Dtest=ProductServiceImplTest,ProductInventoryConsumerTest,PaymentSucceededConsumerTest test`
- `mvn -pl basebackend-mall-product-api -Dtest=ProductServiceImplTest,OrderCreatedConsumerTest,ProductInventoryConsumerTest,PaymentSucceededConsumerTest test`
- `mvn -pl basebackend-mall-trade-api -Dtest=TradeServiceImplTest test`
- `mvn -pl basebackend-mall-trade-api -Dtest=TradeServiceImplTest,PaymentSucceededConsumerTest test`
- `mvn -pl basebackend-mall-trade-api -Dtest=TradeServiceImplTest,PaymentSucceededConsumerTest,PaymentFailedConsumerTest test`
- `mvn -pl basebackend-file-service,basebackend-mall-product-api,basebackend-mall-trade-api,basebackend-mall-pay-api test`
- `mvn -pl basebackend-mall-pay-api -Dtest=PayServiceImplTest,OrderCreatedConsumerTest,OrderTimeoutClosedConsumerTest test`
- `mvn -pl basebackend-mall-trade-api,basebackend-mall-product-api,basebackend-mall-pay-api -Dtest=MallEventContractTest,PayServiceImplTest,OrderCreatedConsumerTest,OrderTimeoutClosedConsumerTest,TradeServiceImplTest,PaymentSucceededConsumerTest,PaymentFailedConsumerTest,ProductServiceImplTest,ProductInventoryConsumerTest test -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl basebackend-mall-trade-api,basebackend-mall-product-api,basebackend-mall-pay-api test`
- 顶层 `mvn clean install` 已于 `2026-03-20 13:38:31 +08:00` 全量通过
- 新一轮顶层 `mvn clean install` 已于 `2026-03-20 14:17:08 +08:00` 在本轮整改后再次全量通过
- 顶层 `mvn clean install` 曾于 `2026-03-20 14:30:50 +08:00` 因 `basebackend-album-api/target/generated-test-sources/test-annotations` 历史 `root` 属主导致 `clean` 失败；修复属主后已于 `2026-03-20 14:34:21 +08:00` 再次全量通过
- 顶层 `mvn clean install` 曾于 `2026-03-20 14:44:23 +08:00` 再次被同一 `album-api target` 属主残留卡住；修复属主后已于 `2026-03-20 14:49:10 +08:00` 全量通过
- 顶层 `mvn clean install` 曾于 `2026-03-20 15:04:36 +08:00` 因 `basebackend-chat-api/target/generated-test-sources/test-annotations` 历史 `root` 属主导致 `clean` 失败；修复属主后已于 `2026-03-20 15:08:12 +08:00` 再次全量通过
- 顶层 `mvn clean install` 曾于 `2026-03-20 15:59:33 +08:00` 因 `basebackend-database/database-failover/target/generated-test-sources/test-annotations` 历史 `root` 属主导致 `clean` 失败
- 顶层 `mvn clean install` 曾于 `2026-03-20 16:03:04 +08:00` 因 `basebackend-chat-api/target/generated-test-sources/test-annotations` 历史 `root` 属主导致 `clean` 失败
- subagent 扫描额外发现 `basebackend-album-api/target/generated-test-sources/test-annotations` 仍存在历史 `root` 属主，本轮已在最终重跑前一并修复
- 顶层 `mvn clean install` 已于 `2026-03-20 16:07:51 +08:00` 全量通过

## 测试缺口

- 已补 `AuthHandshakeInterceptor` 认证边界测试，当前缺口主要在握手与下游消息路由的集成联测。
- 已补 `wechat-login` 禁用回归测试，但尚未覆盖未来接入可信第三方认证后的正向链路。
- 文件服务已补路径穿越、滑动窗口限流，以及旧版下载/删除接口下线后的控制器级回归；当前仍缺结合真实共享权限数据与生产过滤链的更高层集成联测。
- 已补用户/角色控制器与监控控制器的权限注解回归测试，但尚未做结合真实权限数据的端到端授权联测。
- 已补 2FA `TOTP` 验证回归测试，但 `sms/email` 正向链路仍缺真实验证码基础设施。
- 已补代码生成器数据源响应脱敏与管理员角色限制测试，但尚未覆盖带真实数据库连接的控制器集成测试。
- 已补商城下单服务端核价、商品服务失败拒单、支付单重复事件幂等、关键消费者委派回归，以及跨模块 `producer -> JSON -> consumer` 契约回归测试；当前已覆盖 `trade.order-created -> payService.handleOrderCreated`、`trade.order-created -> productService.reserveStockForOrder`、`trade.order-cancelled -> productService.releaseReservedStockForCancelledOrder`、`trade.order-timeout-closed -> payService.handleOrderTimeoutClosed`、`trade.order-timeout-closed -> productService.releaseReservedStockForTimeoutOrder`、`pay.payment-succeeded -> tradeService.markOrderPaid`、`pay.payment-succeeded -> productService.deductStockForPaidOrder`、`pay.payment-failed -> tradeService.markOrderPaymentFailed`，并验证了 `TradeServiceImpl` / `PayServiceImpl` 实际发出的消息 JSON 可以被下游模块消费者兼容解析；当前剩余缺口主要是带真实 broker、数据库与服务实例的端到端联调。
- 已补文件列表/详情访问范围服务级回归、`FilePermissionService` 权限判定回归、`FileController` 的用户上下文透传测试，以及基于 standalone MockMvc 的 HTTP 层列表/详情/废弃接口回归；当前仍缺结合真实共享权限数据与生产过滤链的更高层集成联测。
- Mall Product 已补原子库存更新、取消/超时关单释放预占库存，以及支付成功消息消费者委派回归测试，但仍缺真实并发压测级别的竞争场景测试。
- 已补内部操作日志签名校验与用户绑定测试，但仍缺把签名校验挂进真实 Spring Security 链路后的控制器级集成测试。
- 已补 `SessionManager` 租户复合键测试，但聊天服务链路仍缺覆盖“同 `userId` 不同 `tenantId`”的端到端推送隔离测试。
- Ticket 审批链已补重复提交、远端已存在流程实例回填、并发竞争清理重复流程的单元测试，但跨服务网络抖动场景下的真正一致性压测仍缺失。

## 残余风险与假设

- `basebackend-common / jwt / database / cache / logging / security / observability / messaging` 这一组基础模块的专用 reviewer 超时未返回，因此这些模块并不能视为“无问题”。
- 本报告只列高置信度缺陷，没有把所有 TODO/骨架代码都算作正式 findings。
- 一些控制器可能在网关或外部安全链路上还有兜底，但从服务内代码看，授权约束并不自洽；因此本报告按“服务自身应能自证安全边界”标准给出结论。
