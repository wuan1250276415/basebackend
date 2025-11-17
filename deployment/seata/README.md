# Seata 分布式事务部署指南

## 📋 概述

本指南介绍如何部署和配置 Seata 2.0.0 分布式事务环境，包括 Seata Server、数据库、配置中心和监控组件。

---

## 🏗️ 架构图

```
┌────────────────────────────────────────────────────────────────┐
│                        Seata 分布式事务架构                      │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │  微服务 A     │  │   微服务 B     │  │   微服务 C     │             │
│  │(user-service)│  │(auth-service)│  │(app-service)│             │
│  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘             │
│         │                │                   │                   │
│         └────────────────┼───────────────────┘                   │
│                          │                                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │               Seata Server (TC)                            │ │
│  │              端口: 7091/8091                              │ │
│  └────────────────────────┬──────────────────────────────────┘ │
│                           │                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │            Nacos 配置中心                                  │ │
│  │           端口: 8888                                      │ │
│  └────────────────────────┬──────────────────────────────────┘ │
│                           │                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │ MySQL 数据库   │  │   Prometheus  │  │      Grafana          │ │
│  │  端口: 3307    │  │   端口: 9091   │  │    端口: 3001        │ │
│  └──────────────┘  └──────────────┘  └────────────────────────┘ │
│                           │                                     │
└───────────────────────────┼─────────────────────────────────────┘
                            │
                    ┌───────▼───────┐
                    │   数据库表     │
                    │ - global_table │
                    │ - branch_table │
                    │ - lock_table   │
                    │ - undo_log     │
                    └───────────────┘
```

---

## 🚀 快速开始

### 1. 部署 Seata 环境

```bash
# 进入部署目录
cd deployment/seata

# 给脚本添加执行权限
chmod +x deploy-seata.sh

# 启动 Seata 环境
./deploy-seata.sh start

# 查看服务状态
./deploy-seata.sh status

# 健康检查
./deploy-seata.sh health
```

### 2. 验证部署

访问以下地址确认服务正常运行：

- **Seata Server 控制台**: http://localhost:7091
  - 用户名: admin
  - 密码: admin

- **Nacos 控制台**: http://localhost:8888
  - 用户名: nacos
  - 密码: nacos

- **Prometheus**: http://localhost:9091

- **Grafana**: http://localhost:3001
  - 用户名: admin
  - 密码: admin123

---

## 📦 组件说明

### Seata Server
- **版本**: 2.0.0
- **端口**: 7091 (控制台) / 8091 (服务)
- **存储模式**: DB (MySQL)
- **协调模式**: Standalone

### MySQL 数据库
- **版本**: 8.0
- **端口**: 3307
- **密码**: 123456
- **用途**: 存储 Seata 事务数据

### Nacos 配置中心
- **版本**: v2.3.2
- **端口**: 8848 (8888 对外)
- **用途**: 配置管理和服务注册

### 监控组件
- **Prometheus**: 端口 9091
- **Grafana**: 端口 3001
- **用途**: 监控和可视化

---

## 🔧 微服务集成

### 步骤 1: 添加依赖

在每个微服务的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 步骤 2: 配置 Seata

将 `deployment/seata/service-configurations/` 目录下的配置文件内容复制到对应服务的 `application.yml` 中：

**basebackend-user-service/src/main/resources/application.yml**:
```yaml
seata:
  tx-service-group: basebackend_user_tx_group
  service:
    vgroup-mapping:
      basebackend_user_tx_group: default
    grouplist:
      default: localhost:8091
  data-source-proxy-mode: AT
  client:
    undo:
      log-table: undo_log
```

### 步骤 3: 创建 undo_log 表

在每个微服务的数据库中执行：

```sql
CREATE TABLE `undo_log` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `branch_id` BIGINT NOT NULL,
  `xid` VARCHAR(100) NOT NULL,
  `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGTEXT NOT NULL,
  `log_status` INT NOT NULL,
  `log_created_by` VARCHAR(32) NOT NULL,
  `log_modified_by` VARCHAR(32) NOT NULL,
  `ext` VARCHAR(100) DEFAULT NULL,
  `gmt_create` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log_xid` (`xid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 步骤 4: 使用分布式事务

```java
@Service
public class UserService {

    @GlobalTransactional(name = "create-user-and-assign-role", timeoutMills = 300000)
    public UserDTO createUserWithRole(UserCreateRequest request) {
        // 1. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        userMapper.insert(user);

        // 2. 调用其他服务
        roleServiceClient.assignRoleToUser(user.getId(), request.getRoleCode());

        return convertToDTO(user);
    }
}
```

