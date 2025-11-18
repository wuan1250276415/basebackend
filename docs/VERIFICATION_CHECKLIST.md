# 微服务优化验证清单

本清单用于验证优化后的微服务系统是否正常运行。

## 验证环境

- [ ] JDK 17+ 已安装
- [ ] Maven 3.8+ 已安装
- [ ] Docker 已安装并运行
- [ ] Docker Compose 已安装
- [ ] 8081、8082、8083、8848端口未被占用

## 验证步骤

### 1. 代码编译验证 ✅

```bash
# 编译项目
mvn clean compile -pl basebackend-system-api,basebackend-auth-api,basebackend-user-api -am -DskipTests

# 预期结果
# [INFO] BUILD SUCCESS
# [INFO] Total time: ~14s
```

**验证点**:
- [ ] 编译成功，无错误
- [ ] 所有模块编译通过
- [ ] 仅有少量警告（可忽略）

### 2. 基础设施启动验证

```bash
# 启动MySQL和Redis
cd docker/compose/base
docker-compose up -d

# 启动Nacos
cd ../middleware
docker-compose up -d nacos

# 等待30秒让服务完全启动
sleep 30
```

**验证点**:
- [ ] MySQL容器运行中
- [ ] Redis容器运行中
- [ ] Nacos容器运行中
- [ ] 可以访问 http://localhost:8848/nacos

### 3. 数据库初始化验证

```bash
# 连接MySQL
mysql -h localhost -u root -p

# 执行以下SQL
CREATE DATABASE IF NOT EXISTS basebackend_admin;
USE basebackend_admin;
SOURCE basebackend-admin-api/src/main/resources/db/schema.sql;
```

**验证点**:
- [ ] 数据库创建成功
- [ ] 表结构创建成功
- [ ] 可以查询到表: `SHOW TABLES;`

### 4. 微服务启动验证

```bash
# 方式1: 使用脚本
./bin/start/start-microservices.sh

# 方式2: 手动启动
cd basebackend-user-api && mvn spring-boot:run &
cd basebackend-system-api && mvn spring-boot:run &
cd basebackend-auth-api && mvn spring-boot:run &

# 等待服务启动（约60秒）
sleep 60
```

**验证点**:
- [ ] User API启动成功（端口8081）
- [ ] System API启动成功（端口8082）
- [ ] Auth API启动成功（端口8083）
- [ ] 日志无严重错误

### 5. 服务健康检查验证

```bash
# 使用验证脚本
./bin/test/verify-services.sh

# 或手动检查
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

**验证点**:
- [ ] User API健康检查返回 `{"status":"UP"}`
- [ ] System API健康检查返回 `{"status":"UP"}`
- [ ] Auth API健康检查返回 `{"status":"UP"}`

### 6. Nacos注册验证

```bash
# 检查服务注册
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-api"
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-system-api"
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-auth-api"
```

**验证点**:
- [ ] User API已注册到Nacos
- [ ] System API已注册到Nacos
- [ ] Auth API已注册到Nacos
- [ ] 实例数量 > 0

### 7. 认证功能验证

```bash
# 测试登录
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 预期响应
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
#   "expiresIn": 3600,
#   "userInfo": {
#     "userId": 1,
#     "username": "admin"
#   }
# }
```

**验证点**:
- [ ] 返回accessToken
- [ ] 返回expiresIn
- [ ] 返回userInfo
- [ ] HTTP状态码200

### 8. JWT Token验证

```bash
# 使用上一步获取的Token
TOKEN="your_access_token_here"

# 验证Token
curl -X POST http://localhost:8083/api/auth/verify \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$TOKEN\"}"
```

**验证点**:
- [ ] Token验证成功
- [ ] 返回验证结果

### 9. 用户服务验证

```bash
# 获取用户列表
curl -X GET "http://localhost:8081/api/user/users?current=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**验证点**:
- [ ] 返回用户列表
- [ ] 包含分页信息
- [ ] HTTP状态码200

### 10. 系统服务验证

```bash
# 获取部门树
curl -X GET http://localhost:8082/api/system/depts/tree \
  -H "Authorization: Bearer $TOKEN"

# 获取菜单树
curl -X GET http://localhost:8082/api/system/menus/tree \
  -H "Authorization: Bearer $TOKEN"

# 获取字典数据
curl -X GET "http://localhost:8082/api/system/dicts/data?dictType=user_status" \
  -H "Authorization: Bearer $TOKEN"
```

