# basebackend-user-api 模块审查报告

**审查日期**: 2026-03-15  
**审查范围**: `basebackend-user-api` 模块源码、配置、测试、现有文档  
**审查方式**: 静态代码审查 + 结构检查 + 交叉核对安全基线/Mapper/测试代码  

## 1. 结论摘要

`basebackend-user-api` 当前可以编译出 `target/classes` 与 `target/test-classes`，但从安全性、权限边界和实现一致性看，模块仍存在多项高风险问题，不建议以当前状态作为可上线基线。

本次审查发现：

| 级别 | 数量 | 说明 |
|------|------|------|
| Critical | 1 | 公开登录接口存在身份冒用风险 |
| High | 4 | 退出登录、2FA、权限控制、菜单关联存在实质性缺陷 |
| Medium | 4 | 缓存一致性、异常语义、配置安全、测试覆盖存在明显缺口 |

总体判断：

- 安全性: 不通过
- 权限控制: 不通过
- 设计一致性: 不通过
- 测试充分性: 不通过
- 文档完整性: 不通过

## 2. 审查范围与方法

本次审查覆盖以下内容：

- 模块结构、POM、配置文件、README、现有审查文档
- Controller / Service / Mapper / Entity / DTO 主路径实现
- 与 `basebackend-security`、`basebackend-jwt` 的接口契约
- 单元测试覆盖范围与测试代码有效性

说明：

- `verify-module` / `verify-quality` / `verify-security` 的 Node 脚本未能直接执行，原因是当前环境中的 `node` 通过 Volta 包装并尝试写入沙箱外目录。
- 本次尝试重新运行 `mvn -pl basebackend-user-api test`，但本机命令执行环境异常，`mvn`/`cmd.exe` 无法启动，因此未完成本轮测试复跑。
- 模块目录下不存在 `target/surefire-reports`，无法引用最近一次 Surefire 结果。

## 3. 关键问题清单

### 3.1 Critical

#### C1. 微信单点登录接口可被任意手机号冒用，并自动创建启用账号

**证据**