---

## 📊 监控仪表板

### 1. Seata 事务监控

访问 Grafana (http://localhost:3001)，导入 Seata 监控仪表板：

**Seata Dashboard JSON**:
```json
{
  "dashboard": {
    "title": "Seata Distributed Transaction Monitor",
    "panels": [
      {
        "title": "Transaction Success Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(seata_global_table_status{status=\"1\"}[5m]) / rate(seata_global_table_status[5m]) * 100"
          }
        ]
      },
      {
        "title": "Transaction Count",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(seata_global_table_status[5m])"
          }
        ]
      }
    ]
  }
}
```

### 2. 告警规则

Prometheus 已配置告警规则，当以下情况触发时会发送告警：

- 事务失败率 > 10%
- 事务执行时间 P95 > 60秒
- 活跃事务数 > 100
- 锁等待时间 P95 > 10秒

---

## 🧪 测试验证

### 1. 事务成功测试

```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user",
    "password": "123456",
    "email": "test@example.com",
    "roleCode": "USER"
  }'
```

**预期结果**:
- 返回 200
- 数据库中用户已创建
- 角色已分配
- Seata 控制台显示成功事务

### 2. 事务回滚测试

在 Service 中抛出异常：

```java
@GlobalTransactional(name = "test-rollback")
public void testRollback() {
    userMapper.insert(user);
    // 模拟业务异常
    throw new RuntimeException("测试回滚");
}
```

**预期结果**:
- 用户未插入数据库
- Seata 控制台显示回滚事务

### 3. 压测测试

```bash
# 使用 JMeter 或 wrk 进行压力测试
wrk -t12 -c400 -d30s http://localhost:8081/api/users
```

**预期结果**:
- QPS > 1000
- 事务成功率 > 99%
- P95 响应时间 < 200ms

---

## ❌ 常见问题

### 问题 1: Seata Server 启动失败

**现象**: `Address already in use`

**解决方案**:
```bash
# 检查端口占用
netstat -tlnp | grep 7091

# 杀死占用进程
kill -9 <PID>

# 重新启动
./deploy-seata.sh restart
```

### 问题 2: 事务超时

**现象**: 事务执行超过超时时间

**解决方案**:
- 增加 `@GlobalTransactional` 的 `timeoutMills` 参数
- 优化事务内的业务逻辑
- 检查数据库连接池配置

### 问题 3: 事务被悬挂

**现象**: 事务状态长时间为 `Begin`

**解决方案**:
```sql
-- 手动清理悬挂事务
DELETE FROM global_table WHERE status = 1 AND gmt_create < DATE_SUB(NOW(), INTERVAL 10 MINUTE);
```

### 问题 4: 微服务集成失败

**现象**: 微服务无法注册到 Seata

**解决方案**:
1. 检查微服务配置文件
2. 验证网络连通性: `telnet localhost:8091`
3. 查看微服务日志
4. 检查 `undo_log` 表是否创建

---

## 📈 性能调优

### 1. Seata Server 调优

```yaml
# config/application.yml
seata:
  transport:
    server:
      # 增大线程池
      thread-factory:
        boss-thread-size: 2
        worker-thread-size: 32
    client:
      rm:
        # 调整缓存大小
        async-commit-buffer-limit: 20000
```

### 2. 数据库调优

```sql
-- 为 undo_log 表添加索引
ALTER TABLE undo_log ADD INDEX idx_branch_id (branch_id);
ALTER TABLE undo_log ADD INDEX idx_log_status (log_status);

-- 定期清理历史数据
CALL clean_expired_global_transactions(7);
```

### 3. 微服务调优

```yaml
# application.yml
spring:
  datasource:
    # 优化连接池
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
```

---

## 📚 参考资料

1. [Seata 官方文档](https://seata.io/zh-cn/docs/overview/what-is-seata)
2. [Seata AT 模式详解](https://seata.io/zh-cn/docs/user/transaction/at/)
3. [Seata 配置参考](https://seata.io/zh-cn/docs/user/configurations/)
4. [Seata 监控集成](https://seata.io/zh-cn/docs/user/ops/monitoring)

---

## 🎯 下一步

部署完成并验证后，继续执行：

1. **Phase 11.2**: 实现分布式缓存优化
2. **Phase 11.3**: 部署 XXL-Job 分布式任务调度
3. **Phase 11.4**: Nacos 配置中心增强
4. **Phase 11.5**: 安全加固

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**版本：** v1.0

**加油喵～ 分布式事务环境部署完成！** ฅ'ω'ฅ