**验证点**:
- [ ] 部门树返回正确
- [ ] 菜单树返回正确
- [ ] 字典数据返回正确
- [ ] 树形结构正确

### 11. Redis缓存验证

```bash
# 连接Redis
redis-cli

# 查看Token缓存
KEYS auth:token:*

# 查看用户缓存
KEYS auth:user:*

# 查看某个Token
GET auth:token:your_token_here
```

**验证点**:
- [ ] Token已缓存到Redis
- [ ] 用户信息已缓存
- [ ] TTL设置正确（3600秒）

### 12. API文档验证

访问以下地址验证API文档：
- http://localhost:8081/doc.html
- http://localhost:8082/doc.html
- http://localhost:8083/doc.html

**验证点**:
- [ ] User API文档可访问
- [ ] System API文档可访问
- [ ] Auth API文档可访问
- [ ] 接口列表完整

### 13. 日志验证

```bash
# 查看日志
tail -f logs/info.log
tail -f logs/error.log

# 查看特定服务日志
tail -f logs/basebackend-user-api.log
tail -f logs/basebackend-system-api.log
tail -f logs/basebackend-auth-api.log
```

**验证点**:
- [ ] 日志正常输出
- [ ] 无严重错误
- [ ] 请求日志记录正确

### 14. 性能验证

```bash
# 使用Apache Bench测试
ab -n 100 -c 10 http://localhost:8081/actuator/health

# 查看响应时间
# Time per request: < 100ms (平均)
```

**验证点**:
- [ ] 响应时间 < 100ms
- [ ] 无请求失败
- [ ] 并发处理正常

### 15. 功能完整性验证

#### System-API功能
- [ ] 部门管理
  - [ ] 获取部门树
  - [ ] 获取部门列表
  - [ ] 创建部门
  - [ ] 更新部门
  - [ ] 删除部门
  - [ ] 查询子部门

- [ ] 菜单管理
  - [ ] 获取菜单树
  - [ ] 获取菜单列表
  - [ ] 创建菜单
  - [ ] 更新菜单
  - [ ] 删除菜单
  - [ ] 获取用户菜单

- [ ] 字典管理
  - [ ] 获取字典列表
  - [ ] 创建字典
  - [ ] 更新字典
  - [ ] 删除字典
  - [ ] 获取字典数据

#### Auth-API功能
- [ ] 用户登录
- [ ] 用户登出
- [ ] Token验证
- [ ] Token刷新
- [ ] 获取当前用户信息
- [ ] 修改密码

#### User-API功能
- [ ] 用户CRUD
- [ ] 角色管理
- [ ] 用户角色分配
- [ ] 用户状态管理
- [ ] 密码重置

## 验证结果

### 通过标准
- ✅ 所有编译验证通过
- ✅ 所有服务启动成功
- ✅ 所有健康检查通过
- ✅ 所有API测试通过
- ✅ 核心功能验证通过

### 验证日期
- 验证人: ___________
- 验证日期: ___________
- 验证环境: ___________

### 问题记录
如有问题，请记录：

| 序号 | 问题描述 | 严重程度 | 状态 | 备注 |
|-----|---------|---------|------|------|
| 1   |         |         |      |      |
| 2   |         |         |      |      |
| 3   |         |         |      |      |

## 快速验证脚本

如果您想快速验证所有功能，可以运行：

```bash
# Linux/Mac
./bin/test/verify-services.sh

# Windows
bin\test\verify-services.bat
```

## 故障排查

如果验证失败，请参考：
- [快速启动指南](./QUICK_START_AFTER_OPTIMIZATION.md)
- [优化完成报告](./OPTIMIZATION_COMPLETION_REPORT.md)
- [故障排查文档](./troubleshooting/)

## 相关文档

- [优化待办清单](./OPTIMIZATION_TODO.md)
- [优化完成报告](./OPTIMIZATION_COMPLETION_REPORT.md)
- [优化总结](./OPTIMIZATION_SUMMARY.md)
- [快速启动指南](./QUICK_START_AFTER_OPTIMIZATION.md)

---

**文档版本**: v1.0  
**创建日期**: 2025-11-17  
**适用版本**: BaseBackend 1.0.0-SNAPSHOT
