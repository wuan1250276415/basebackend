# 服务启动和验证指南

## 📋 服务启动顺序

### 1. 启动基础设施服务

```bash
# 1.1 启动 MySQL
docker-compose up -d mysql

# 1.2 启动 Redis
docker-compose up -d redis

# 1.3 启动 Nacos
cd nacos/bin
./startup.sh -m standalone
```

**验证：**
```bash
# 验证 MySQL
mysql -u root -p123456 -e "SELECT 1;"

# 验证 Redis
redis-cli -h 1.117.67.222 -a redis_ycecQi ping

# 验证 Nacos
curl http://localhost:8848/nacos/v1/console/health/readiness
```

### 2. 启动微服务（按依赖顺序）

```bash
# 2.1 启动用户服务
cd basebackend-user-service
mvn spring-boot:run

# 2.2 启动认证服务
cd basebackend-auth-service
mvn spring-boot:run

# 2.3 启动菜单服务
cd basebackend-menu-service
mvn spring-boot:run

# 2.4 启动部门服务
cd basebackend-dept-service
mvn spring-boot:run

# 2.5 启动字典服务
cd basebackend-dict-service
mvn spring-boot:run

# 2.6 启动日志服务
cd basebackend-log-service
mvn spring-boot:run

# 2.7 启动监控服务
cd basebackend-monitor-service
mvn spring-boot:run

# 2.8 启动应用服务
cd basebackend-application-service
mvn spring-boot:run

# 2.9 启动通知服务
cd basebackend-notification-service
mvn spring-boot:run

# 2.10 启动档案服务
cd basebackend-profile-service
mvn spring-boot:run

# 2.11 启动网关服务
cd basebackend-gateway
mvn spring-boot:run
```

### 3. 服务端口对应表

| 服务名 | 端口 | 健康检查 URL |
|--------|------|-------------|
| user-service | 8081 | http://localhost:8081/actuator/health |
| auth-service | 8082 | http://localhost:8082/actuator/health |
| dept-service | 8083 | http://localhost:8083/actuator/health |
| dict-service | 8084 | http://localhost:8084/actuator/health |
| log-service | 8085 | http://localhost:8085/actuator/health |
| monitor-service | 8086 | http://localhost:8086/actuator/health |
| application-service | 8087 | http://localhost:8087/actuator/health |
| menu-service | 8088 | http://localhost:8088/actuator/health |
| notification-service | 8089 | http://localhost:8089/actuator/health |
| profile-service | 8090 | http://localhost:8090/actuator/health |
| gateway | 8180 | http://localhost:8180/actuator/health |

---

## 🔍 验证步骤

### 步骤 1: 逐个验证服务健康

```bash
#!/bin/bash
# 验证所有服务健康状态

services=(
    "8081:user-service"
    "8082:auth-service"
    "8083:dept-service"
    "8084:dict-service"
    "8085:log-service"
    "8086:monitor-service"
    "8087:application-service"
    "8088:menu-service"
    "8089:notification-service"
    "8090:profile-service"
    "8180:gateway"
)

for service in "${services[@]}"; do
    port=$(echo $service | cut -d: -f1)
    name=$(echo $service | cut -d: -f2)

    echo "检查 $name ($port)..."

    if curl -s -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
        echo "  ✓ $name 正常运行"
    else
        echo "  ✗ $name 未运行或异常"
    fi
done
```

### 步骤 2: 验证 Gateway 路由

```bash
# 检查 Gateway 路由配置
curl http://localhost:8180/actuator/gateway/routes

# 测试各个路由
curl -I http://localhost:8180/api/users/test
curl -I http://localhost:8180/api/auth/info
curl -I http://localhost:8180/api/menus/tree
curl -I http://localhost:8180/api/profile/preference
```

### 步骤 3: 测试 API 调用

#### 3.1 用户服务测试

```bash
# 获取用户信息
curl "http://localhost:8180/api/users/by-username?username=admin"
```

#### 3.2 菜单服务测试

```bash
# 获取菜单树
curl "http://localhost:8180/api/menus/tree"
```

#### 3.3 档案服务测试（需要认证）

