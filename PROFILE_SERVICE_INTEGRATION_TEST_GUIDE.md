# Profile Service 集成测试指南

## 📋 测试目标

验证 profile-service 独立部署后的功能完整性，包括：
- 个人资料管理
- 偏好设置管理
- 密码修改功能
- Feign 调用链路
- Gateway 路由

---

## 🚀 启动步骤

### 1. 启动依赖服务

```bash
# 启动 Nacos
cd nacos/bin
./startup.sh -m standalone

# 启动 MySQL（如果未运行）
docker-compose up -d mysql

# 启动 Redis（如果未运行）
docker-compose up -d redis
```

### 2. 初始化 profile-service 数据库

```bash
# 创建数据库并初始化表
mysql -u root -p < basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql
```

### 3. 启动 profile-service

```bash
cd basebackend-profile-service
mvn spring-boot:run
```

**验证启动成功：**
- 端口：8090
- 健康检查：http://localhost:8090/actuator/health
- 预期响应：`{"status":"UP"}`

---

## 🧪 API 测试

### 测试 1: 获取用户偏好设置（首次访问返回默认值）

```bash
curl -X GET http://localhost:8180/api/profile/preference \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json"
```

**预期响应：**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "theme": "light",
    "language": "zh-CN",
    "timezone": "Asia/Shanghai",
    "emailNotification": 1,
    "systemNotification": 1,
    "pageSize": 10
  }
}
```

### 测试 2: 更新用户偏好设置

```bash
curl -X PUT http://localhost:8180/api/profile/preference \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "theme": "dark",
    "language": "en-US",
    "emailNotification": 0,
    "pageSize": 20
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

### 测试 3: 获取个人资料

```bash
curl -X GET http://localhost:8180/api/profile/info \
  -H "Authorization: Bearer <token>"
```

**预期响应：**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "管理员",
    "email": "admin@example.com",
    "deptName": "技术部"
  }
}
```

### 测试 4: 更新个人资料

```bash
curl -X PUT http://localhost:8180/api/profile/info \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "新昵称",
    "email": "newemail@example.com",
    "phone": "13800138000"
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

### 测试 5: 修改密码

```bash
curl -X PUT http://localhost:8180/api/profile/password \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "OldPass123",
    "newPassword": "NewPass123",
    "confirmPassword": "NewPass123"
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

---

## 🔍 验证项目

### 1. Feign 调用验证

**验证点：**
- profile-service → admin-api（获取用户信息）
- profile-service → dept-service（获取部门信息）
- profile-service → user-service（更新用户资料、修改密码）

**日志检查：**
```bash
# 查看 profile-service 日志
tail -f basebackend-profile-service/logs/application.log

# 查找 Feign 调用日志
grep "profile-service" logs/application.log | grep "getByUsername"
grep "profile-service" logs/application.log | grep "updateUserProfile"
```

### 2. Gateway 路由验证

**测试路由：**
- ✅ `/api/profile/**` → `basebackend-profile-service`
- ✅ `/api/users/**` → `basebackend-user-service`
- ✅ `/api/auth/**` → `basebackend-auth-service`

**验证命令：**
```bash
# 查看 Gateway 路由列表
curl http://localhost:8180/actuator/gateway/routes
```

### 3. 数据库验证

**检查 user_preference 表：**
```sql
USE basebackend_profile;
SELECT * FROM user_preference;
```

**预期结果：**
- 表结构正确
- 有数据记录（如果已更新过偏好设置）

---

## 📊 性能指标

### 响应时间
- 获取偏好设置：< 100ms
- 更新偏好设置：< 200ms
- 获取个人资料：< 150ms
- 修改密码：< 300ms

### 吞吐量
- 并发用户：100+
- QPS：500+
- 成功率：> 99.9%

---

## ❌ 常见问题

### 问题 1: 端口冲突

**现象：** profile-service 启动失败

**解决方案：**
```bash
# 检查端口占用
netstat -tlnp | grep 8090

# 杀死占用端口的进程
kill -9 <PID>
```

### 问题 2: 数据库连接失败

**现象：** 无法连接 MySQL

**解决方案：**
```bash
# 检查 MySQL 服务状态
docker-compose ps mysql

# 重启 MySQL
docker-compose restart mysql
```

### 问题 3: Nacos 服务注册失败

**现象：** Gateway 无法路由到 profile-service

**解决方案：**
```bash
# 检查 Nacos 服务状态
curl http://localhost:8848/nacos/v1/console/health/readiness

# 重启 profile-service 等待服务注册
```

### 问题 4: Feign 调用失败

**现象：** 500 错误，调用 admin-api 失败

**解决方案：**
1. 检查 admin-api 是否启动
2. 检查 UserFeignClient 接口是否正确
3. 检查网络连通性

---

## 📝 测试报告

完成测试后，请记录以下信息：

- [ ] 数据库初始化成功
- [ ] profile-service 启动成功
- [ ] 所有 API 接口测试通过
- [ ] Feign 调用链路正常
- [ ] Gateway 路由配置正确
- [ ] 响应时间符合预期
- [ ] 无错误日志

**发现问题：**
- 问题描述：
- 复现步骤：
- 解决方案：

---

## 🎯 后续工作

测试完成后，需要处理的问题：

1. **在 admin-api 中添加缺失的端点**
   - PUT /api/admin/users/{id}/profile
   - PUT /api/admin/users/{id}/password

2. **优化 Feign 调用**
   - 配置超时时间
   - 添加熔断降级

3. **性能调优**
   - 添加缓存
   - 优化数据库查询

---

**测试完成日期：** _______________
**测试人员：** _______________
**测试结果：** _______________
