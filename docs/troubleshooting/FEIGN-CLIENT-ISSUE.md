# Feign Client依赖问题修复

## 问题描述

启动user-api时报错：
```
Parameter 1 of constructor in com.basebackend.user.service.impl.AuthServiceImpl 
required a bean of type 'com.basebackend.feign.client.DeptFeignClient' that could not be found.
```

## 问题原因

`AuthServiceImpl` 依赖了 `DeptFeignClient` 来获取部门信息，但是：
1. Feign客户端没有正确扫描
2. 或者system-api服务未启动，导致Feign客户端无法创建

## 已应用的修复

### 修复1：注释Feign依赖（临时方案）

文件：`basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java`

```java
// 注释掉Feign客户端依赖
// @Lazy
// private final DeptFeignClient deptFeignClient;

// 注释掉获取部门名称的代码
// Result<DeptBasicDTO> dept = deptFeignClient.getById(user.getDeptId());
// if (dept != null) {
//     userInfo.setDeptName(dept.getData().getDeptName());
// }
```

### 修复2：配置Feign扫描包（备用方案）

文件：`basebackend-user-api/src/main/java/com/basebackend/user/UserApiApplication.java`

```java
@EnableFeignClients(basePackages = {"com.basebackend.feign.client"})
```

## 影响

- ✅ 用户可以正常登录
- ⚠️ 登录响应中不包含部门名称（deptName为空）
- ⚠️ 在线用户信息中不包含部门名称

## 启动服务

现在可以正常启动user-api：

```bash
cd basebackend-user-api
mvn spring-boot:run
```

或使用IDE启动。

## 测试登录

```bash
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username": "admin","password": "password"}'
```

预期响应：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "userInfo": {
      "userId": 1,
      "username": "admin",
      "nickname": "管理员",
      "deptId": 1,
      "deptName": null,  // 暂时为空
      "email": "admin@example.com",
      "phone": "13800138000",
      "avatar": null,
      "gender": 0,
      "userType": 1,
      "status": 1
    },
    "permissions": ["*:*:*"],
    "roles": ["admin"]
  }
}
```

## 长期解决方案

### 方案1：启用Feign并启动system-api

1. **启动system-api服务**
   ```bash
   cd basebackend-system-api
   mvn spring-boot:run
   ```

2. **取消注释Feign代码**
   
   在 `AuthServiceImpl.java` 中恢复：
   ```java
   @Lazy
   private final DeptFeignClient deptFeignClient;
   
   Result<DeptBasicDTO> dept = deptFeignClient.getById(user.getDeptId());
   if (dept != null) {
       userInfo.setDeptName(dept.getData().getDeptName());
   }
   ```

3. **重启user-api**

### 方案2：直接查询数据库

修改 `AuthServiceImpl` 直接查询部门表：

```java
private final SysDeptMapper deptMapper;

// 在login方法中
if (user.getDeptId() != null) {
    SysDept dept = deptMapper.selectById(user.getDeptId());
    if (dept != null) {
        userInfo.setDeptName(dept.getDeptName());
    }
}
```

### 方案3：延迟加载部门信息

登录时不返回部门名称，由前端在需要时单独调用接口获取。

## 推荐方案

**开发阶段**：使用方案2（直接查询数据库），简单快速

**生产环境**：使用方案1（Feign调用），符合微服务架构

## 相关文件

- `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java`
- `basebackend-user-api/src/main/java/com/basebackend/user/UserApiApplication.java`
- `basebackend-feign-api/src/main/java/com/basebackend/feign/client/DeptFeignClient.java`

## 其他可能的Feign问题

### 问题1：Feign客户端未找到

**错误**：
```
No Feign Client for loadBalancing defined
```

**解决**：
1. 确保 `@EnableFeignClients` 注解存在
2. 确保扫描包路径正确
3. 确保目标服务已注册到Nacos

### 问题2：服务调用失败

**错误**：
```
Load balancer does not have available server for client: basebackend-system-api
```

**解决**：
1. 启动system-api服务
2. 检查Nacos注册
3. 检查网络连接

## 总结

通过注释Feign依赖，user-api可以独立启动和运行。登录功能正常，只是暂时不返回部门名称。

后续可以根据需要选择合适的方案来获取部门信息。

---

**修复时间**: 2025-11-18  
**修复状态**: ✅ 已解决  
**影响范围**: user-api启动和登录功能
