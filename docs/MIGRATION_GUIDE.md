# BaseBackend 迁移指南

> **版本**: v1.0  
> **最后更新**: 2025-11-18  
> **适用场景**: 从Admin API迁移到微服务架构

---

## 📋 迁移概述

本指南帮助您从单体Admin API迁移到新的微服务架构。

### 架构变化

**迁移前**:
```
Admin API (单体应用)
├── 用户管理
├── 系统管理
├── 认证授权
├── 通知管理
└── 监控管理
```

**迁移后**:
```
微服务架构
├── Gateway (API网关)
├── User API (用户服务)
├── System API (系统服务)
├── Auth API (认证服务)
├── Notification Service (通知服务)
└── Observability Service (可观测性服务)
```

---

## 🔄 迁移步骤

### 阶段1: 准备工作

#### 1.1 环境准备
- [ ] 安装JDK 17
- [ ] 安装Maven 3.8+
- [ ] 安装MySQL 8.0+
- [ ] 安装Redis 6.0+
- [ ] 安装Nacos 2.2+
- [ ] 安装Docker (可选)

#### 1.2 代码准备
```bash
# 克隆新代码
git clone https://github.com/your-org/basebackend.git
cd basebackend

# 切换到迁移分支
git checkout feature/admin-api-splitting
```

#### 1.3 数据库准备
```bash
# 备份现有数据
mysqldump -u root -p basebackend > backup_before_migration.sql

# 执行新表结构
mysql -u root -p basebackend < sql/migration/add_new_tables.sql
```

---

### 阶段2: 灰度迁移

#### 2.1 部署新服务（不影响现有服务）

```bash
# 启动新服务（使用不同端口）
java -jar basebackend-user-api.jar --server.port=8081 &
java -jar basebackend-system-api.jar --server.port=8082 &
java -jar basebackend-auth-api.jar --server.port=8083 &
```

#### 2.2 配置网关灰度路由

```yaml
gateway:
  gray:
    enabled: true
    rules:
      - serviceName: user-api
        grayVersion: v2.0.0  # 新服务
        stableVersion: v1.0.0  # 旧服务
        strategy: weight
        weight: 10  # 10%流量到新服务
```

#### 2.3 逐步增加流量

```
第1天: 10%流量 -> 新服务
第3天: 30%流量 -> 新服务
第5天: 50%流量 -> 新服务
第7天: 100%流量 -> 新服务
```

---

### 阶段3: 前端调用路径更新

#### 3.1 API路径映射

| 旧路径 | 新路径 | 说明 |
|--------|--------|------|
| /api/admin/users | /api/user/users | 用户管理 |
| /api/admin/roles | /api/user/roles | 角色管理 |
| /api/admin/depts | /api/system/depts | 部门管理 |
| /api/admin/menus | /api/system/menus | 菜单管理 |
| /api/admin/dicts | /api/system/dicts | 字典管理 |
| /api/admin/notifications | /api/notifications | 通知管理 |
| /api/admin/metrics | /api/metrics | 指标查询 |
| /api/admin/traces | /api/traces | 追踪查询 |
| /api/admin/logs | /api/logs | 日志查询 |
| /api/admin/alerts | /api/alerts | 告警管理 |

#### 3.2 前端代码更新

**Vue示例**:
```javascript
// 旧代码
const API_BASE = '/api/admin';

// 新代码
const API_ROUTES = {
  user: '/api/user',
  system: '/api/system',
  auth: '/api/auth',
  notification: '/api/notifications',
  observability: '/api/metrics'
};

// 使用
axios.get(`${API_ROUTES.user}/users`);
```

**React示例**:
```javascript
// 配置文件
export const API_CONFIG = {
  gateway: 'http://localhost:8080',
  routes: {
    user: '/api/user',
    system: '/api/system',
    auth: '/api/auth',
    notification: '/api/notifications'
  }
};

// 使用
fetch(`${API_CONFIG.gateway}${API_CONFIG.routes.user}/users`);
```

---

### 阶段4: 数据迁移

#### 4.1 用户数据迁移

```sql
-- 检查数据一致性
SELECT COUNT(*) FROM sys_user;
SELECT COUNT(*) FROM sys_role;
SELECT COUNT(*) FROM sys_permission;

-- 如果有新表，迁移数据
INSERT INTO new_table SELECT * FROM old_table;
```

#### 4.2 通知数据迁移

```sql
-- 迁移通知数据
INSERT INTO user_notification 
SELECT * FROM admin_notification;
```

#### 4.3 告警规则迁移

```sql
-- 导入预置告警规则
SOURCE basebackend-observability-service/src/main/resources/sql/schema.sql;
```

---

### 阶段5: 验证和切换

#### 5.1 功能验证

```bash
# 运行集成测试
./bin/test/integration-test.sh

# 手动测试关键功能
# 1. 用户登录
# 2. 用户管理
# 3. 通知发送
# 4. 监控查询
```

#### 5.2 性能验证

```bash
# 压力测试
ab -n 10000 -c 100 http://localhost:8080/api/user/users

# 监控指标
curl http://localhost:8080/actuator/metrics
```

#### 5.3 完全切换

```yaml
# 关闭灰度，100%流量到新服务
gateway:
  gray:
    enabled: false
```

---

## 🔙 回滚方案

### 快速回滚

如果发现问题，可以快速回滚：

```bash
# 1. 停止新服务
./bin/stop/stop-new-services.sh

# 2. 恢复网关配置
git checkout main -- basebackend-gateway/src/main/resources/application-routes.yml

# 3. 重启网关
./bin/start/restart-gateway.sh

# 4. 验证
curl http://localhost:8080/actuator/health
```

### 数据回滚

```bash
# 恢复数据库
mysql -u root -p basebackend < backup_before_migration.sql
```

---

## 📊 迁移检查清单

### 迁移前
- [ ] 备份数据库
- [ ] 备份配置文件
- [ ] 准备回滚方案
- [ ] 通知相关人员

### 迁移中
- [ ] 部署新服务
- [ ] 配置灰度路由
- [ ] 监控错误日志
- [ ] 验证核心功能

### 迁移后
- [ ] 验证所有功能
- [ ] 性能测试
- [ ] 更新文档
- [ ] 下线旧服务

---

## 🎯 最佳实践

1. **分阶段迁移** - 不要一次性切换所有流量
2. **保留回滚能力** - 随时可以回滚到旧版本
3. **充分测试** - 在测试环境充分验证
4. **监控告警** - 密切关注监控指标
5. **文档更新** - 及时更新API文档和运维文档

---

**文档维护**: 架构团队  
**最后更新**: 2025-11-18