```bash
# 获取偏好设置（需要先登录获取 token）
curl -H "Authorization: Bearer <token>" \
     "http://localhost:8180/api/profile/preference"
```

### 步骤 4: 验证数据库连接

```bash
# 连接到各个数据库
mysql -u root -p123456 -e "SHOW DATABASES;"

# 验证各数据库中的表
mysql -u root -p123456 -e "USE basebackend_user; SHOW TABLES;"
mysql -u root -p123456 -e "USE basebackend_menu; SHOW TABLES;"
mysql -u root -p123456 -e "USE basebackend_dict; SHOW TABLES;"
mysql -u root -p123456 -e "USE basebackend_dept; SHOW TABLES;"
mysql -u root -p123456 -e "USE basebackend_log; SHOW TABLES;"
mysql -u root -p123456 -e "USE basebackend_profile; SHOW TABLES;"
```

---

## 📊 集成测试清单

### 基础设施
- [ ] MySQL 启动正常
- [ ] Redis 启动正常
- [ ] Nacos 启动正常

### 微服务
- [ ] user-service 健康检查通过
- [ ] auth-service 健康检查通过
- [ ] menu-service 健康检查通过
- [ ] dept-service 健康检查通过
- [ ] dict-service 健康检查通过
- [ ] log-service 健康检查通过
- [ ] monitor-service 健康检查通过
- [ ] application-service 健康检查通过
- [ ] notification-service 健康检查通过
- [ ] profile-service 健康检查通过
- [ ] gateway 健康检查通过

### Gateway 路由
- [ ] /api/users/** 路由正常
- [ ] /api/auth/** 路由正常
- [ ] /api/security/** 路由正常
- [ ] /api/menus/** 路由正常
- [ ] /api/depts/** 路由正常
- [ ] /api/dicts/** 路由正常
- [ ] /api/logs/** 路由正常
- [ ] /api/profile/** 路由正常
- [ ] /api/applications/** 路由正常

### API 测试
- [ ] 用户服务 API 可调用
- [ ] 菜单服务 API 可调用
- [ ] 档案服务 API 可调用（需要认证）

### 数据库
- [ ] MySQL 连接正常
- [ ] Redis 连接正常
- [ ] 各数据库表结构正确

---

## 🚨 常见问题

### 问题 1: 服务启动失败

**现象：** `Address already in use`

**解决方案：**
```bash
# 查找占用端口的进程
netstat -tlnp | grep <port>

# 杀死进程
kill -9 <PID>

# 重新启动服务
```

### 问题 2: 数据库连接失败

**现象：** `Connection refused`

**解决方案：**
```bash
# 检查 MySQL 是否启动
systemctl status mysql

# 重启 MySQL
systemctl restart mysql

# 检查防火墙
firewall-cmd --list-ports
```

### 问题 3: Nacos 服务注册失败

**现象：** Gateway 无法路由到服务

**解决方案：**
1. 检查服务日志中的错误信息
2. 确认 Nacos 配置正确
3. 等待服务注册完成（可能需要几秒）

### 问题 4: Feign 调用失败

**现象：** 服务间调用返回 500 错误

**解决方案：**
1. 检查被调用服务是否正常启动
2. 检查 FeignClient 接口是否正确
3. 检查超时配置

---

## 📈 性能测试

### 启动测试服务

```bash
# 使用 Apache Bench 进行简单压测

# 测试用户服务
ab -n 1000 -c 10 http://localhost:8180/api/users/by-username?username=admin

# 测试菜单服务
ab -n 1000 -c 10 http://localhost:8180/api/menus/tree
```

### 性能指标

| 指标 | 目标值 | 当前值 | 状态 |
|------|--------|--------|------|
| 并发数 | 100 | - | - |
| QPS | 1000+ | - | - |
| 平均响应时间 | < 100ms | - | - |
| P95 响应时间 | < 200ms | - | - |
| 成功率 | > 99% | - | - |

---

## 🎯 下一步行动

完成验证后：

1. **记录测试结果**
2. **分析性能数据**
3. **优化性能瓶颈**
4. **编写测试报告**

---

**更新日期：** 2025-11-14
**负责人：** 浮浮酱（猫娘工程师）