- 公开放行 `/api/user/auth/**`:
  [SecurityConfig.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-security/src/main/java/com/basebackend/security/config/SecurityConfig.java#L83)
- 公开暴露 `/wechat-login`:
  [AuthController.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/controller/AuthController.java#L147)
- 仅凭手机号直接登录 / 自动建号:
  [AuthServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java#L340)
- 新账号默认密码固定为 `123456`:
  [AuthServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java#L352)

**问题说明**

当前“微信单点登录”没有微信 OAuth/OpenID、短信验证码、签名验签、一次性票据等任何身份凭证校验，仅凭一个手机号即可：

- 查询已有用户并直接签发 Token
- 不存在时自动创建启用状态账号
- 为新用户设置默认密码 `123456`

这意味着知道手机号即可获得会话，风险是直接的身份冒用与账号接管。

**影响**

- 任意手机号用户可被冒充登录
- 可批量伪造账号并触发脏数据写入
- 默认密码进一步扩大后续撞库与接管风险

**建议**

- 立即下线或封禁该接口
- 改为“微信 code -> 平台换取 openId/unionId -> 绑定用户”的标准流程
- 新用户自动创建时不得设置固定默认密码
- 若保留手机号登录，至少要求短信验证码或服务端签发的一次性票据

### 3.2 High

#### H1. 退出登录未实际吊销 Token，已签发会话在有效期内持续可用

**证据**

- `logout()` 仅记录日志，无任何状态变更:
  [AuthServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java#L161)
- 登录时会写入 token / online user 缓存:
  [AuthServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java#L118)
- JWT 工具已提供吊销能力但模块未使用:
  [JwtUtil.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-jwt/src/main/java/com/basebackend/jwt/JwtUtil.java#L486)

**问题说明**

模块当前具备 JWT 黑名单吊销能力，但 `logout()` 没有：

- 解析当前请求中的 Token
- 调用 `jwtUtil.revokeToken(...)`
- 删除 `login_tokens:` / `online_users:` / 权限角色缓存

这会导致“用户主动退出”只是前端概念，服务端并未失效会话。

**影响**

- 已泄露 Token 在过期前依旧可用
- 在线用户统计与真实状态不一致
- 后续风控与强制下线能力失真

**建议**

- 在 `logout()` 中提取 Bearer Token 并调用 `jwtUtil.revokeToken(token)`
- 同步清理 Redis 中的 token、在线用户、权限和角色缓存
- 增加退出登录和强制下线测试

#### H2. 2FA 启用/禁用接口未校验验证码，安全能力名存实亡

**证据**

- 启用 2FA 时直接标记启用，验证码校验为 TODO:
  [SecurityServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/SecurityServiceImpl.java#L175)
- 禁用 2FA 时同样未校验验证码:
  [SecurityServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/SecurityServiceImpl.java#L210)

**问题说明**

接口虽然要求 `verifyCode` 参数，但服务层完全未校验，任意已登录用户只要能发请求就能：

- 直接启用一个未真正验证的 2FA 配置
- 直接关闭已有 2FA

这与“二次认证”设计目标相矛盾。

**影响**

- 2FA 无法作为可信安全因子
- 账号保护能力被绕过
- 审计记录会误导运维和安全人员

**建议**

- 按 `type` 分流校验 TOTP / 短信 / 邮件验证码
- 只有验证成功后才允许启用/禁用
- 为成功和失败场景补齐单元测试与接口测试

#### H3. 用户与角色管理接口缺少权限注解，默认仅需登录即可访问

**证据**

- Spring Security 默认规则是“除白名单外，其他请求只要已认证即可访问”:
  [SecurityConfig.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-security/src/main/java/com/basebackend/security/config/SecurityConfig.java#L100)
- 权限切面只拦截显式标注了注解的方法:
  [PermissionAspect.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-security/src/main/java/com/basebackend/security/aspect/PermissionAspect.java#L42)
- `UserController` 中仅“重置密码”显式加了权限:
  [UserController.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/controller/UserController.java#L154)
- `RoleController` 的创建、删除、分配菜单等接口均未加权限注解:
  [RoleController.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/controller/RoleController.java#L78)
  [RoleController.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/controller/RoleController.java#L119)
  [RoleController.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/controller/RoleController.java#L136)

**问题说明**

在当前安全基线下，未标注 `@RequiresPermission` 的接口只会执行“是否登录”的检查。结果是：

- 任意已登录用户可调用用户管理接口
- 任意已登录用户可创建/修改/删除角色及其菜单、权限、资源、数据权限配置

这不是代码风格问题，而是实际越权面。

**影响**

- 普通用户可直接调用后台管理接口
- RBAC 模型失去约束意义
- 一旦前端或网关被绕过，后端没有二次防线

**建议**

- 为所有后台管理接口补齐权限注解
- 参考 `basebackend-system-api` 的控制器权限标注方式统一整改
- 增加“普通用户调用管理接口返回 403”的接口级测试

#### H4. 角色菜单接口写入的是 `sys_role_resource`，而菜单查询走的是 `sys_role_menu`

**证据**

- 角色菜单分配/删除/读取均操作 `SysRoleResource`:
  [RoleServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/RoleServiceImpl.java#L170)
  [RoleServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/RoleServiceImpl.java#L185)
  [RoleServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/RoleServiceImpl.java#L234)
- 角色 Mapper 明确定义菜单来源表为 `sys_role_menu`:
  [SysRoleMapper.xml](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/resources/mapper/SysRoleMapper.xml#L48)
- 用户菜单查询同样依赖 `sys_role_menu`:
  [SysUserMapper.xml](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/resources/mapper/SysUserMapper.xml#L37)

**问题说明**

当前“角色菜单”接口名称、DTO 字段、Mapper SQL 和 Service 实现不一致：

- SQL 认为菜单关系存于 `sys_role_menu`
- Service 实际却把菜单当成资源写入 `sys_role_resource`

这会导致接口调用看似成功，但菜单授权链路与用户菜单查询链路无法闭合。

**影响**

- 菜单分配结果可能不会反映到真实菜单权限
- 前端菜单、后端菜单校验、数据模型三者不一致
- 测试用例围绕错误实现编写，进一步掩盖缺陷

**建议**

- 明确“菜单”和“资源”的边界
- 菜单接口改用 `SysRoleMenuMapper` / `sys_role_menu`
- 资源接口继续使用 `sys_role_resource`
- 增加集成测试，验证“分配菜单 -> 查询角色菜单 -> 查询用户菜单”闭环

### 3.3 Medium

#### M1. 权限缓存键前缀不一致，导致登录写缓存与权限读取脱节

**证据**

- 登录写入的键前缀:
  [AuthServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java#L48)
  [AuthServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java#L129)
- 权限服务读取的键前缀:
  [UserPermissionServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/UserPermissionServiceImpl.java#L26)
  [UserPermissionServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/UserPermissionServiceImpl.java#L37)

**问题说明**

登录流程写入：

- `user_permissions:{userId}`
- `user_roles:{userId}`

权限服务读取：

- `user:permissions:{userId}`
- `user:roles:{userId}`

写入和读取永远对不上，缓存命中率为零，且会在 Redis 中形成两套不兼容命名。

**影响**

- 权限缓存形同虚设
- 角色/权限更新后更容易出现读旧值和重复缓存
- 线上排障成本上升

**建议**

- 统一 Redis key 规范
- 将 key 常量下沉到共享组件，避免各服务类自定义字符串
- 为登录后权限读取场景增加单元测试

#### M2. 登录失败属于业务场景，但当前会被控制器误判为系统异常

**证据**

- `AuthController` 只对 `BusinessException` 做业务分支处理:
  [AuthController.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/controller/AuthController.java#L45)
- `AuthServiceImpl` 对“用户不存在/密码错误/用户禁用”抛的是 `RuntimeException`:
  [AuthServiceImpl.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java#L75)

**问题说明**

`AuthController.login()` 设计上希望把业务异常和系统异常区分开，但服务层并未配合，导致常见登录失败会落入通用 `Exception` 分支，最终返回“系统繁忙，请稍后重试”。

**影响**

- 客户端无法正确区分认证失败与系统故障
- 用户体验差，排障困难
- 现有 `AuthController` 的异常分层设计被架空

**建议**

- 登录相关校验统一改抛 `BusinessException`
- 统一用户、角色、认证服务的异常体系
- 增加 Controller 层测试验证返回码和文案

#### M3. 配置文件包含硬编码默认凭据与内部地址

**证据**

- Redis 默认密码与内网地址:
  [application.yml](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/resources/application.yml#L8)
  [application.yml](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/resources/application.yml#L10)
- Nacos 默认账号密码与固定 IP:
  [application.yml](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/resources/application.yml#L54)
  [application.yml](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/resources/application.yml#L57)
  [application.yml](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/resources/application.yml#L62)
- RocketMQ 默认 NameServer 地址:
  [application.yml](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/main/resources/application.yml#L103)

**问题说明**

虽然注释写明“开发兜底配置”，但把真实风格的默认密码、固定 IP、默认账号写入模块资源文件，仍然会：

- 增加误用到非开发环境的概率
- 形成敏感信息扩散
- 让本地配置与部署配置边界不清

**建议**

- 删除资源文件中的默认密码和固定内网地址
- 改为模板化占位符或仅保留本地示例文件
- 启动时若关键配置缺失，应显式失败而不是静默回落

#### M4. 测试覆盖有限，且已有测试与当前实现出现脱节

**证据**

- 当前仅有 4 个测试类:
  [AuthServiceImplTest.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/test/java/com/basebackend/user/service/impl/AuthServiceImplTest.java)
  [ProfileServiceImplTest.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/test/java/com/basebackend/user/service/impl/ProfileServiceImplTest.java)
  [RoleServiceImplTest.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/test/java/com/basebackend/user/service/impl/RoleServiceImplTest.java)
  [UserServiceImplTest.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/test/java/com/basebackend/user/service/impl/UserServiceImplTest.java)
- `ProfileServiceImplTest` 仍围绕 `SecurityContextHolder` 编写，而实现已经依赖 `UserContextHolder`:
  [ProfileServiceImplTest.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/test/java/com/basebackend/user/service/impl/ProfileServiceImplTest.java#L77)
- `UserServiceImplTest` 仍在 mock 未被使用的 `deptFeignClient`，并对角色名/部门名断言采取“允许为空”的宽松策略:
  [UserServiceImplTest.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/test/java/com/basebackend/user/service/impl/UserServiceImplTest.java#L732)
  [UserServiceImplTest.java](/Users/wuan1/IdeaProjects/basebackend/basebackend-user-api/src/test/java/com/basebackend/user/service/impl/UserServiceImplTest.java#L793)

**问题说明**

当前测试存在两个问题：

- 缺少关键路径测试：`SecurityServiceImpl`、`PreferenceServiceImpl`、`UserPermissionServiceImpl`、Controller 权限与异常映射都没有覆盖
- 已有测试为了适配旧实现而“放宽断言”，无法有效发现当前缺陷

**建议**

- 增加安全、权限、缓存一致性、菜单闭环、2FA、退出登录的测试
- 补充 Controller 层切面和鉴权集成测试
- 删除与实现脱节的宽松断言

## 4. 结构与文档检查

### 4.1 模块完整性

检查结果：

- `README.md`: 存在
- `DESIGN.md`: 缺失
- `src/main`: 存在
- `src/test`: 存在
- `docs/CODE_REVIEW_REPORT.md`: 存在，但本次已更新为最新版本

结论：

- 模块不满足“README + DESIGN + code”完整性基线
- 设计决策无法追溯，尤其不利于解释“菜单”和“资源”的边界

### 4.2 代码规模观察

主源码文件中体量最大的几个文件如下：

- `UserServiceImpl.java`: 517 行
- `RoleServiceImpl.java`: 469 行
- `AuthServiceImpl.java`: 432 行
- `RoleController.java`: 413 行
- `UserController.java`: 403 行

说明：

- 控制器与服务类已出现明显膨胀
- 控制器重复 try/catch 和日志模式，建议下沉到统一异常处理

## 5. 整改优先级建议

### 第一优先级

- 关闭或重构 `wechat-login`，补齐真实身份校验
- 实现退出登录的 Token 吊销与缓存清理
- 为用户/角色管理接口补齐权限注解
- 修复 2FA 验证逻辑

### 第二优先级

- 修复角色菜单与角色资源模型错配
- 统一权限/角色 Redis key 规范
- 统一异常体系，清除认证路径中的 `RuntimeException` 误用

### 第三优先级

- 移除资源文件中的默认敏感配置
- 补齐 DESIGN 文档
- 增加安全、权限、缓存和集成测试

## 6. 最终结论

`basebackend-user-api` 模块当前最突出的问题不是“代码风格”，而是“安全边界不稳”和“设计契约不一致”：

- 认证入口存在可直接利用的高危路径
- 后台管理接口默认暴露给所有已登录用户
- 菜单授权链路实现与数据模型不一致
- 2FA、退出登录、权限缓存等安全基础能力没有真正闭环

在上述问题修复前，不建议将本模块视为已完成的后台用户与权限服务。
