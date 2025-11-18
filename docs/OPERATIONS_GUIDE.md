# BaseBackend 运维手册

> **版本**: v1.0  
> **最后更新**: 2025-11-18

---

## 📋 日常运维

### 1. 服务监控

#### 检查服务状态
```bash
# 检查所有服务健康状态
./bin/maintenance/check-services.sh

# 检查Nacos注册服务
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-api
```

#### 监控指标
- CPU使用率 < 70%
- 内存使用率 < 80%
- 响应时间 P95 < 500ms
- 错误率 < 1%

### 2. 日志管理

#### 日志位置
```
logs/
├── basebackend-gateway.log
├── basebackend-user-api.log
├── basebackend-system-api.log
├── basebackend-auth-api.log
├── basebackend-notification-service.log
└── basebackend-observability-service.log
```

#### 日志查看
```bash
# 实时查看日志
tail -f logs/basebackend-user-api.log

# 查看错误日志
grep ERROR logs/basebackend-user-api.log

# 查看最近100行
tail -n 100 logs/basebackend-user-api.log
```

#### 日志清理
```bash
# 清理7天前的日志
find logs/ -name "*.log.*" -mtime +7 -delete

# 压缩旧日志
gzip logs/*.log.2025-11-*
```

### 3. 数据库维护

#### 备份
```bash
# 每日备份
mysqldump -u root -p basebackend > backup/basebackend_$(date +%Y%m%d).sql

# 压缩备份
gzip backup/basebackend_$(date +%Y%m%d).sql
```

#### 优化
```bash
# 分析表
mysql -u root -p -e "ANALYZE TABLE sys_user, sys_role, sys_permission;"

# 优化表
mysql -u root -p -e "OPTIMIZE TABLE sys_user, sys_role, sys_permission;"
```

### 4. 缓存管理

#### Redis监控
```bash
# 查看Redis信息
redis-cli info

# 查看内存使用
redis-cli info memory

# 查看连接数
redis-cli info clients
```

#### 缓存清理
```bash
# 清理特定前缀的缓存
redis-cli --scan --pattern "user:*" | xargs redis-cli del

# 清理过期key
redis-cli --scan --pattern "*" | xargs redis-cli ttl
```

---

## 🚨 告警处理

### 1. 服务宕机

**现象**: 服务无响应，健康检查失败

**处理步骤**:
1. 检查服务进程
2. 查看日志错误
3. 重启服务
4. 验证恢复

```bash
# 检查进程
ps aux | grep basebackend-user-api

# 重启服务
./bin/start/restart-service.sh user-api

# 验证
curl http://localhost:8081/actuator/health
```

### 2. 数据库连接池耗尽

**现象**: 大量"Cannot get connection"错误

**处理步骤**:
1. 检查数据库连接数
2. 检查慢查询
3. 增加连接池大小
4. 优化SQL查询

```bash
# 查看MySQL连接数
mysql -u root -p -e "SHOW PROCESSLIST;"

# 查看慢查询
mysql -u root -p -e "SELECT * FROM mysql.slow_log LIMIT 10;"
```

### 3. 内存溢出

**现象**: OutOfMemoryError，服务频繁重启

**处理步骤**:
1. 分析堆转储文件
2. 检查内存泄漏
3. 调整JVM参数
4. 优化代码

```bash
# 生成堆转储
jmap -dump:format=b,file=heapdump.hprof <pid>

# 分析堆转储
jhat heapdump.hprof
```

---

## 📊 性能优化

### 1. 应用层优化

- 启用缓存
- 优化数据库查询
- 使用连接池
- 异步处理

### 2. 数据库优化

- 添加索引
- 优化SQL
- 分库分表
- 读写分离

### 3. 缓存优化

- 合理设置过期时间
- 使用缓存预热
- 防止缓存穿透

---

## 🔧 故障排查

### 常见问题

1. **服务无法启动** - 检查端口、配置、依赖
2. **接口超时** - 检查网络、数据库、缓存
3. **内存泄漏** - 分析堆转储、检查代码
4. **CPU过高** - 分析线程栈、优化代码

---

**文档维护**: 运维团队  
**最后更新**: 2025-11-18
